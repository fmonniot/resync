package eu.monniot.resync.ui.downloader

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.webkit.WebView
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.FileProvider
import eu.monniot.resync.BuildConfig
import eu.monniot.resync.FileName
import eu.monniot.resync.downloader.*
import eu.monniot.resync.makeEpub
import eu.monniot.resync.ui.EmphasizedEasing
import eu.monniot.resync.ui.KeepScreenOn
import eu.monniot.resync.ui.ReSyncTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import java.io.File
import java.lang.NumberFormatException

private const val TAG = "DownloadFic"

@Composable
fun DownloadScreen(
    driverType: DriverType,
    storyId: StoryId,
    chapterId: ChapterId,
    onDone: () -> Unit,
) {
    val context = LocalContext.current

    val driver = remember(driverType, context) {
        when (driverType) {
            DriverType.ArchiveOfOurOwn -> ArchiveOfOurOwnDriver(context.filesDir)
            DriverType.FanFictionNet -> FanFictionNetDriver(context.filesDir)
        }
    }

    val (state, setState) = remember {
        mutableStateOf<DownloadState>(
            DownloadState.FetchingFirstChapter(
                storyId,
                chapterId
            )
        )
    }

    // Because AO3 download can be quite long (1s between chapters + rate limit wait time),
    // we keep the screen on to avoid having the app goes into background and interrupt
    // the download.
    // We can't really do background processing either as we are using a WebView.
    KeepScreenOn()

    var downloadJob by remember { mutableStateOf<Job?>(null) }

    // Incremented by the Error screen's "Try again" button (see onRetry below) to force the
    // LaunchedEffect to re-run without changing storyId/chapterId.
    var attempt by remember { mutableIntStateOf(0) }

    LaunchedEffect(key1 = storyId, key2 = chapterId, key3 = attempt) {
        downloadJob = coroutineContext[Job]
        try {
            // wait for the driver to be attached to a running WebView
            driver.ready()

            downloadLogic(context.filesDir, storyId, chapterId, driverType, driver, setState)

            // Reaching DownloadState.Success is the end of the flow; onDone() is now only
            // called from the "Done" button on the Success screen (or from onCancel below).
        } catch (e: CancellationException) {
            // Cancellation (see onCancel below) is not an error: don't render the Error
            // screen, and don't call onDone() again - onCancel already does.
            throw e
        } catch (e: Throwable) {
            Log.e(TAG, "Error caught when downloading story", e)
            setState(DownloadState.Error(e))
        }
    }

    val onCancel: () -> Unit = {
        downloadJob?.cancel()
        clearChapterCache(driver.storyCacheDir(storyId))
        onDone()
    }

    // Resetting the state is not optional: downloadLogic does not set a state before its first
    // driver.readChapter call, so without this the Error screen would stay on screen for the
    // whole retry with no feedback that anything happened.
    val onRetry: () -> Unit = {
        setState(DownloadState.FetchingFirstChapter(storyId, chapterId))
        attempt++
    }

    Box {

        // Not entirely certain if the size count for something tbh.
        // At one point cloudflare was not resolving correctly, then
        // I increased the size to what is below, it worked, then
        // when I went back to the 1/1dp size, it continued working.
        // Maybe a cookie? Let's see if small size continue to work.
        // If no, I guess I'll need something to automatically
        // display a big view for one load and then hide it again.
        // No idea ¯\_(ツ)_/¯
        val webViewModifier = if (!BuildConfig.DEBUG) {
            Modifier
                .fillMaxWidth()
                .height(50.dp)
        } else {
            Modifier.size(1.dp, 1.dp)
        }

        // We need a web view to grab some fiction content.
        // This is mainly to go around the CloudFlare protection that FF.Net have.
        // By using a web view, we are seen as "normal" web traffic.
        AndroidView(factory = ::WebView, modifier = webViewModifier) { webView ->
            driver.installGrabber(webView)
        }

        // Shared-axis-X screen transition between DownloadState states. contentKey = { it::class }
        // is load-bearing: DownloadingRemainingChapters emits a new instance per chapter, and
        // without keying on the class alone every chapter update would re-trigger the full
        // screen transition instead of just updating in place. The `s` lambda parameter is used
        // below, not `state` directly, for the same reason as the Search<->Download transition.
        AnimatedContent(
            targetState = state,
            contentKey = { it::class },
            transitionSpec = {
                val offsetSpec = tween<IntOffset>(300, easing = EmphasizedEasing)
                val fadeSpec = tween<Float>(300, easing = EmphasizedEasing)
                (slideInHorizontally(offsetSpec) { it / 4 } + fadeIn(fadeSpec)) togetherWith
                        (slideOutHorizontally(offsetSpec) { -it / 4 } + fadeOut(fadeSpec))
            },
            label = "downloadState",
        ) { s ->
            when (s) {
                is DownloadState.FetchingFirstChapter -> FetchingFirstChapterView(
                    storyId = s.storyId,
                    chapterId = s.chapterId,
                    onCancel = onCancel,
                )
                is DownloadState.ConfirmChapters -> ConfirmChapters(
                    s.storyName,
                    s.authorName,
                    s.initialChapterNumber,
                    s.totalChapters,
                    s.driverType,
                    s.onUserConfirmation,
                    onCancel = onCancel,
                )
                is DownloadState.DownloadingRemainingChapters -> DownloadingRemainingChapters(
                    storyName = s.storyName,
                    currentlyDownloading = s.currentlyDownloading,
                    totalToDownloads = s.totalToDownloads,
                    notice = s.notice,
                    onCancel = onCancel,
                )
                is DownloadState.Error -> DisplayDownloadError(
                    error = s.throwable,
                    driverType = driverType,
                    storyId = storyId,
                    chapterId = chapterId,
                    onRetry = onRetry,
                    onDone = onCancel,
                )
                is DownloadState.Success -> DownloadSuccess(
                    summary = s.summary,
                    onShare = { shareEpub(context, s.epubFile, s.fileName) },
                    onDone = onDone,
                )
            }
        }
    }
}

/*
    Flow is:
    1. Fetch chapter storyId/chapterId
    2. if total chapters == 1, skip this step
       Otherwise let user choose what to download
    3. If more than one chapter selected, download remaining chapters
    4. Build the epub and end on DownloadState.Success; sharing is a separate, explicit user
       action from there (see shareEpub) rather than something this function does itself.
     */
suspend fun downloadLogic(
    filesDir: File,
    storyId: StoryId,
    chapterId: ChapterId,
    driverType: DriverType,
    driver: Driver,
    setState: (DownloadState) -> Unit,
) {

    // initial state is FetchingFirstChapter, no need to re-set it here
    val initialChapter = driver.readChapter(storyId, chapterId)

    val (chaptersInEpub, wholeStory) = selectChaptersToDownload(
        initialChapter,
        driverType,
        driver,
        setState,
    )

    // Build the epub file and its name
    val epub = makeEpub(chaptersInEpub)
    val fileName = FileName.make(chaptersInEpub, wholeStory)

    // The epub has been built successfully, so the cached chapter HTML backing it is no
    // longer needed regardless of what happens with the resulting file (share sheet,
    // etc.) - clear it now rather than letting it accumulate on disk forever. This is
    // deliberately NOT done earlier/inside selectChaptersToDownload: an in-progress (and
    // possibly AO3-rate-limited) download relies on that cache to resume without
    // refetching already-downloaded chapters.
    clearChapterCache(driver.storyCacheDir(storyId))

    // Direct reMarkable Cloud upload was removed; sharing (now behind the "Share to reMarkable"
    // button on the Success screen, see shareEpub below) is currently the only upload path. A
    // future reimplementation of the cloud integration plugs back in here.
    // Read how to do so at https://developer.android.com/training/secure-file-sharing
    val epubFile = filesDir.resolve("epub/$fileName")
    epubFile.parentFile?.mkdir()
    epubFile.writeBytes(epub)

    val storyName = chaptersInEpub.first().storyName
    val chapterNums = chaptersInEpub.map { it.num }
    val summary = when {
        wholeStory ->
            "$storyName saved as an EPUB, ready to send to reMarkable."
        chapterNums.size == 1 ->
            "$storyName — chapter ${chapterNums.first()} saved as an EPUB, ready to send to reMarkable."
        else ->
            "$storyName — chapters ${chapterNums.min()}–${chapterNums.max()} saved as an EPUB, ready to send to reMarkable."
    }

    setState(DownloadState.Success(epubFile, fileName, storyName, summary))
}

/**
 * Fires the Android Share sheet for [file] (an EPUB previously written by [downloadLogic]),
 * offering it to whatever app the user picks - typically the reMarkable app. Held verbatim from
 * the code this replaced: the `ClipData`/`EXTRA_STREAM` duplication and the
 * `FLAG_GRANT_READ_URI_PERMISSION` flag are load-bearing for the reMarkable app, not incidental.
 */
private fun shareEpub(context: Context, file: File, fileName: String) {
    // Read how to do so at https://developer.android.com/training/secure-file-sharing
    val fileUri = FileProvider.getUriForFile(
        context,
        "eu.monniot.resync.fileprovider",
        file
    )

    val shareIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        type = "application/epub+zip"

        clipData = ClipData(fileName.replace(".epub", ""),
            arrayOf("application/epub+zip"),
            ClipData.Item(fileUri))

        // BC purposes, which isn't require for rm app (maybe)
        putExtra(Intent.EXTRA_STREAM, fileUri)

        putExtra(Intent.EXTRA_SUBJECT, "Sharing Story...")
        putExtra(Intent.EXTRA_TEXT, "Sharing Story...")
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
    }

    context.startActivity(Intent.createChooser(shareIntent, "Share Story"), null)
}

/**
 * The chapter-selection/rate-limit-retry state machine, factored out of [downloadLogic] so it
 * can be driven in a unit test with a fake [ChapterReader] instead of a WebView-backed [Driver]
 * and a real [Context]. Given the already-fetched first chapter, this either returns it as-is
 * (one-shot stories), or asks the user (via [setState]/[DownloadState.ConfirmChapters]) which
 * chapters to download and fetches the rest, retrying through [readWithRateLimit] on AO3 rate
 * limits.
 *
 * @return the chapters to bundle into the epub (sorted by chapter number), and whether that
 *         selection represents the whole story.
 */
suspend fun selectChaptersToDownload(
    initialChapter: Chapter,
    driverType: DriverType,
    chapterReader: ChapterReader,
    setState: (DownloadState) -> Unit,
): Pair<List<Chapter>, Boolean> {
    val knownChapters = initialChapter.chapterIndex

    val chaptersInEpub = mutableListOf(initialChapter)
    var wholeStory = true

    if (initialChapter.totalChapters == 1) {
        // one-shot, we can build and upload the epub with the known chapter
    } else {
        // Multiple chapters available, let the user choose which one to include
        val userChoice = CompletableDeferred<ChapterSelection>()

        setState(DownloadState.ConfirmChapters(
            storyName = initialChapter.storyName,
            authorName = initialChapter.author,
            initialChapterNumber = initialChapter.num,
            totalChapters = initialChapter.totalChapters,
            driverType,
            onUserConfirmation = { selection ->
                userChoice.complete(selection)
            }
        ))

        // wait for user choice to be made
        val chapterSelection = userChoice.await()

        Log.d(TAG, "whole story choice: chapterSelection=$chapterSelection")
        // Set the wholeStory flag based on user choice.
        wholeStory = when (chapterSelection) {
            ChapterSelection.All -> true
            // in this branch the story has more than one chapter
            is ChapterSelection.One -> false
            // Two choices could be made here:
            // 1. if the range is start==1 && end==totalChapters, then it's the whole
            //    story
            // 2. within the same condition, the story might not be completed yet
            //    and as such we cannot say if it's a whole story or not.
            // We went with option two here, although it conflicts with the one-shot
            // decision previously made. This is because one-shots are common enough
            // that I feel the exception make sense.
            is ChapterSelection.Range -> false
        }

        when (chapterSelection) {
            is ChapterSelection.One ->
                if (chapterSelection.chapter == initialChapter.num) {
                    // Nothing to do, the chapter is already downloaded
                } else {
                    // The id have to exists, because the selection is constrained
                    // within the known/existing chapters.
                    setState(
                        DownloadState.DownloadingRemainingChapters(
                            initialChapter.storyName, 1, 1, null
                        )
                    )
                    val id = knownChapters[chapterSelection.chapter]!!

                    val chapter = readWithRateLimit(
                        { chapterReader.readChapter(initialChapter.storyId, id) },
                        {
                            setState(
                                DownloadState.DownloadingRemainingChapters(
                                    initialChapter.storyName, 1, 1, it
                                )
                            )
                        }
                    )

                    // The user only want the selected chapter, remove the initial one
                    chaptersInEpub.clear()
                    chaptersInEpub.add(chapter)
                }

            // TODO Factor together All and Range as they are very similar
            ChapterSelection.All -> {
                // The user wants everything, let's iterate over the known chapters

                val setDlState = { index: Int, notice: String? ->
                    setState(
                        DownloadState.DownloadingRemainingChapters(
                            initialChapter.storyName,
                            index,
                            initialChapter.totalChapters - 1,
                            notice,
                        )
                    )
                }

                // We filter out the chapter we have already downloaded
                knownChapters.values
                    .filter { it != initialChapter.chapterId }
                    .forEachIndexed { index, id ->
                        // TODO Check if the index is correctly aligned (0 or 1)
                        setDlState(index, null)

                        // Chapters stay cached on disk (Driver.readChapter) for the duration of
                        // the download: AO3's rate-limited retries can make a story take a long
                        // time, and re-fetching everything from scratch after a rate limit hit
                        // would be expensive. downloadLogic clears the story's cache directory
                        // once the epub has been built successfully.

                        // AO3 has a vague definition of what they consider normal usage.
                        // Let's wait 5 seconds between each chapter. It's more or less like
                        // a human going over each chapter and checking the first sentence.
                        if (driverType == DriverType.ArchiveOfOurOwn) {
                            delay(1000)
                        }

                        val chapter = readWithRateLimit(
                            { chapterReader.readChapter(initialChapter.storyId, id) },
                            { setDlState(index, it) }
                        )

                        chaptersInEpub.add(chapter)
                    }
            }

            is ChapterSelection.Range -> {
                val toDownload = knownChapters
                    .filter { (num, id) -> chapterSelection.contains(num) && initialChapter.chapterId != id }

                val setDlState = { index: Int, notice: String? ->
                    setState(
                        DownloadState.DownloadingRemainingChapters(
                            initialChapter.storyName,
                            index,
                            toDownload.size,
                            notice
                        )
                    )
                }

                toDownload.values.forEachIndexed { index, id ->
                    // TODO Check if the index is correctly aligned (0 or 1)
                    setDlState(index, null)

                    // AO3 has a vague definition of what they consider normal usage.
                    // Let's wait 5 seconds between each chapter. It's more or less like
                    // a human going over each chapter and checking the first sentence.
                    if (driverType == DriverType.ArchiveOfOurOwn) {
                        delay(1000)
                    }

                    val chapter = readWithRateLimit(
                        { chapterReader.readChapter(initialChapter.storyId, id) },
                        { setDlState(index, it) }
                    )

                    chaptersInEpub.add(chapter)
                }
            }
        }

    } // end of multiple chapters flow

    // Make sure that we put the chapters in order
    chaptersInEpub.sortBy { it.num }

    return chaptersInEpub to wholeStory
}

internal suspend fun readWithRateLimit(
    read: suspend () -> Chapter,
    updateState: (String) -> Unit,
    maxRetry: Int = 10, // tried for 10 minutes to know the limit
): Chapter {
    for (i in 1..maxRetry) {
        try {
            return read()
        } catch (e: Driver.Companion.RateLimited) {
            // Wait 90 seconds before trying again
            for (time in 60 downTo 1) {
                updateState(ao3RLNotice(i, time))
                delay(1000)
            }
        }
    }

    // If we reached this point, then we couldn't read the chapter even after retries
    // Let's throw the exception and bubble up (TODO Or do better ?)
    throw Driver.Companion.RateLimited
}

internal fun ao3RLNotice(limitHit: Int, remainingSeconds: Int): String {
    val n = when (limitHit) {
        1 -> "once"
        2 -> "twice"
        else -> "$limitHit times"
    }

    return "AO3 rate limit hit ($n)\nWaiting $remainingSeconds second${if (remainingSeconds > 1) "s" else ""} before resuming download."
}

sealed interface DownloadState {
    data class FetchingFirstChapter(
        val storyId: StoryId,
        val chapterId: ChapterId,
    ) : DownloadState

    data class ConfirmChapters(
        val storyName: String,
        val authorName: String,
        val initialChapterNumber: Int,
        val totalChapters: Int,
        val driverType: DriverType,
        val onUserConfirmation: (ChapterSelection) -> Unit,
    ) : DownloadState

    // TODO Is currently 0 or 1-indexed value ?
    data class DownloadingRemainingChapters(
        val storyName: String,
        val currentlyDownloading: Int,
        val totalToDownloads: Int,
        val notice: String?,
    ) : DownloadState

    data class Error(val throwable: Throwable) : DownloadState

    data class Success(
        val epubFile: File,
        val fileName: String,
        val storyName: String,
        val summary: String,
    ) : DownloadState
}


/**
 * The 64dp header row shared by the full-screen task states ([FetchingFirstChapterView],
 * [DownloadingRemainingChapters]): a close [IconButton] bound to [onCancel], plus [title] when
 * known. [FetchingFirstChapterView] passes `title = null` - the story name isn't known until the
 * first chapter has been fetched and parsed, which is what that state is doing.
 */
@Composable
private fun TaskStateHeader(title: String?, onCancel: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onCancel,
            modifier = Modifier.padding(start = 4.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Close",
            )
        }

        if (title != null) {
            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
fun FetchingFirstChapterView(
    storyId: StoryId,
    chapterId: ChapterId,
    onCancel: () -> Unit = {},
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TaskStateHeader(title = null, onCancel = onCancel)

        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 24.dp),
        ) {
            // No progress argument: fetching the first chapter is a single unknown-duration
            // request, so the indeterminate animation is M3's (and the design's) default.
            CircularProgressIndicator(modifier = Modifier.size(64.dp))

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Looking up story",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "id: ${storyId.id} · chapter: ${chapterId.id}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(
    showBackground = true,
    name = "Looking up story"
)
@Composable
fun FetchFirstPreview() {
    ReSyncTheme {
        FetchingFirstChapterView(
            storyId = StoryId(1),
            chapterId = ChapterId(null),
        )
    }
}

/**
 * A left-aligned "OR" divider: two [HorizontalDivider]s flanking a small centered label. Used
 * between the primary "download entire story" action and the expanded specific-chapters card -
 * it does not appear in the collapsed state (see [ConfirmChapters]).
 */
@Composable
private fun OrDivider(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f))
        Text(
            text = "OR",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        HorizontalDivider(modifier = Modifier.weight(1f))
    }
}

@Composable
fun ConfirmChapters(
    storyName: String,
    authorName: String,
    initialChapterNumber: Int,
    totalChapters: Int,
    driverType: DriverType,
    onUserConfirmation: (ChapterSelection) -> Unit,
    onCancel: () -> Unit = {},
    // Preview-only hook to render the expanded disclosure state; real callers never pass this.
    initiallyExpanded: Boolean = false,
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    var chapterStart by remember { mutableStateOf(initialChapterNumber) }
    var chapterEnd by remember { mutableStateOf(totalChapters) }

    // One chevron vector, rotated 180 degrees when expanded, shared by the collapsed
    // TextButton's trailing icon and the expanded Card's collapse IconButton below - rather than
    // swapping between ExpandMore/ExpandLess assets.
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "chooseChaptersChevron",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        // Header: back arrow + story title on a surfaceContainer block. Static (nothing on this
        // screen scrolls-to-collapse), so a plain Column carries the color/typography rather
        // than TopAppBar and its scrollBehavior machinery.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer),
        ) {
            Row(
                modifier = Modifier.height(64.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onCancel,
                    modifier = Modifier.padding(start = 4.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                    )
                }
            }

            Text(
                text = storyName,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
            )
        }

        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "by $authorName",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // This screen is unreachable for one-shot stories (selectChaptersToDownload skips
            // ConfirmChapters entirely when totalChapters == 1), so the chip is unconditional.
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = { /* display-only, not interactive */ },
                    label = { Text("$totalChapters chapters") },
                    enabled = true,
                )
                AssistChip(
                    onClick = { /* display-only, not interactive */ },
                    label = { Text(driverType.websiteName()) },
                    enabled = true,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { onUserConfirmation(ChapterSelection.All) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Download entire story")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Collapsed: a bare text-button row. No card, no "OR" divider - those only appear
            // once expanded, below. The collapsed state is a TextButton and the expanded state
            // is a Card - not one container that grows - so each gets its own AnimatedVisibility
            // rather than a single container animating open.
            AnimatedVisibility(visible = !expanded) {
                TextButton(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "Choose specific chapters",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.rotate(chevronRotation),
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(tween(300, easing = EmphasizedEasing)) + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column {
                    OrDivider()

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "Choose specific chapters",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                IconButton(onClick = { expanded = false }) {
                                    Icon(
                                        imageVector = Icons.Rounded.ExpandMore,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.rotate(chevronRotation),
                                    )
                                }
                            }

                            // One two-thumb slider replaces the old From/To Slider pair. `steps`
                            // is required, not optional: without it the slider is continuous and
                            // dragging the two thumbs together - the only way to reach
                            // ChapterSelection.One below - becomes practically unreachable.
                            // `steps` counts the values *between* the endpoints, hence
                            // totalChapters - 2. RangeSlider enforces start <= endInclusive
                            // itself, so unlike the old "To" Slider there's no need to clamp
                            // valueRange against chapterStart.
                            RangeSlider(
                                value = chapterStart.toFloat()..chapterEnd.toFloat(),
                                onValueChange = { range ->
                                    chapterStart = range.start.roundToInt()
                                    chapterEnd = range.endInclusive.roundToInt()
                                },
                                valueRange = 1f..totalChapters.toFloat(),
                                steps = (totalChapters - 2).coerceAtLeast(0),
                            )

                            // Track bounds, not the current selection - the selection is
                            // reflected in the button label below.
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = "1",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = "$totalChapters",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            FilledTonalButton(
                                onClick = {
                                    onUserConfirmation(
                                        if (chapterStart == chapterEnd) {
                                            ChapterSelection.One(chapterStart)
                                        } else {
                                            ChapterSelection.Range(chapterStart, chapterEnd)
                                        }
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = if (chapterStart == chapterEnd) {
                                        "Download chapter $chapterStart"
                                    } else {
                                        // U+2013 en dash, not a hyphen.
                                        "Download chapters $chapterStart–$chapterEnd"
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


@Preview(
    showBackground = true,
    showSystemUi = true,
    name = "Ask Confirmation (Light)",
)
@Composable
fun ConfirmChaptersLightPreview() {
    ReSyncTheme {
        ConfirmChapters(
            storyName = "The Story Name",
            authorName = "The Author Name",
            initialChapterNumber = 1,
            totalChapters = 42,
            driverType = DriverType.ArchiveOfOurOwn,
            onUserConfirmation = {},
        )
    }
}

@Preview(
    showBackground = true,
    showSystemUi = true,
    name = "Ask Confirmation (Dark)",
)
@Composable
fun ConfirmChaptersDarkPreview() {
    ReSyncTheme(darkTheme = true) {
        ConfirmChapters(
            storyName = "The Story Name",
            authorName = "The Author Name",
            initialChapterNumber = 1,
            totalChapters = 42,
            driverType = DriverType.ArchiveOfOurOwn,
            onUserConfirmation = {},
        )
    }
}

@Preview(
    showBackground = true,
    showSystemUi = true,
    name = "Ask Confirmation (Expanded)",
)
@Composable
fun ConfirmChaptersExpandedPreview() {
    ReSyncTheme {
        ConfirmChapters(
            storyName = "The Story Name",
            authorName = "The Author Name",
            initialChapterNumber = 1,
            totalChapters = 42,
            driverType = DriverType.ArchiveOfOurOwn,
            onUserConfirmation = {},
            initiallyExpanded = true,
        )
    }
}

@Composable
fun DownloadingRemainingChapters(
    storyName: String,
    currentlyDownloading: Int,
    totalToDownloads: Int,
    notice: String?,
    onCancel: () -> Unit = {},
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TaskStateHeader(title = storyName, onCancel = onCancel)

        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 24.dp),
        ) {

            // TODO We should change the current/total numbers to start from 0
            // Currently it's a bit strange when getting, say, the last 5 chapters
            // as the wheel's progression will start at the end.
            // eg. start at 45 and end ta 50, progression is from 90 to 100%.
            // Use the preview tool to understand what bounds we need, then create
            // value classes to enforce 0-indexed or 1-indexed value. Maybe.
            //
            // The raw ratio is backed by animateFloatAsState so the arc sweeps between chapters
            // instead of jumping a whole chapter's worth of progress at once.
            val animatedProgress by animateFloatAsState(
                targetValue = currentlyDownloading.toFloat() / totalToDownloads,
                animationSpec = tween(400),
                label = "downloadProgress",
            )
            CircularProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.size(64.dp),
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Fetching story",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "$currentlyDownloading of $totalToDownloads chapters",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (notice != null) {
                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = notice,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    )
                }
            }
        }
    }
}

@Preview(
    showBackground = true,
    name = "Downloading chapters (w/o notice, Light)"
)
@Composable
fun DownloadingRemainingChaptersPreview() {
    ReSyncTheme {
        DownloadingRemainingChapters(
            storyName = "The Story Name",
            currentlyDownloading = 8888,
            totalToDownloads = 9999,
            notice = null
        )
    }
}

@Preview(
    showBackground = true,
    name = "Downloading chapters (w/o notice, Dark)"
)
@Composable
fun DownloadingRemainingChaptersDarkPreview() {
    ReSyncTheme(darkTheme = true) {
        DownloadingRemainingChapters(
            storyName = "The Story Name",
            currentlyDownloading = 8888,
            totalToDownloads = 9999,
            notice = null
        )
    }
}

/**
 * Builds the "Copy error details" clipboard payload: a small provider/story/chapter table
 * followed by the raw stack trace. Uses [driverType]/[storyId]/[chapterId]'s underlying values
 * rather than the value classes' `toString()`, which would otherwise leak as e.g. `StoryId(id=…)`.
 */
private fun errorDetailsText(
    error: Throwable,
    driverType: DriverType,
    storyId: StoryId,
    chapterId: ChapterId,
): String {
    val chapter = chapterId.id?.toString() ?: "—"
    return "Provider  ${driverType.websiteName()}\n" +
            "Story ID  ${storyId.id}\n" +
            "Chapter   $chapter\n" +
            "\n" +
            error.stackTraceToString()
}

@Composable
fun DisplayDownloadError(
    error: Throwable,
    driverType: DriverType,
    storyId: StoryId,
    chapterId: ChapterId,
    onRetry: () -> Unit = {},
    onDone: () -> Unit = {},
    // Preview-only hook to render the expanded disclosure state; real callers never pass this.
    initiallyExpanded: Boolean = false,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    var expanded by remember { mutableStateOf(initiallyExpanded) }

    // One chevron vector, rotated 180 degrees when expanded, rather than swapping between
    // ExpandMore/ExpandLess assets.
    val detailsChevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "technicalDetailsChevron",
    )

    // Plays once on first composition of this screen, landing just after the ~300ms
    // Search<->Download / DownloadState screen transition settles rather than during it.
    val circleVisibleState = remember { MutableTransitionState(false).apply { targetState = true } }

    Column(
        verticalArrangement = if (expanded) Arrangement.Top else Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .then(if (expanded) Modifier.padding(top = 32.dp) else Modifier),
    ) {
        AnimatedVisibility(
            visibleState = circleVisibleState,
            enter = scaleIn(
                initialScale = 0.6f,
                animationSpec = tween(200, delayMillis = 150),
            ) + fadeIn(animationSpec = tween(200, delayMillis = 150)),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(28.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Something went wrong",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "We couldn't finish reading this chapter. This is usually temporary — try again in a moment.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Try again")
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Close")
        }

        TextButton(
            onClick = {
                val details = errorDetailsText(error, driverType, storyId, chapterId)
                scope.launch {
                    clipboard.setClipEntry(
                        ClipEntry(ClipData.newPlainText("reSync error", details))
                    )
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                        Toast.makeText(context, "Error details copied", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Copy error details")
        }

        TextButton(
            onClick = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Technical details")
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Rounded.ExpandMore,
                contentDescription = null,
                modifier = Modifier.rotate(detailsChevronRotation),
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(tween(300, easing = EmphasizedEasing)) + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column {
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = errorDetailsText(error, driverType, storyId, chapterId),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 20.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Start,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    )
                }
            }
        }
    }
}

@Preview(
    showBackground = true,
    name = "Downloading chapters (with notice, Light)"
)
@Composable
fun DownloadingRemainingChaptersNoticePreview() {
    ReSyncTheme {
        DownloadingRemainingChapters(
            storyName = "The Story Name",
            currentlyDownloading = 2,
            totalToDownloads = 3,
            notice = "AO3 rate limit hit (1 time)\nWaiting 90sec before resuming download."
        )
    }
}

@Preview(
    showBackground = true,
    name = "Downloading chapters (with notice, Dark)"
)
@Composable
fun DownloadingRemainingChaptersNoticeDarkPreview() {
    ReSyncTheme(darkTheme = true) {
        DownloadingRemainingChapters(
            storyName = "The Story Name",
            currentlyDownloading = 2,
            totalToDownloads = 3,
            notice = "AO3 rate limit hit (1 time)\nWaiting 90sec before resuming download."
        )
    }
}

@Preview(
    showBackground = true,
    name = "Download failed (Light)"
)
@Composable
fun DisplayDownloadErrorPreview() {
    val exception = NumberFormatException("For input string: \"\"")

    ReSyncTheme {
        DisplayDownloadError(
            error = exception,
            storyId = StoryId(27855042),
            chapterId = ChapterId(68198782),
            driverType = DriverType.ArchiveOfOurOwn
        )
    }
}

@Preview(
    showBackground = true,
    name = "Download failed (Dark)"
)
@Composable
fun DisplayDownloadErrorDarkPreview() {
    val exception = NumberFormatException("For input string: \"\"")

    ReSyncTheme(darkTheme = true) {
        DisplayDownloadError(
            error = exception,
            storyId = StoryId(27855042),
            chapterId = ChapterId(68198782),
            driverType = DriverType.ArchiveOfOurOwn
        )
    }
}

@Preview(
    showBackground = true,
    name = "Download failed (Details expanded, Light)"
)
@Composable
fun DisplayDownloadErrorExpandedPreview() {
    val exception = NumberFormatException("For input string: \"\"")

    ReSyncTheme {
        DisplayDownloadError(
            error = exception,
            storyId = StoryId(27855042),
            chapterId = ChapterId(68198782),
            driverType = DriverType.ArchiveOfOurOwn,
            initiallyExpanded = true,
        )
    }
}

@Preview(
    showBackground = true,
    name = "Download failed (Details expanded, Dark)"
)
@Composable
fun DisplayDownloadErrorExpandedDarkPreview() {
    val exception = NumberFormatException("For input string: \"\"")

    ReSyncTheme(darkTheme = true) {
        DisplayDownloadError(
            error = exception,
            storyId = StoryId(27855042),
            chapterId = ChapterId(68198782),
            driverType = DriverType.ArchiveOfOurOwn,
            initiallyExpanded = true,
        )
    }
}

/**
 * The end of the download flow: a static (no entrance animation) confirmation that the epub was
 * built, with sharing as an explicit action rather than an automatic side effect of the download
 * completing. The tonal circle scales/fades in on first composition (see [DownloadSuccess]'s
 * `circleVisibleState`), landing just after the screen transition into this state settles.
 */
@Composable
fun DownloadSuccess(
    summary: String,
    onShare: () -> Unit,
    onDone: () -> Unit,
) {
    // Plays once on first composition of this screen, landing just after the ~300ms
    // Search<->Download / DownloadState screen transition settles rather than during it.
    val circleVisibleState = remember { MutableTransitionState(false).apply { targetState = true } }

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
    ) {
        AnimatedVisibility(
            visibleState = circleVisibleState,
            enter = scaleIn(
                initialScale = 0.6f,
                animationSpec = tween(200, delayMillis = 150),
            ) + fadeIn(animationSpec = tween(200, delayMillis = 150)),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(28.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Story ready",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = summary,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onShare,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = Icons.Rounded.Share,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Share to reMarkable")
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Done")
        }
    }
}

@Preview(
    showBackground = true,
    name = "Success (whole story, Light)"
)
@Composable
fun DownloadSuccessWholeStoryPreview() {
    ReSyncTheme {
        DownloadSuccess(
            summary = "The Story Name saved as an EPUB, ready to send to reMarkable.",
            onShare = {},
            onDone = {},
        )
    }
}

@Preview(
    showBackground = true,
    name = "Success (single chapter, Light)"
)
@Composable
fun DownloadSuccessSingleChapterPreview() {
    ReSyncTheme {
        DownloadSuccess(
            summary = "The Story Name — chapter 3 saved as an EPUB, ready to send to reMarkable.",
            onShare = {},
            onDone = {},
        )
    }
}

@Preview(
    showBackground = true,
    name = "Success (range, Light)"
)
@Composable
fun DownloadSuccessRangePreview() {
    ReSyncTheme {
        DownloadSuccess(
            summary = "The Story Name — chapters 3–7 saved as an EPUB, ready to send to reMarkable.",
            onShare = {},
            onDone = {},
        )
    }
}

@Preview(
    showBackground = true,
    name = "Success (range, Dark)"
)
@Composable
fun DownloadSuccessRangeDarkPreview() {
    ReSyncTheme(darkTheme = true) {
        DownloadSuccess(
            summary = "The Story Name — chapters 3–7 saved as an EPUB, ready to send to reMarkable.",
            onShare = {},
            onDone = {},
        )
    }
}

