package eu.monniot.resync.ui.downloader

@JvmInline
value class StoryId(val id: Int)

@JvmInline
value class ChapterId(val id: Int)

data class Chapter(
    val storyId: StoryId,
    val chapterId: ChapterId,
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
    data class One(val num: Int) : ChapterSelection()
    data class Range(val start: Int, val end: Int) : ChapterSelection()

    fun firstChapter(): Int = when (this) {
        All -> 1
        is One -> num
        is Range -> start
    }

    fun lastChapter(totalChapters: Int? = null): Int? = when (this) {
        All -> if (totalChapters == null) null else {
            if (totalChapters > 1) totalChapters else null
        }
        is One -> null
        is Range -> if (end > firstChapter()) end else null
    }
}

