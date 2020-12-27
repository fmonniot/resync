package eu.monniot.resync

import eu.monniot.resync.ui.Chapter
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

    // When generating a multi chapter, add the chapter name before the start of the chapter
    val insertChapterName = chapterList.size > 1

    val chapters = chapterList.sortedBy { it.num }.iterator()

    // First chapter is important: it defines the metadata for the entire story
    val firstChapter = chapters.next()

    // Story metadata
    book.metadata.addAuthor(Author(firstChapter.author))
    book.metadata.addTitle(firstChapter.storyName)

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
    val title = if (insertTitle) {
        "<h2>${chapter.chapterName}</h2><hr style=\"width:100%;margin: 0 10% 0 10%;\"></hr>"
    } else ""

    val content = """<?xml version="1.0" encoding="UTF-8"?>
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops">
<body>
$title
${chapter.content.replace("xmlns=\"http://www.w3.org/1999/xhtml\"", "")}
</body>
</html>"""

    val resource = Resource(content.toByteArray(), "chapter_${chapter.num}.xhtml")

    this.addSection(chapter.chapterName ?: "", resource);
}
