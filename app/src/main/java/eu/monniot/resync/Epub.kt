package eu.monniot.resync

import android.text.TextUtils
import eu.monniot.resync.ui.Chapter
import eu.monniot.resync.ui.ChapterNum
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nl.siegmann.epublib.domain.Author
import nl.siegmann.epublib.domain.Book
import nl.siegmann.epublib.domain.Resource
import nl.siegmann.epublib.epub.EpubWriter
import java.io.ByteArrayOutputStream


suspend fun makeEpub(chapterList: List<Chapter>): ByteArray {

    // Create new Book
    val book = Book()

    // When generating a multi chapter story, add the chapter name before the start of the chapter
    val insertChapterName = chapterList.size > 1

    val sortedChapters = chapterList.sortedBy { it.num }
    val chapters = sortedChapters.iterator()

    // First chapter is important: it defines the metadata for the entire story
    val firstChapter = chapters.next()

    // Story metadata
    book.metadata.addAuthor(Author(firstChapter.author))
    book.metadata.addTitle(firstChapter.storyName)
    book.addCover(
        firstChapter,
        sortedChapters.first().num to sortedChapters.last().num
    )

    // Insert the first chapter
    book.addChapter(firstChapter, insertChapterName)

    // And then all the others
    for (c in chapters) {
        book.addChapter(c, insertChapterName)
    }

    // Finally generate the epub file itself
    val epubWriter = EpubWriter()
    val bytes = ByteArrayOutputStream()

    withContext(Dispatchers.IO) {
        @Suppress("BlockingMethodInNonBlockingContext")
        epubWriter.write(book, bytes)
    }

    return bytes.toByteArray()
}

fun Book.addChapter(chapter: Chapter, insertTitle: Boolean) {
    val chapterName = chapter.chapterName?.let { TextUtils.htmlEncode(it) } ?: ""
    val title = if (insertTitle) {
        "<h2>${chapterName}</h2><hr style=\"width:100%;margin: 0 10% 0 10%;\"></hr>"
    } else ""

    val content = """<?xml version="1.0" encoding="UTF-8"?>
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops">
<body>
$title
${chapter.content.replace("xmlns=\"http://www.w3.org/1999/xhtml\"", "")}
</body>
</html>"""

    val resource = Resource(content.toByteArray(), "chapter_${chapter.num}.xhtml")

    this.addSection(chapterName ?: "", resource)
}

// Note that remarkable will ignore the layout when internally converting from epub to pdf.
// Emitting as a PDF would make us loose the typo/size choice on the tablet.
// So we keep the layout for now and hope that the reader will be able to decode those in the future
// (even though I highly doubt that will be the case).
fun Book.addCover(chapter: Chapter, range: Pair<ChapterNum, ChapterNum>) {
    val (first, last) = range

    val subTitle = when {
        first == last && chapter.chapterName != null -> chapter.chapterName
        first == last -> ""
        else -> "Chapters $first to $last"
    }

    val content = """<?xml version="1.0" encoding="UTF-8"?>
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops">
<body style="margin: 10% 15%;padding:0;height: 80%;width: 70%;">
<h1 style="margin-top: 25%;width:100%;text-align: center;">${chapter.storyName}</h1>
<h2 style="margin-top: 5%;text-align: center;">$subTitle</h2>
<hr style="margin:0 10% 0 10%"></hr>
<h2 style="text-align: center;">By ${chapter.author}</h2>
</body>
</html>"""

    this.coverPage = Resource(content.toByteArray(), "cover.xhtml")
}
