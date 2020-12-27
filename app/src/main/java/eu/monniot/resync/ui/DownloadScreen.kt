package eu.monniot.resync.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import eu.monniot.resync.makeEpub
import eu.monniot.resync.rmcloud.RmClient
import eu.monniot.resync.rmcloud.RmCloud
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay

@Composable
fun DownloadScreen(
    storyId: StoryId,
    chapterSelection: ChapterSelection,
    askConfirmation: Boolean,
    onDone: () -> Unit
) {

    val state: MutableState<LinkCollectionState> =
        remember {
            mutableStateOf(
                FetchFirstChapter(
                    onDone,
                    storyId,
                    chapterSelection,
                    askConfirmation,
                    noWebView = false, // TODO Remove once loader issue fixed
                )
            )
        }

    val screen = state.value
    println("calling .Screen on $screen")
    screen.Screen(state = state)
}


// implementation details below: state machine for the download screen

sealed class LinkCollectionState {
    @Composable
    abstract fun Screen(state: MutableState<LinkCollectionState>)
}

// Helper method to simplify the state machinery in previews
// This should mimic the setup in DownloadScreen
@Composable
fun LinkCollectionStatePreviewContainer(lcs: LinkCollectionState) {
    val state: MutableState<LinkCollectionState> =
        remember {
            mutableStateOf(
                lcs
            )
        }

    state.value.Screen(state)
}

class FetchFirstChapter(
    private val onDone: () -> Unit,
    private val storyId: StoryId,
    private val chapterSelection: ChapterSelection,
    private val askConfirmation: Boolean,
    private val noWebView: Boolean = false
) :
    LinkCollectionState() {

    // TODO For some reason the screen sometimes goes blank (white)
    // It seems to be related to the AndroidView/WebView as it render normally
    // once noWebView is set to true.
    // Wait for compose next alphas (or beta) before investing time in debugging this issue
    @Composable
    override fun Screen(state: MutableState<LinkCollectionState>) {

        println("FetchFirstChapter.noWebView = $noWebView")

        // TODO Can we make those class member ?
        val deferred = CompletableDeferred<List<Chapter>>()

        // TODO See if we can put this into a separate method (to simplify logic testing)
        LaunchedEffect(subject = storyId) {
            val chapters = deferred.await()
            val firstChapter = chapters[0]

            if (firstChapter.totalChapters == 1) {
                // There is only one chapter, choice of story/chapter is the same
                state.value = BuildAndUpload(onDone, storyId, listOf(firstChapter))
            } else {

                if (askConfirmation) {
                    state.value = AskConfirmation(onDone, storyId, firstChapter)
                } else {
                    // Automatic download

                    val last = chapterSelection.lastChapter(firstChapter.totalChapters)

                    if (last != null && last > firstChapter.num) {
                        // More chapters needs to be downloaded
                        state.value = FetchAllChapters(onDone, storyId)
                    } else {
                        // Specific chapter selected, building
                        state.value = BuildAndUpload(onDone, storyId, listOf(firstChapter))
                    }
                }
            }

            println("state.value = ${state.value}")
        }

        Box {

            // Hack to not render WebView in @Preview mode
            if (!noWebView) {
                GetChaptersView(storyId, chapterSelection, deferred)
            }

            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().fillMaxHeight(),
            ) {

                Text(
                    text = "Looking up Story",
                    style = MaterialTheme.typography.h6,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "(id: $storyId | Chapter: $chapterSelection)",
                    style = MaterialTheme.typography.body2,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )

                Spacer(modifier = Modifier.height(8.dp))

                CircularProgressIndicator(
                    modifier = Modifier.width(100.dp).height(100.dp)
                        .align(Alignment.CenterHorizontally)
                )

            }
        }
    }
}


@Preview(
    showBackground = true,
    device = Devices.PIXEL_3,
    showSystemUi = true,
    name = "Download - Fetch first chapter - Pixel 3"
)
@Composable
fun FetchFirstPreview() {
    LinkCollectionStatePreviewContainer(
        FetchFirstChapter(
            onDone = {},
            storyId = 1,
            chapterSelection = ChapterSelection.All,
            askConfirmation = true,
            noWebView = true
        )
    )
}

class AskConfirmation(
    private val onDone: () -> Unit,
    private val storyId: StoryId,
    private val firstChapter: Chapter,
) : LinkCollectionState() {

    private var chapterStart by mutableStateOf(firstChapter.num)
    private var chapterEnd by mutableStateOf(firstChapter.totalChapters)

    @Composable
    fun StoryDetails() {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = firstChapter.storyName, style = MaterialTheme.typography.h3)

            Row {
                Text(text = "By", style = MaterialTheme.typography.subtitle1)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = firstChapter.author,
                    style = MaterialTheme.typography.subtitle1.copy(fontStyle = FontStyle.Italic)
                )
            }

            // TODO Chapter name ?
            Text(text = if (firstChapter.totalChapters > 1) "${firstChapter.totalChapters} chapters" else "One Shot")
        }
    }

    @Composable
    fun SyncChoice(onStorySelected: () -> Unit, onChapterSelected: (ChapterSelection) -> Unit) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Surface(elevation = 1.dp) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(onClick = onStorySelected) {
                        Text(text = "Synchronise entire story")
                    }
                }
            }

            Text("OR", modifier = Modifier.padding(8.dp))

            Surface(elevation = 1.dp) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Button(onClick = {
                        onChapterSelected(
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

                    // First Chapter
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("From", modifier = Modifier.width(64.dp))
                        Slider(
                            value = chapterStart.toFloat(),
                            onValueChange = { chapterStart = it.toInt() },
                            valueRange = 1f..firstChapter.totalChapters.toFloat(),
                        )
                    }

                    // Last chapter
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("To", modifier = Modifier.width(64.dp))
                        Slider(
                            value = chapterEnd.toFloat(),
                            onValueChange = { chapterEnd = it.toInt() },
                            valueRange = chapterStart.toFloat()..firstChapter.totalChapters.toFloat(),
                        )
                    }
                }
            }
        }
    }

    @Composable
    override fun Screen(state: MutableState<LinkCollectionState>) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(0.dp, 40.dp, 0.dp, 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            StoryDetails()

            Spacer(modifier = Modifier.weight(1f).fillMaxWidth())

            SyncChoice(
                onStorySelected = {
                    if (firstChapter.totalChapters > 1) {
                        state.value = FetchAllChapters(onDone, storyId)
                    } else {
                        state.value = BuildAndUpload(onDone, storyId, listOf(firstChapter))
                    }
                }, onChapterSelected = {
                    state.value = BuildAndUpload(onDone, storyId, listOf(firstChapter))
                }
            )
        }
    }
}

@Preview(
    showBackground = true,
    device = Devices.PIXEL_3,
    showSystemUi = true,
    name = "Download - Ask Confirmation - Pixel 3"
)
@Composable
fun AskConfirmationPreview() {
    val chapter =
        Chapter(1, "The Chapter Title", "The Story Name", "The Author", 42, "My super story. ")
    LinkCollectionStatePreviewContainer(
        AskConfirmation({}, 42, chapter)
    )
}

@Preview(
    showBackground = true,
    name = "Details - Ask Confirmation View"
)
@Composable
fun AskConfirmationDetailsPreview() {
    val chapter =
        Chapter(1, "The Chapter Title", "The Story Name", "The Author", 42, "My super story. ")
    val ask = AskConfirmation({}, 42, chapter)

    ask.StoryDetails()
}

@Preview(
    showBackground = true,
    name = "Sync - Ask Confirmation View"
)
@Composable
fun AskConfirmationSyncPreview() {
    val chapter =
        Chapter(1, "The Chapter Title", "The Story Name", "The Author", 42, "My super story. ")
    val ask = AskConfirmation({}, 42, chapter)

    ask.SyncChoice(onStorySelected = {}, onChapterSelected = {})
}


class FetchAllChapters(private val onDone: () -> Unit, private val storyId: StoryId) :
    LinkCollectionState() {
    @Composable
    override fun Screen(state: MutableState<LinkCollectionState>) {
        val deferred = CompletableDeferred<List<Chapter>>()

        LaunchedEffect(subject = storyId) {
            val chapters = deferred.await()

            state.value = BuildAndUpload(onDone, storyId, chapters)
        }

        Column {
            Text(text = "Fetching Story")

            GetChaptersView(storyId, ChapterSelection.All, deferred)
        }
    }
}

class BuildAndUpload(
    private val onDone: () -> Unit,
    private val storyId: StoryId,
    private val chapters: List<Chapter>,
) : LinkCollectionState() {

    // TODO suspend and load device token from storage (as well as existing user token)
    private val rmCloud = RmClient(RmCloud.DEVICE_TOKEN)

    @Composable
    override fun Screen(state: MutableState<LinkCollectionState>) {

        LaunchedEffect(storyId) {
            val epub = makeEpub(chapters)

            val fileName = if (chapters.size > 1) {
                "${chapters[0].storyName}.epub"
            } else {
                "${chapters[0].storyName} - Ch ${chapters[0].num}.epub"
            }

            rmCloud.uploadEpub(fileName, epub)

            state.value = Done(onDone)
        }

        Text("Uploading to the reMarkable Cloud")
    }
}

class Done(val onDone: () -> Unit) : LinkCollectionState() {
    @Composable
    override fun Screen(state: MutableState<LinkCollectionState>) {

        var countDown by remember { mutableStateOf(3) }

        LaunchedEffect(subject = this) {

            while (countDown > 0) {
                delay(1000)
                countDown -= 1
            }

            onDone()
        }

        Column {
            Text("Done")
            Text("Closing in $countDown second${if (countDown > 1) "s" else ""}")
        }


    }
}