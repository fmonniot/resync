package eu.monniot.resync

import org.junit.Assert.assertEquals
import org.junit.Test

class FileNameTest {


    @Test
    fun parse_wholeStory() {
        val actual = FileName.parse("My Story.epub")
        val expected = "My Story" to FileName.NoChapter

        assertEquals(expected, actual)
    }

    @Test
    fun parse_oneChapterStory() {
        val actual = FileName.parse("My Story - Ch 1.epub")
        val expected = "My Story" to FileName.OneChapter(1)

        assertEquals(expected, actual)
    }

    @Test
    fun parse_multiChapterStory() {
        val actual = FileName.parse("My Story - Ch 2-4.epub")
        val expected = "My Story" to FileName.RangeChapter(2, 4)

        assertEquals(expected, actual)
    }

    @Test
    fun parse_invalidFileName() {
        val actual = FileName.parse("My Story - 1 - 2 - 3.pdf")
        val expected = null

        assertEquals(expected, actual)
    }

}
