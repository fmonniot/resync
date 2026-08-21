package eu.monniot.resync

import eu.monniot.resync.downloader.Chapter
import eu.monniot.resync.downloader.ChapterId
import eu.monniot.resync.downloader.StoryId
import kotlinx.coroutines.test.runTest
import nl.siegmann.epublib.domain.Book
import nl.siegmann.epublib.epub.EpubReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

class EpubTest {

    // chapterName is deliberately left null throughout: Book.addChapter only calls
    // android.text.TextUtils.htmlEncode when it's non-null, and that Android API isn't
    // available on the plain JVM these unit tests run on (no Robolectric here).
    private fun chapter(
        num: Int,
        content: String,
        totalChapters: Int = num,
        storyName: String = "The Story",
        author: String = "The Author",
    ): Chapter = Chapter(
        storyId = StoryId(1),
        chapterId = ChapterId(num),
        chapterIndex = emptyMap(),
        num = num,
        chapterName = null,
        storyName = storyName,
        author = author,
        totalChapters = totalChapters,
        content = content,
    )

    private fun readBack(epub: ByteArray): Book =
        EpubReader().readEpub(ByteArrayInputStream(epub))

    private fun resourceText(book: Book, index: Int): String =
        String(book.spine.getResource(index).data, StandardCharsets.UTF_8)

    @Test
    fun addCover_singleChapterWithName_usesChapterNameAsSubtitle() {
        val book = Book()
        val chapter = chapter(1, "content").copy(chapterName = "The Chapter")

        book.addCover(chapter, firstChapterNumber = 1, lastChapterNumber = 1)

        val cover = String(book.coverPage.data, StandardCharsets.UTF_8)
        assertTrue(cover.contains("<h1 style=\"margin-top: 25%;width:100%;text-align: center;\">The Story</h1>"))
        assertTrue(cover.contains("<h2 style=\"margin-top: 5%;text-align: center;\">The Chapter</h2>"))
    }

    @Test
    fun addCover_singleChapterWithoutName_hasNoSubtitle() {
        val book = Book()
        val chapter = chapter(1, "content")

        book.addCover(chapter, firstChapterNumber = 1, lastChapterNumber = 1)

        val cover = String(book.coverPage.data, StandardCharsets.UTF_8)
        assertTrue(cover.contains("<h2 style=\"margin-top: 5%;text-align: center;\"></h2>"))
    }

    @Test
    fun addCover_multipleChapters_showsChapterRange() {
        val book = Book()
        val chapter = chapter(3, "content")

        book.addCover(chapter, firstChapterNumber = 1, lastChapterNumber = 7)

        val cover = String(book.coverPage.data, StandardCharsets.UTF_8)
        assertTrue(cover.contains("Chapters 1 to 7"))
    }

    @Test
    fun makeEpub_singleChapter_roundTripsTitleAuthorAndSanitisedContent() = runTest {
        val chapter = chapter(1, "a&nbsp;b<br>", totalChapters = 1)

        val bytes = makeEpub(listOf(chapter))
        val book = readBack(bytes)

        assertEquals("The Story", book.title)
        assertEquals("The Author", book.metadata.authors.single().let { "${it.firstname} ${it.lastname}" }.trim())
        // Index 0 in the spine is the generated cover page (see Book.addCover); the chapter
        // itself follows it.
        assertEquals(2, book.spine.size())
        assertTrue(resourceText(book, 1).contains("ab<br/>"))
    }

    @Test
    fun makeEpub_singleChapter_doesNotInsertAChapterTitleHeading() = runTest {
        val bytes = makeEpub(listOf(chapter(1, "body", totalChapters = 1)))
        val book = readBack(bytes)

        assertFalse(
            "a single-chapter epub shouldn't have a chapter-title <h2> inside the chapter body",
            resourceText(book, 1).contains("<h2>")
        )
    }

    @Test
    fun makeEpub_multipleChapters_ordersBySpineByChapterNumberRegardlessOfInputOrder() = runTest {
        val chapters = listOf(
            chapter(3, "third", totalChapters = 3),
            chapter(1, "first", totalChapters = 3),
            chapter(2, "second", totalChapters = 3),
        )

        val bytes = makeEpub(chapters)
        val book = readBack(bytes)

        // Index 0 in the spine is the generated cover page (see Book.addCover); the chapters
        // follow it, in num order rather than the input order above.
        assertEquals(4, book.spine.size())
        assertTrue(resourceText(book, 1).contains("first"))
        assertTrue(resourceText(book, 2).contains("second"))
        assertTrue(resourceText(book, 3).contains("third"))
    }

    @Test
    fun makeEpub_multipleChapters_insertsAChapterTitleHeading() = runTest {
        val chapters = listOf(
            chapter(1, "first", totalChapters = 2),
            chapter(2, "second", totalChapters = 2),
        )

        val bytes = makeEpub(chapters)
        val book = readBack(bytes)

        assertTrue(
            "a multi-chapter epub should insert a chapter-title heading before each chapter's body",
            resourceText(book, 1).contains("<h2>")
        )
    }

    @Test
    fun sanitiseContent_hrWithAttributes_keepsSurroundingText() {
        val input = """<hr size="1" noshade> she turned away <em>slowly</em>"""
        val actual = sanitiseContent(input)

        assertEquals("<hr/> she turned away <em>slowly</em>", actual)
    }

    @Test
    fun sanitiseContent_bareHr_isNormalised() {
        assertEquals("<hr/>", sanitiseContent("<hr>"))
    }

    @Test
    fun sanitiseContent_hrWithAttributes_isNormalised() {
        assertEquals("<hr/>", sanitiseContent("""<hr size="1" noshade>"""))
    }

    @Test
    fun sanitiseContent_uppercaseHr_isNormalised() {
        assertEquals("<hr/>", sanitiseContent("<HR>"))
        assertEquals("<hr/>", sanitiseContent("""<HR SIZE="1" NOSHADE>"""))
    }

    @Test
    fun sanitiseContent_bareBr_isNormalised() {
        assertEquals("<br/>", sanitiseContent("<br>"))
    }

    @Test
    fun sanitiseContent_selfClosingBr_isNormalised() {
        assertEquals("<br/>", sanitiseContent("<br/>"))
        assertEquals("<br/>", sanitiseContent("<br />"))
    }

    @Test
    fun sanitiseContent_uppercaseBr_isNormalised() {
        assertEquals("<br/>", sanitiseContent("<BR>"))
    }

    @Test
    fun sanitiseContent_multipleTagsOnOneLine_areAllNormalised() {
        val input = "before<br>middle<hr size=\"1\">after<BR/>end<HR noshade>tail"
        val expected = "before<br/>middle<hr/>after<br/>end<hr/>tail"

        assertEquals(expected, sanitiseContent(input))
    }

    @Test
    fun sanitiseContent_stripsNbspAndXhtmlNamespace() {
        val input = "<p xmlns=\"http://www.w3.org/1999/xhtml\">a&nbsp;b</p>"
        val expected = "<p >ab</p>"

        assertEquals(expected, sanitiseContent(input))
    }
}
