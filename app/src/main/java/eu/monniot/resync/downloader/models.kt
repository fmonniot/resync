package eu.monniot.resync.downloader


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