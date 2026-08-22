package eu.monniot.resync.ui.launcher

import eu.monniot.resync.FileName
import eu.monniot.resync.database.Document
import eu.monniot.resync.database.DocumentsDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

// Robolectric-backed: a bare `Application()` only satisfies the AndroidViewModel constructor
// signature, it doesn't provide a working Application — any real method call on it throws
// "Method ... not mocked" per CLAUDE.md's testing notes. RuntimeEnvironment.getApplication()
// gives us a real (shadowed) Application instance instead. The pure `group()` tests below don't
// touch Application at all, so this doesn't change their behavior.
@RunWith(RobolectricTestRunner::class)
class ConsolidateViewModelTest {

    @Test
    fun group_basicIsCorrect() {
        val docs = listOf(
            doc("Hello there.epub"),
            doc("My Life - Ch 1.epub"),
            doc("My Life - Ch 2 - 3.epub"),
            doc("My Life - Ch 4.epub"),
            doc("My Life - Ch 5.epub"),
            doc("My Life - Ch 6 - 7.epub"),
        )

        val expected = listOf(
            GroupedDocument(
                "My Life", listOf(
                    FileName.OneChapter(1),
                    FileName.RangeChapter(2, 3),
                    FileName.OneChapter(4),
                    FileName.OneChapter(5),
                    FileName.RangeChapter(6, 7),
                )
            )
        )

        Assert.assertEquals(expected, ConsolidateViewModel.group(docs))
    }

    @Test
    fun group_dashedTitleIsGroupedTogether() {
        // A story title containing a dash used to shift the split-based parser's
        // fields, causing FileName.parse to return null and the document to be
        // silently dropped from the grouping. It should now group like any other
        // story.
        val docs = listOf(
            doc("Some Story - The Sequel - Ch 1.epub"),
            doc("Some Story - The Sequel - Ch 2-3.epub"),
            doc("Some Story - The Sequel - Ch 4.epub"),
        )

        val expected = listOf(
            GroupedDocument(
                "Some Story - The Sequel", listOf(
                    FileName.OneChapter(1),
                    FileName.RangeChapter(2, 3),
                    FileName.OneChapter(4),
                )
            )
        )

        Assert.assertEquals(expected, ConsolidateViewModel.group(docs))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun documents_reflectsGroupOfWhateverTheDaoEmits() = runTest {
        val docs = listOf(
            doc("My Life - Ch 1.epub"),
            doc("My Life - Ch 2 - 3.epub"),
        )
        val dao = FakeDocumentsDao(docs)

        val model = ConsolidateViewModel(RuntimeEnvironment.getApplication(), dao)

        val expected = ConsolidateViewModel.group(docs)
        Assert.assertEquals(expected, model.documents.first())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun refreshDocuments_togglesRefreshingWhileRunningThenSettlesBack() = runTest {
        val model = ConsolidateViewModel(RuntimeEnvironment.getApplication(), FakeDocumentsDao(emptyList()))

        // refreshDocuments() suspends between flipping the flag on and off, so a collector
        // running concurrently should observe both the `true` and the final `false`. Asserting
        // only the value after the call returns would pass even if refreshDocuments() did
        // nothing at all, since the writes are synchronous around the suspension point.
        val emissions = mutableListOf<Boolean>()
        val collector = launch { model.refreshing.collect { emissions.add(it) } }
        runCurrent()

        model.refreshDocuments()
        runCurrent()

        collector.cancel()
        Assert.assertEquals(listOf(false, true, false), emissions)
    }

    private fun doc(name: String): Document =
        Document(id = "", version = 1, type = "", name, false, null)

    // In-memory fake, no Room involved: the seam ConsolidateViewModel exposes for
    // testing only needs a DocumentsDao, not a real database.
    private class FakeDocumentsDao(initial: List<Document>) : DocumentsDao {
        private val documents = MutableStateFlow(initial)

        override fun getAll(): Flow<List<Document>> = documents

        override fun getAllWithParent(parent: String): Flow<List<Document>> =
            throw UnsupportedOperationException("not used by ConsolidateViewModel")

        override suspend fun upsert(item: Document) {
            documents.value = documents.value.filterNot { it.id == item.id } + item
        }

        override suspend fun delete(item: Document) {
            documents.value = documents.value.filterNot { it.id == item.id }
        }
    }

}
