package eu.monniot.resync

import eu.monniot.resync.downloader.Chapter
import eu.monniot.resync.downloader.ChapterId
import eu.monniot.resync.downloader.StoryId
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

    @Test
    fun parse_dashedTitle_wholeStory() {
        val actual = FileName.parse("My - Dashed - Story.epub")
        val expected = "My - Dashed - Story" to FileName.NoChapter

        assertEquals(expected, actual)
    }

    @Test
    fun parse_dashedTitle_singleDash_oneChapter() {
        val actual = FileName.parse("Some Story - The Sequel - Ch 3.epub")
        val expected = "Some Story - The Sequel" to FileName.OneChapter(3)

        assertEquals(expected, actual)
    }

    @Test
    fun parse_dashedTitle_multipleDashes_range() {
        val actual = FileName.parse("A - B - C - Ch 1-2.epub")
        val expected = "A - B - C" to FileName.RangeChapter(1, 2)

        assertEquals(expected, actual)
    }

    @Test
    fun parse_dashAdjacentToTitle_oneChapter() {
        // A dash that is part of the title itself (no surrounding spaces), right
        // before the real " - Ch " separator.
        val actual = FileName.parse("Story-With-Dashes - Ch 5.epub")
        val expected = "Story-With-Dashes" to FileName.OneChapter(5)

        assertEquals(expected, actual)
    }

    @Test
    fun parse_spacedRange_isTolerated() {
        // Some existing documents were written with spaces around the range dash
        // (e.g. "Ch 2 - 3" instead of "Ch 2-3"); both forms should parse the same.
        val actual = FileName.parse("My Life - Ch 2 - 3.epub")
        val expected = "My Life" to FileName.RangeChapter(2, 3)

        assertEquals(expected, actual)
    }

    @Test
    fun make_oneShot_usesBareName() {
        // A genuine one-shot (a single chapter that is the whole story) should not
        // be named as if it were a partial "Ch 1" download.
        val actual = FileName.make(listOf(chapter("My Story", 1)), wholeStory = true)
        val expected = "My Story.epub"

        assertEquals(expected, actual)
    }

    @Test
    fun make_singleChapterOfMultiChapterStory_usesChapterSuffix() {
        val actual = FileName.make(listOf(chapter("My Story", 3)), wholeStory = false)
        val expected = "My Story - Ch 3.epub"

        assertEquals(expected, actual)
    }

    @Test
    fun roundTrip_dashedTitle_oneShot() {
        val chapters = listOf(chapter("Some Story - The Sequel", 1))
        val fileName = FileName.make(chapters, wholeStory = true)

        val actual = FileName.parse(fileName)
        val expected = "Some Story - The Sequel" to FileName.NoChapter

        assertEquals(expected, actual)
    }

    @Test
    fun roundTrip_dashedTitle_oneChapter() {
        val chapters = listOf(chapter("Some Story - The Sequel", 3))
        val fileName = FileName.make(chapters, wholeStory = false)

        val actual = FileName.parse(fileName)
        val expected = "Some Story - The Sequel" to FileName.OneChapter(3)

        assertEquals(expected, actual)
    }

    @Test
    fun roundTrip_dashedTitle_range() {
        val chapters = listOf(chapter("A - B - C", 1), chapter("A - B - C", 2))
        val fileName = FileName.make(chapters, wholeStory = false)

        val actual = FileName.parse(fileName)
        val expected = "A - B - C" to FileName.RangeChapter(1, 2)

        assertEquals(expected, actual)
    }

    @Test
    fun roundTrip_dashedTitle_wholeMultiChapterStory() {
        val chapters = listOf(chapter("A - B - C", 1), chapter("A - B - C", 2))
        val fileName = FileName.make(chapters, wholeStory = true)

        val actual = FileName.parse(fileName)
        val expected = "A - B - C" to FileName.NoChapter

        assertEquals(expected, actual)
    }

    @Test
    fun formatChapters_noChapter_isBlank() {
        assertEquals("", FileName.formatChapters(FileName.NoChapter))
        assertEquals("", FileName.formatChapters(FileName.NoChapter, withPrefix = true))
    }

    @Test
    fun formatChapters_oneChapter_withoutPrefix() {
        assertEquals("3", FileName.formatChapters(FileName.OneChapter(3)))
    }

    @Test
    fun formatChapters_oneChapter_withPrefix() {
        assertEquals("Ch 3", FileName.formatChapters(FileName.OneChapter(3), withPrefix = true))
    }

    @Test
    fun formatChapters_rangeChapter_withoutPrefix() {
        assertEquals("2-4", FileName.formatChapters(FileName.RangeChapter(2, 4)))
    }

    @Test
    fun formatChapters_rangeChapter_withPrefix() {
        assertEquals("Ch 2-4", FileName.formatChapters(FileName.RangeChapter(2, 4), withPrefix = true))
    }

    private fun chapter(storyName: String, num: Int): Chapter =
        Chapter(
            storyId = StoryId(1),
            chapterId = ChapterId(num),
            chapterIndex = emptyMap(),
            num = num,
            chapterName = null,
            storyName = storyName,
            author = "Author",
            totalChapters = num,
            content = ""
        )

}
