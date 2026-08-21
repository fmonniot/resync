package eu.monniot.resync.ui.launcher

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
// M3 ListItem/MaterialTheme are aliased to avoid clashing with the M2 `androidx.compose.material.*`
// wildcard import above, which is still needed for ModalBottomSheetLayout/PullRefreshIndicator
// (M2 usage removed in redesign-09) and for the M2 `ListItem` used by `DocumentBottomSheetView`.
import androidx.compose.material3.Icon as M3Icon
import androidx.compose.material3.ListItem as M3ListItem
import androidx.compose.material3.MaterialTheme as M3MaterialTheme
import androidx.compose.material3.Text as M3Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.AndroidViewModel
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
            ConsolidateEmptyState(
                // TODO(redesign-11): swap for Icons.Rounded.CloudOff once the Material Symbols
                // icon set lands (docs/tickets/redesign-11-material-symbols-icons.md) -
                // material-icons-core has no "cloud_off" glyph.
                icon = null,
                title = "No reMarkable account set",
                subtitle = "Use the Share sheet after downloading a story to send it to reMarkable for now.",
            )

        ViewState.NotInitialized ->
            // TODO Is this feature still something I need ?
            Column(modifier = Modifier.fillMaxSize()) {
                M3Text(
                    text = "TODO: Select a folder",
                    style = M3MaterialTheme.typography.bodyLarge,
                    color = M3MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
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

                val pullRefreshState = rememberPullRefreshState(refreshing, onRefresh)

                Box(Modifier.pullRefresh(pullRefreshState)) {
                    LazyColumn(Modifier.fillMaxWidth()) {
                        if (documents.isEmpty()) {
                            item {
                                ConsolidateEmptyState(
                                    // TODO(redesign-11): swap for Icons.Rounded.Inbox once the
                                    // Material Symbols icon set lands
                                    // (docs/tickets/redesign-11-material-symbols-icons.md) -
                                    // material-icons-core has no "inbox" glyph.
                                    icon = null,
                                    title = "No documents yet",
                                    subtitle = "Pull down to refresh",
                                    modifier = Modifier.fillParentMaxSize(),
                                )
                            }
                        } else {

                            // TODO Sort and group documents alphabetically
                            items(documents) { doc ->
                                M3ListItem(
                                    headlineContent = { M3Text(doc.title) },
                                    supportingContent = {
                                        // TODO Join continuous chapters (eg. 1, 2, 3 as 1-3, or 1,2,3,5 as 1-3,5)
                                        // See also GroupedDocument data class
                                        val text =
                                            doc.chapters.joinToString { FileName.formatChapters(it) }
                                        M3Text(text)
                                    },
                                    // TODO(redesign-11): add a leading Icons.Rounded.Description
                                    // once the Material Symbols icon set lands
                                    // (docs/tickets/redesign-11-material-symbols-icons.md) -
                                    // material-icons-core has no "description" glyph.
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

                    PullRefreshIndicator(refreshing, pullRefreshState, Modifier.align(Alignment.TopCenter))
                }
            }
        }
    }
}

@Composable
private fun ConsolidateEmptyState(
    icon: ImageVector?,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (icon != null) {
            M3Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = M3MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        M3Text(
            text = title,
            style = M3MaterialTheme.typography.titleMedium,
            color = M3MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(4.dp))

        M3Text(
            text = subtitle,
            style = M3MaterialTheme.typography.bodyMedium,
            color = M3MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

// The ColumnScope received isn't used, but I do want it to have the
// compiler remind me that this function needs to be within a vertical
// alignment (i.e. a Column).
@Suppress("UnusedReceiverParameter")
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ColumnScope.DocumentBottomSheetView(document: GroupedDocument) {

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
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = "Consolidate the story",
                tint = MaterialTheme.colors.onPrimary
            )
        },
        trailing = {
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
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
    private val isRefreshing = MutableStateFlow(false)
    // The reMarkable Cloud integration was removed (see CLAUDE.md), so there is genuinely no
    // account to be found on startup. `NotInitialized` (folder selection) has no code path that
    // reaches it today; it's kept as an anchor point for a future cloud reimplementation.
    private val isInitialized = mutableStateOf(ViewState.NoAccount)

    val initialized: State<ViewState>
        get() = isInitialized
    val refreshing: StateFlow<Boolean>
        get() = isRefreshing.asStateFlow()

    val documents: Flow<List<GroupedDocument>>

    init {
        val db = RemarkableDatabase.getInstance(application)

        dao = db.documentsDao()

        // TODO Load the initialized state from preferences
        //  (a parent have been set, null if root have been selected)

        // TODO Manage with parent
        documents = dao.getAll().map { group(it) }
    }

    // TODO Re-entry point for a future cloud sync: pull the remote document list and
    // upsert it into `dao`, the way `RmClient.listDocuments()` used to before the direct
    // reMarkable Cloud integration was removed.
    fun refreshDocuments() {
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
    name = "No Account (Light)",
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
    name = "No Account (Dark)",
)
@Composable
fun ConsolidateViewNoAccountDarkPreview() {
    ReSyncTheme(darkTheme = true) {
        ConsolidateView(ViewState.NoAccount, false, emptyList())
    }
}

@Preview(
    showBackground = true,
    device = Devices.PIXEL_3,
    showSystemUi = true,
    name = "No Documents (Light)",
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
    name = "No Documents (Dark)",
)
@Composable
fun ConsolidateViewInitializedNoDocsDarkPreview() {
    ReSyncTheme(darkTheme = true) {
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