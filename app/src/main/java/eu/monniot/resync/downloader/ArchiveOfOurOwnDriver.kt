package eu.monniot.resync.downloader

import eu.monniot.resync.ui.downloader.ChapterId
import eu.monniot.resync.ui.downloader.StoryId

class ArchiveOfOurOwnDriver : Driver() {
    // Note that for AO3, we need a chapter Id and not num in the url
    // TODO Introduce a ChapterId data class with both number and id in it
    // For FF.Net it's gonna be the same, for AO3 it's different
    override fun makeUrl(storyId: StoryId, chapterId: ChapterId): String =
        "https://archiveofourown.org/works/${storyId}/chapters/${chapterId}?view_adult=true"

    private val scriptText =
        "javascript:window.grabber.%s(document.querySelector('%s').innerText);"

    private val scriptHtml =
        "javascript:window.grabber.%s(new XMLSerializer().serializeToString(document.querySelector('%s')));"

    private val scriptChapter =
        "javascript:window.grabber.%s(document.querySelector('.work.meta.group dd.chapters').innerText.split('/')[%s]);"

    override val chapterTextScript: String
        get() = scriptHtml.format("onChapterText", "#chapters")
    override val storyNameScript: String
        get() = scriptText.format("onStoryName", ".title.heading")
    override val authorNameScript: String
        get() = scriptText.format("onAuthorName", ".byline.heading")
    override val chapterNameScript: String
        get() = scriptText.format("onAuthorName", ".chapter.preface.group > .title")
    override val totalChaptersScript: String
        get() = scriptChapter.format("onTotalChapters", "1")
    override val chapterNumScript: String
        get() = scriptChapter.format("onChapterNumber", "0")
}
