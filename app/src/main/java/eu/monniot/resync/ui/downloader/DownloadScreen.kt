package eu.monniot.resync.ui.downloader

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.FileProvider
import eu.monniot.resync.BuildConfig
import eu.monniot.resync.FileName
import eu.monniot.resync.downloader.*
import eu.monniot.resync.makeEpub
import eu.monniot.resync.rmcloud.PreferencesManager
import eu.monniot.resync.rmcloud.RmClient
import eu.monniot.resync.ui.KeepScreenOn
import eu.monniot.resync.ui.ReSyncTheme
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
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

    val driver = when (driverType) {
        DriverType.ArchiveOfOurOwn -> ArchiveOfOurOwnDriver(context.filesDir)
        DriverType.FanFictionNet -> FanFictionNetDriver(context.filesDir)
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

    LaunchedEffect(key1 = storyId, key2 = chapterId) {
        try {
            // wait for the driver to be attached to a running WebView
            driver.ready()

            downloadLogic(context, storyId, chapterId, driverType, driver, setState)

            // Only call onDone if there was no error, otherwise let the user
            // choose when to close the app.
            onDone()
        } catch (e: Throwable) {
            println("Error caught when downloading story")
            e.printStackTrace()
            setState(DownloadState.Error(e))
        }
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

        when (state) {
            is DownloadState.FetchingFirstChapter -> FetchingFirstChapterView(
                storyId = state.storyId,
                chapterId = state.chapterId,
            )
            is DownloadState.ConfirmChapters -> ConfirmChapters(
                state.storyName,
                state.authorName,
                state.initialChapterNumber,
                state.totalChapters,
                state.driverType,
                state.onUserConfirmation,
            )
            is DownloadState.DownloadingRemainingChapters -> DownloadingRemainingChapters(
                currentlyDownloading = state.currentlyDownloading,
                totalToDownloads = state.totalToDownloads,
                notice = state.notice,
            )
            is DownloadState.Error -> DisplayDownloadError(
                error = state.throwable,
                driverType,
                storyId,
                chapterId
            )
            DownloadState.BuildingAndUploading -> Text("Uploading to the reMarkable Cloud")
            DownloadState.Done -> Text("The story is now available on your tablet")
        }
    }
}

/*
    Flow is:
    1. Fetch chapter storyId/chapterId
    2. if total chapters == 1, skip this step
       Otherwise let user choose what to download
    3. If more than one chapter selected, download remaining chapters
    4. Build epub and upload to rm cloud
     */
suspend fun downloadLogic(
    context: Context,
    storyId: StoryId,
    chapterId: ChapterId,
    driverType: DriverType,
    driver: Driver,
    setState: (DownloadState) -> Unit,
) {

    // initial state is FetchingFirstChapter, no need to re-set it here
    val initialChapter = driver.readChapter(storyId, chapterId)
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
                    setState(DownloadState.DownloadingRemainingChapters(1, 1, null))
                    val id = knownChapters[chapterSelection.chapter]!!

                    val chapter = readWithRateLimit(
                        { driver.readChapter(storyId, id) },
                        { setState(DownloadState.DownloadingRemainingChapters(1, 1, it)) }
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

                        // TODO Investigate if storing chapters on disk is a good idea or not
                        // Mostly because with AO3's RL dl a story can now take a long time,
                        // increasing the risk of loosing work/time.

                        // AO3 has a vague definition of what they consider normal usage.
                        // Let's wait 5 seconds between each chapter. It's more or less like
                        // a human going over each chapter and checking the first sentence.
                        if (driverType == DriverType.ArchiveOfOurOwn) {
                            delay(1000)
                        }

                        val chapter = readWithRateLimit(
                            { driver.readChapter(storyId, id) },
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
                        { driver.readChapter(storyId, id) },
                        { setDlState(index, it) }
                    )

                    chaptersInEpub.add(chapter)
                }
            }
        }

    } // end of multiple chapters flow

    setState(DownloadState.BuildingAndUploading)

    // Make sure that we put the chapters in order
    chaptersInEpub.sortBy { it.num }

    // Build the epub file and its name
    val epub = makeEpub(chaptersInEpub)
    val fileName = FileName.make(chaptersInEpub, wholeStory)

    // TODO Inject PreferencesManager as parameter
    // Which means we will be able to test this function as unit test
    // without mocking the android framework
    val preferencesManager = PreferencesManager.create(context)

    when (preferencesManager.readUploadMethod()) {
        PreferencesManager.Companion.UploadMethod.Direct -> {
            Log.d(TAG, "Upload to reMarkable cloud directly")
            val tokens = preferencesManager.readCurrentAccount().tokens

            if (tokens == null) {
                // TODO save epub for later and display it in the LauncherActivity
                // Also maybe have a custom screen to tell the user what happened ?
            } else {
                val rmCloud = RmClient(tokens)
                rmCloud.uploadEpub(fileName, epub)
            }

            setState(DownloadState.Done)

            // Wait for the done animation to be complete
            // The activity will be close once this function return
            delay(1500)
        }
        PreferencesManager.Companion.UploadMethod.Share -> {
            Log.d(TAG, "Upload via Android Share")
            // Read how to do so at https://developer.android.com/training/secure-file-sharing
            // 1. Save the epub on file system
            val epubFile = context.filesDir.resolve("epub/$fileName")
            epubFile.parentFile?.mkdir()
            epubFile.writeBytes(epub)

            // 2. Initiate intent to share epub file
            val fileUri = FileProvider.getUriForFile(
                context,
                "eu.monniot.resync.fileprovider",
                epubFile
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

            startActivity(context, Intent.createChooser(shareIntent, "Share Story"), null)
        }
    }
}

private suspend fun readWithRateLimit(
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

private fun ao3RLNotice(limitHit: Int, remainingSeconds: Int): String {
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
        val currentlyDownloading: Int,
        val totalToDownloads: Int,
        val notice: String?,
    ) : DownloadState

    data class Error(val throwable: Throwable) : DownloadState

    object BuildingAndUploading : DownloadState

    // TODO Add remaining time until screen is removed
    object Done : DownloadState
}


@Composable
fun FetchingFirstChapterView(storyId: StoryId, chapterId: ChapterId) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
    ) {

        Text(
            text = "Looking up Story",
            style = MaterialTheme.typography.h6,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "(id: ${storyId.id} | Chapter: ${chapterId.id})",
            style = MaterialTheme.typography.body2,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )

        Spacer(modifier = Modifier.height(8.dp))

        CircularProgressIndicator(
            modifier = Modifier
                .width(100.dp)
                .height(100.dp)
                .align(Alignment.CenterHorizontally)
        )

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

@Composable
fun ConfirmChapters(
    storyName: String,
    authorName: String,
    initialChapterNumber: Int,
    totalChapters: Int,
    driverType: DriverType,
    onUserConfirmation: (ChapterSelection) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(0.dp, 40.dp, 0.dp, 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // Story Details
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = storyName, style = MaterialTheme.typography.h3)

            Row {
                Text(text = "By", style = MaterialTheme.typography.subtitle1)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = authorName,
                    style = MaterialTheme.typography.subtitle1.copy(fontStyle = FontStyle.Italic)
                )
            }

            // TODO Chapter name ?
            Text(text = if (totalChapters > 1) "$totalChapters chapters" else "One Shot")
            Text(text = "From: ${driverType.websiteName()}")
        }

        Spacer(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )

        // Synchronisation choice
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Surface(elevation = 1.dp) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(onClick = { onUserConfirmation(ChapterSelection.All) }) {
                        Text(text = "Synchronise entire story")
                    }
                }
            }

            Text("OR", modifier = Modifier.padding(8.dp))

            Surface(elevation = 1.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp, 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    var chapterStart by remember { mutableStateOf(initialChapterNumber) }
                    var chapterEnd by remember { mutableStateOf(totalChapters) }

                    Button(onClick = {
                        onUserConfirmation(
                            if (chapterStart == chapterEnd) {
                                ChapterSelection.One(chapterStart)
                            } else {
                                ChapterSelection.Range(chapterStart, chapterEnd)
                            }
                        )
                    }) {
                        val ch =
                            if (chapterStart != chapterEnd) "$chapterStart - $chapterEnd" else "$chapterStart"
                        Text(text = "Synchronise specific chapters ($ch)")
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // First chapter to download
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("From", modifier = Modifier.width(64.dp))
                        Slider(
                            value = chapterStart.toFloat(),
                            onValueChange = { chapterStart = it.toInt() },
                            valueRange = 1f..totalChapters.toFloat(),
                        )
                    }

                    // Last chapter to download
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("To", modifier = Modifier.width(64.dp))
                        Slider(
                            value = chapterEnd.toFloat(),
                            onValueChange = { chapterEnd = it.toInt() },
                            valueRange = chapterStart.toFloat()..totalChapters.toFloat(),
                        )
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

@Composable
fun DownloadingRemainingChapters(
    currentlyDownloading: Int,
    totalToDownloads: Int,
    notice: String?,
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
    ) {

        Text(
            text = "Fetching Story",
            style = MaterialTheme.typography.h6,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .width(100.dp)
                .height(100.dp),
            contentAlignment = Alignment.Center
        ) {

            // TODO We should change the current/total numbers to start from 0
            // Currently it's a bit strange when getting, say, the last 5 chapters
            // as the wheel's progression will start at the end.
            // eg. start at 45 and end ta 50, progression is from 90 to 100%.
            // Use the preview tool to understand what bounds we need, then create
            // value classes to enforce 0-indexed or 1-indexed value. Maybe.
            CircularProgressIndicator(
                progress = currentlyDownloading.toFloat() / totalToDownloads,
                modifier = Modifier
                    .fillMaxSize()
            )
            Text(
                text = "$currentlyDownloading/${totalToDownloads}\nchapters",
                style = MaterialTheme.typography.body2,
                textAlign = TextAlign.Center,
            )
        }

        if (notice != null) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = notice,
                style = MaterialTheme.typography.body2,
                textAlign = TextAlign.Center,
            )
        }

    }
}

@Preview(
    showBackground = true,
    name = "Downloading chapters (w/o notice)"
)
@Composable
fun DownloadingRemainingChaptersPreview() {
    ReSyncTheme {
        DownloadingRemainingChapters(
            currentlyDownloading = 8888,
            totalToDownloads = 9999,
            notice = null
        )
    }
}

@Composable
fun DisplayDownloadError(
    error: Throwable,
    driverType: DriverType,
    storyId: StoryId,
    chapterId: ChapterId,
) {
    val state = rememberScrollState()

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .verticalScroll(state),
    ) {

        Text(
            text = "Error while downloading story",
            style = MaterialTheme.typography.h6,
        )

        Text(
            text = "$storyId; $chapterId; DriverType($driverType)",
            style = MaterialTheme.typography.body2,
            textAlign = TextAlign.Center,
        )

        Text(error.stackTraceToString())
    }
}

@Preview(
    showBackground = true,
    name = "Downloading chapters (with notice)"
)
@Composable
fun DownloadingRemainingChaptersNoticePreview() {
    ReSyncTheme {
        DownloadingRemainingChapters(
            currentlyDownloading = 2,
            totalToDownloads = 3,
            notice = "AO3 rate limit hit (1 time)\nWaiting 90sec before resuming download."
        )
    }
}

@Preview(
    showBackground = true,
    name = "Download failed"
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
