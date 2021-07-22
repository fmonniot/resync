package eu.monniot.resync.ui.launcher

import eu.monniot.resync.database.Document
import org.junit.Assert
import org.junit.Test

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
            GroupedDocument("My Life", (1..7).toList())
        )

        Assert.assertEquals(expected, ConsolidateViewModel.group(docs))
    }


    private fun doc(name: String): Document =
        Document(id = "", version = 1, type = "", name, false, null)

}