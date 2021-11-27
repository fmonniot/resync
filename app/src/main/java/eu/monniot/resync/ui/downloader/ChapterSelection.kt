package eu.monniot.resync.ui.downloader

@JvmInline
value class StoryId(val id: Int)

// The id is optional because, depending on the provider, it might
// not exists. For example, FF.Net will display the first chapter when
// not present in the url. On the other hand, AO3 does not have a chapter
// id at all for one-shots (and it redirects to the url with chapter id when
// there are more than one chapter).
@JvmInline
value class ChapterId(val id: Int?)


data class Chapter(
    val storyId: StoryId,
    val chapterId: ChapterId,
    /** An index of chapter number to its id */
    val chapterIndex: Map<Int, ChapterId>,
    val num: Int,
    val chapterName: String?,
    val storyName: String,
    val author: String,
    val totalChapters: Int,
    val content: String
)

/**
 * Represents what the chapters the user selected for download.
 *
 * Note that the selection is using chapters numbers −as seen by the
 * user− which can be different from the actual chapter id used by
 * the story provider (eg. ffnet uses numbers, ao3 uses ids).
 */
sealed class ChapterSelection {
    object All : ChapterSelection()
    data class One(val chapter: Int) : ChapterSelection()
    data class Range(val start: Int, val end: Int) : ChapterSelection() {
        fun contains(value: Int): Boolean = value in start..end
        fun size(): Int = end - start // TODO test this one, I'm bad at those
    }
}

