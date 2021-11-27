package eu.monniot.resync.ui.downloader

import android.content.Context
import android.webkit.WebView
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import eu.monniot.resync.FileName
import eu.monniot.resync.downloader.ArchiveOfOurOwnDriver
import eu.monniot.resync.downloader.Driver
import eu.monniot.resync.downloader.DriverType
import eu.monniot.resync.downloader.FanFictionNetDriver
import eu.monniot.resync.makeEpub
import eu.monniot.resync.rmcloud.RmClient
import eu.monniot.resync.rmcloud.readTokens
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay

@Composable
fun DownloadScreen(
    driverType: DriverType,
    storyId: StoryId,
    chapterId: ChapterId,
    noWebView: Boolean = false,
    onDone: () -> Unit
) {
    val context = LocalContext.current

    val driver = when (driverType) {
        DriverType.ArchiveOfOurOwn -> ArchiveOfOurOwnDriver()
        DriverType.FanFictionNet -> FanFictionNetDriver()
    }

    val (state, setState) = remember {
        mutableStateOf<DownloadState>(
            DownloadState.FetchingFirstChapter(
                storyId,
                chapterId
            )
        )
    }

    LaunchedEffect(key1 = storyId, key2 = chapterId) {
        try {
            // wait for the driver to be attached to a running WebView
            driver.ready()

            downloadLogic(context, storyId, chapterId, driverType, driver, setState)
        } finally {
            onDone()
        }
    }

    Box {

        // Hack to not render WebView in @Preview mode and avoid crashes
        if (!noWebView) {
            // We need a web view to grab some fiction content.
            // This is mainly to go around the CloudFlare protection that FF.Net have.
            // By using a web view, we are seen as "normal" web traffic.
            AndroidView(factory = ::WebView, modifier = Modifier.size(1.dp, 1.dp)) { webView ->
                driver.installGrabber(webView)
            }
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
    val knownChapters: Map<Int, ChapterId> = emptyMap() // TODO initialChapter.knownChapters

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
                    setState(DownloadState.DownloadingRemainingChapters(1, 1))
                    val id = knownChapters[chapterSelection.chapter]!!
                    val chapter = driver.readChapter(storyId, id)

                    // The user only want the selected chapter, remove the initial one
                    chaptersInEpub.clear()
                    chaptersInEpub.add(chapter)
                }

            // TODO Factor together All and Range as they are very similar
            ChapterSelection.All -> {
                // The user wants everything, let's iterate over the known chapters

                val setDlState = { index: Int ->
                    setState(
                        DownloadState.DownloadingRemainingChapters(
                            index,
                            initialChapter.totalChapters - 1
                        )
                    )
                }

                // We filter out the chapter we have already downloaded
                knownChapters.values
                    .filter { it != initialChapter.chapterId }
                    .forEachIndexed { index, id ->
                        // TODO Check if the index is correctly aligned (0 or 1)
                        setDlState(index)

                        val chapter = driver.readChapter(storyId, id)
                        chaptersInEpub.add(chapter)

                    }
            }
            is ChapterSelection.Range -> {
                val toDownload = knownChapters
                    .filter { (num, id) -> chapterSelection.contains(num) && initialChapter.chapterId != id }

                val setDlState = { index: Int ->
                    setState(
                        DownloadState.DownloadingRemainingChapters(
                            index,
                            toDownload.size
                        )
                    )
                }

                toDownload.values.forEachIndexed { index, id ->
                    // TODO Check if the index is correctly aligned (0 or 1)
                    setDlState(index)

                    val chapter = driver.readChapter(storyId, id)
                    chaptersInEpub.add(chapter)
                }
            }
        }

    } // end of multiple chapters flow

    setState(DownloadState.BuildingAndUploading)

    // Make sure that we put the chapters in order
    chaptersInEpub.sortBy { it.num }

    // TODO Change makeEpub (or create a temporary alternative) to accept new Chapter class
    // The current code does not work and is just a quick compiler fix to let me make
    // progress on the rest of this function
    @Suppress("UNCHECKED_CAST") val epub =
        makeEpub(chaptersInEpub as List<eu.monniot.resync.ui.Chapter>)
    val fileName = FileName.make(chaptersInEpub, wholeStory)
    val tokens = readTokens(context)

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
    ) : DownloadState

    object BuildingAndUploading : DownloadState
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
            text = "(id: $storyId | Chapter: $chapterId)",
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

@Composable
fun ConfirmChapters(
    storyName: String,
    authorName: String,
    initialChapterNumber: Int,
    totalChapters: Int,
    driverType: DriverType,
    onUserConfirmation: (ChapterSelection) -> Unit
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

@Composable
fun DownloadingRemainingChapters(
    currentlyDownloading: Int,
    totalToDownloads: Int
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
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .width(100.dp)
                .height(100.dp)
                .align(Alignment.CenterHorizontally),
            contentAlignment = Alignment.Center
        ) {

            // TODO We should change the current/total numbers to start from 0
            // Currently it's a bit strange when getting, say, the last 5 chapters
            // as the wheel's progression will start at the end.
            // eg. start at 45 and end ta 50, progression is from 90 to 100%.
            CircularProgressIndicator(
                progress = currentlyDownloading.toFloat() / totalToDownloads,
                modifier = Modifier
                    .fillMaxSize()
            )
            Text(
                text = "$currentlyDownloading/${totalToDownloads} chapters",
                style = MaterialTheme.typography.body2,
            )
        }

    }
}
