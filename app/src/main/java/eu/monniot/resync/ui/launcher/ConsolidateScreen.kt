package eu.monniot.resync.ui.launcher

import android.app.Application
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import eu.monniot.resync.database.Document
import eu.monniot.resync.database.DocumentsDao
import eu.monniot.resync.database.RemarkableDatabase
import eu.monniot.resync.ui.ReSyncTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import androidx.compose.material.ListItem
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// TODO Add a way to group together existing stories.
// As time pass, I found out that I have a lot of epub
// generated for the same story: generally one per chapter.
// It would be nice to offer a UI showing all fragmented
// stories and offer a way to merge them into one file.
// A Story Defragmenter of sort :grin:
@Composable
fun ConsolidateScreen() {
    // TODO Will it work with AndroidViewModel? Or will we have to provide a factory class?
    val model: ConsolidateViewModel = viewModel()

    val initialized by model.initialized
    val documents by model.documents.collectAsState(emptyList())

    ConsolidateView(initialized, documents)

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
fun ConsolidateView(initialized: Boolean, documents: List<Document>) {
    if(initialized) {
        LazyColumn(modifier = Modifier.padding(top = 16.dp, bottom = 56.dp)) {
            items(documents) { torrent ->
                ListItem(
                    text = { Text(torrent.name) },
                    secondaryText = {
                        // TODO Chapters available
                        Text("Ch 1, 2, 3-7")
                    }
                )
            }
        }
    } else {
        Text("TODO: Select a folder")
    }

}

// TODO It seems AndroidViewModel can be instantiated automatically. Let's see if subclasses can too.
class ConsolidateViewModel(application: Application): AndroidViewModel(application) {

    // TODO Store Cloud access classes

    private val dao: DocumentsDao

    // TODO Load the initialized state from preferences
    //  (a parent have been set, null if root have been selected)
    val initialized: State<Boolean> = mutableStateOf(false)

    // TODO Expose grouped documents and not the row doc themselves
    // We will probably also need a way to find back the story id
    val documents: Flow<List<Document>>

    init {
        val db = RemarkableDatabase.getInstance(application)

        dao = db.documentsDao()
        documents = dao.getAll() // TODO Manage with parent
    }

    fun refreshDocuments() {
        viewModelScope.launch {

        }
    }

    fun consolidate(story: String) {
        viewModelScope.launch {

        }
    }

}

/*
class TodoViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        if (modelClass.isAssignableFrom(TodoViewModel::class.java)) {
            return TodoViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
*/

@Preview
@Composable
fun ConsolidateViewUninitializedPreview() {
    ReSyncTheme {
        ConsolidateView(false, emptyList())
    }
}


@Preview
@Composable
fun ConsolidateViewInitializedNoDocsPreview() {
    ReSyncTheme {
        ConsolidateView(true, emptyList())
    }
}

@Preview
@Composable
fun ConsolidateViewInitializedDocsPreview() {
    val docs = listOf(
        Document("id", 1, "type", "name 1", true, "parent"),
        Document("id", 1, "type", "name 2", true, "parent"),
        Document("id", 1, "type", "name 3", true, "parent")
    )
    ReSyncTheme {
        ConsolidateView(true, docs)
    }
}