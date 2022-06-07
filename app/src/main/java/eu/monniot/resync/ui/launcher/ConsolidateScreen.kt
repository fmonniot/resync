package eu.monniot.resync.ui.launcher

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import eu.monniot.resync.database.Document
import eu.monniot.resync.database.DocumentsDao
import eu.monniot.resync.database.RemarkableDatabase
import eu.monniot.resync.ui.ReSyncTheme
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.unit.dp
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import eu.monniot.resync.FileName
import eu.monniot.resync.rmcloud.PreferencesManager
import eu.monniot.resync.rmcloud.RmClient
import kotlinx.coroutines.flow.*

// TODO Add a way to group together existing stories.
// As time pass, I found out that I have a lot of epub
// generated for the same story: generally one per chapter.
// It would be nice to offer a UI showing all fragmented
// stories and offer a way to merge them into one file.
// A Story Defragmenter of sort :grin:
@Composable
fun ConsolidateScreen() {
    val model: ConsolidateViewModel = viewModel()

    val initialized by model.initialized
    val documents by model.documents.collectAsState(emptyList())
    val refreshing by model.refreshing.collectAsState()

    ConsolidateView(initialized, refreshing, documents) {
        model.refreshDocuments()
    }

    /* TODO List of steps
    // -- Room
    /v  1. Create a database package
    /v  2. Create a data class for the documents
    /v  3. Create a DAO interface to insert/update/delete documents
    /v  4. Add a Query to the DAO to list documents within a certain folder
    /v  5. Create a Database class
    // -- ViewModel
    /v  6. Create a ViewModel class
    //  7. load the parameter containing the parent folder
    //  8. offer a simple boolean interface if not set
    /v  9. provides the interface to expose LiveData/State of local documents
    /v 10. A function to trigger a refresh of the documents from the cloud
    // -- View
    // 11. If first time, display a loading screen when fetching documents
    // 12. Use this function to display the parent selection screen if none selected
    // 13. Otherwise list all documents
    // At that point we are done with the basics. We can now think on how
    // we are going to present the consolidation UI ?
    // Idea 1: Don't display all documents, but only those that have common
    //         name before the " - Ch xx-xx" suffix.
    //         We can then display them like
    //           Line 1: Story Name
    //           Line 2: Ch 1, 2, 3-7, …
    //         Tapping on an item opens up a bottom screen asking how to
    //         consolidate that story ? At first only downloading entire story
    //         will be proposed but in time we might want to merge existing
    //         documents. Not entirely certain which one make more sense.
    //         Might also be interesting to provides missing chapters. Stg like
    //           Ch 1 to 15 in 11 documents.
    //           Missing 4 & 7.
    */
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ConsolidateView(
    initialized: ViewState,
    refreshing: Boolean,
    documents: List<GroupedDocument>,
    onRefresh: () -> Unit = {},
) {
    when (initialized) {
        ViewState.NoAccount ->
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Rounded.Warning,
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .aspectRatio(1f)
                        .padding(bottom = 16.dp),
                    contentDescription = "No reMarkable account set",
                    tint = MaterialTheme.colors.onSurface.copy(alpha = 0.18f)
                )

                Text("No reMarkable account set")
            }

        ViewState.NotInitialized ->
            // TODO Is this feature still something I need ?
            Column {
                Text("TODO: Select a folder")
            }

        ViewState.Ok -> {

            val modalBottomSheetState = rememberModalBottomSheetState(
                initialValue = ModalBottomSheetValue.Hidden
            )
            val coroutineScope = rememberCoroutineScope()

            var bottomSheetDocument by remember { mutableStateOf<GroupedDocument?>(null) }

            ModalBottomSheetLayout(
                sheetState = modalBottomSheetState,
                sheetContent = {
                    val doc = bottomSheetDocument
                    if (doc == null) {
                        Text("No document selected")
                    } else {
                        DocumentBottomSheetView(doc)
                    }
                }
            ) {

                SwipeRefresh(
                    modifier = Modifier.fillMaxWidth(),
                    state = rememberSwipeRefreshState(refreshing),
                    onRefresh = onRefresh,
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (documents.isEmpty()) {
                            // TODO Fill max height, otherwise the pull is hard to do
                            // (and you have to know the trick)
                            item {
                                Text("No documents yet, pull to refresh")
                            }
                        } else {

                            // TODO Sort and group documents alphabetically
                            items(documents) { doc ->
                                ListItem(
                                    text = { Text(doc.title) },
                                    secondaryText = {
                                        // TODO Join continuous chapters (eg. 1, 2, 3 as 1-3, or 1,2,3,5 as 1-3,5)
                                        // See also GroupedDocument data class
                                        val text =
                                            doc.chapters.joinToString { FileName.formatChapters(it) }
                                        Text(text)
                                    },
                                    modifier = Modifier.clickable {
                                        bottomSheetDocument = doc
                                        coroutineScope.launch {
                                            modalBottomSheetState.show()
                                        }
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

// The ColumnScope received isn't used, but I do want it to have the
// compiler remind me that this function needs to be within a vertical
// alignment (i.e. a Column).
@Suppress("unused")
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun @Composable ColumnScope.DocumentBottomSheetView(document: GroupedDocument) {

    ListItem(
        text = {
            Text(
                text = document.title,
                style = MaterialTheme.typography.h6
            )
        },
        overlineText = {
            Text(
                text = "Story"
            )
        }
    )

    ListItem(
        text = {
            val text =
                document.chapters.joinToString {
                    FileName.formatChapters(
                        it,
                        withPrefix = true
                    )
                }

            Text(text)
        },
        overlineText = {
            Text(
                text = "Files to consolidate"
            )
        }
    )

    // Arrow direction depends on text direction, as icon/trailing will probably be reversed
    // TODO Might make sense to create our own component instead of trying to retrofit ListItem
    // Look at ListItem and OneLine.ListItem
    ListItem(
        modifier = Modifier
            .background(MaterialTheme.colors.primary)
            .clickable {
                println("Consolidate it !")
            },
        icon = {
            Icon(
                Icons.Rounded.KeyboardArrowRight,
                contentDescription = "Consolidate the story",
                tint = MaterialTheme.colors.onPrimary
            )
        },
        trailing = {
            Icon(
                Icons.Rounded.KeyboardArrowLeft,
                contentDescription = "Consolidate the story",
                tint = MaterialTheme.colors.onPrimary
            )
        },
        text = {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Consolidate",
                    style = MaterialTheme.typography.button,
                    modifier = Modifier,
                    color = MaterialTheme.colors.onPrimary
                )
            }
        }
    )
}

@Preview
@Composable
fun DocumentBottomSheetViewPreview() {
    val doc =
        GroupedDocument("Story B", listOf(FileName.RangeChapter(1, 2), FileName.OneChapter(4)))
    ReSyncTheme {
        Column {
            DocumentBottomSheetView(doc)
        }
    }
}


// TODO Update to support multi account
// Either by filtering docs based on the active account or by adding
// some metadata on the items to indicate where they are coming from.
class ConsolidateViewModel(application: Application) : AndroidViewModel(application) {

    private val dao: DocumentsDao
    private val rmCloud: RmClient?
    private val isRefreshing = MutableStateFlow(false)
    private val isInitialized = mutableStateOf(ViewState.NotInitialized)

    val initialized: State<ViewState>
        get() = isInitialized
    val refreshing: StateFlow<Boolean>
        get() = isRefreshing.asStateFlow()

    val documents: Flow<List<GroupedDocument>>

    init {
        val db = RemarkableDatabase.getInstance(application)

        val tokens = PreferencesManager.create(application).readCurrentAccount().tokens
        rmCloud = tokens?.let { RmClient(it) }
        dao = db.documentsDao()


        // TODO Load the initialized state from preferences
        //  (a parent have been set, null if root have been selected)

        // TODO Manage with parent
        documents = dao.getAll().map { group(it) }
    }

    fun refreshDocuments() {
        viewModelScope.launch {
            if (rmCloud != null) {
                isRefreshing.emit(true)
                syncRemoteFiles(rmCloud, dao)
                isRefreshing.emit(false)
            } else {
                // Show a banner notification saying there aren't any
                // remarkable account set up. Somehow.
            }
        }
    }

    fun consolidate(story: String) {
        viewModelScope.launch {

        }
    }

    companion object {
        fun group(documents: List<Document>): List<GroupedDocument> {
            return documents.asSequence()
                .map { FileName.parse(it.name) }
                .map {
                    if (it == null) null
                    else {
                        // TODO Is that filter something we even want to keep ?
                        when (it.second) {
                            is FileName.NoChapter ->
                                null
                            is FileName.OneChapter ->
                                it
                            is FileName.RangeChapter ->
                                it
                        }
                    }
                }
                .filterNotNull()
                .groupBy(keySelector = { it.first }, valueTransform = { it.second })
                .map {
                    GroupedDocument(it.key, it.value)
                }
                .filter { it.chapters.size > 1 }
                .toList()
        }

        suspend fun syncRemoteFiles(client: RmClient, dao: DocumentsDao) {
            val docs = client.listDocuments().map { Document.fromApi(it) }

            for (doc in docs) {
                dao.upsert(doc)
            }
        }
    }
}

enum class ViewState {
    NoAccount,
    NotInitialized,
    Ok
}

data class GroupedDocument(val title: String, val chapters: List<FileName.Chapters>)

@Preview(
    showBackground = true,
    device = Devices.PIXEL_3,
    showSystemUi = true,
)
@Composable
fun ConsolidateViewUninitializedPreview() {
    ReSyncTheme {
        ConsolidateView(ViewState.NotInitialized, false, emptyList())
    }
}

@Preview(
    showBackground = true,
    device = Devices.PIXEL_3,
    showSystemUi = true,
)
@Composable
fun ConsolidateViewNoAccountPreview() {
    ReSyncTheme {
        ConsolidateView(ViewState.NoAccount, false, emptyList())
    }
}

@Preview(
    showBackground = true,
    device = Devices.PIXEL_3,
    showSystemUi = true,
)
@Composable
fun ConsolidateViewInitializedNoDocsPreview() {
    ReSyncTheme {
        ConsolidateView(ViewState.Ok, false, emptyList())
    }
}

@Preview(
    showBackground = true,
    device = Devices.PIXEL_3,
    showSystemUi = true,
)
@Composable
fun ConsolidateViewInitializedDocsPreview() {
    val docs = listOf(
        GroupedDocument("Story A", listOf(FileName.OneChapter(1))),
        GroupedDocument("Story B", listOf(FileName.RangeChapter(1, 2), FileName.OneChapter(4))),
        GroupedDocument("Story C", listOf(FileName.RangeChapter(2, 3)))
    )
    ReSyncTheme {
        ConsolidateView(ViewState.Ok, false, docs)
    }
}