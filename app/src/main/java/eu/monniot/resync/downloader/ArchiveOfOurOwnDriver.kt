package eu.monniot.resync.downloader

import eu.monniot.resync.ui.ChapterNum
import eu.monniot.resync.ui.StoryId

class ArchiveOfOurOwnDriver : Driver() {
    override fun makeUrl(storyId: StoryId, chapterNum: ChapterNum): String =
        TODO("Not yet implemented")

    override val chapterTextScript: String
        get() = TODO("Not yet implemented")
    override val storyNameScript: String
        get() = TODO("Not yet implemented")
    override val authorNameScript: String
        get() = TODO("Not yet implemented")
    override val chapterNameScript: String
        get() = TODO("Not yet implemented")
    override val totalChaptersScript: String
        get() = TODO("Not yet implemented")
}
