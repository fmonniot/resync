package eu.monniot.resync.downloader

import org.jsoup.Jsoup

class ArchiveOfOurOwnDriver : Driver() {

    override fun makeUrl(storyId: StoryId, chapterId: ChapterId): String =
        if (chapterId.id == null) {
            "https://archiveofourown.org/works/${storyId.id}?view_adult=true"
        } else {
            "https://archiveofourown.org/works/${storyId.id}/chapters/${chapterId.id}?view_adult=true"
        }

    override fun parseWebPage(source: String, storyId: StoryId, chapterId: ChapterId): Chapter {
        @Suppress("UNUSED_VARIABLE") val document = Jsoup.parse(source)

        // knownChapters:
        // Array.apply([], document.querySelectorAll('#chapter_index option')).map((el, i) => [el.value, i])

        TODO("Not yet implemented")
    }

    private val scriptText =
        "javascript:window.grabber.%s(document.querySelector('%s').innerText);"

    private val scriptHtml =
        "javascript:window.grabber.%s(new XMLSerializer().serializeToString(document.querySelector('%s')));"

    private val scriptChapter =
        "javascript:window.grabber.%s(document.querySelector('.work.meta.group dd.chapters').innerText.split('/')[%s]);"

    val chapterTextScript: String
        get() = scriptHtml.format("onChapterText", "#chapters")
    val storyNameScript: String
        get() = scriptText.format("onStoryName", ".title.heading")
    val authorNameScript: String
        get() = scriptText.format("onAuthorName", ".byline.heading")
    val chapterNameScript: String
        get() = scriptText.format("onAuthorName", ".chapter.preface.group > .title")
    val totalChaptersScript: String
        get() = scriptChapter.format("onTotalChapters", "1")
    val chapterNumScript: String
        get() = scriptChapter.format("onChapterNumber", "0")
}
