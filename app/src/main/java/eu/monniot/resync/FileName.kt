package eu.monniot.resync

import eu.monniot.resync.ui.Chapter

object FileName {

    fun make(chapters: List<Chapter>, wholeStory: Boolean): String =
        if (chapters.size > 1) {
            if (wholeStory) {
                "${chapters[0].storyName}.epub"
            } else {
                "${chapters[0].storyName} - Ch ${chapters.first().num}-${chapters.last().num}.epub"
            }
        } else {
            "${chapters[0].storyName} - Ch ${chapters[0].num}.epub"
        }

    /**
     * @param name The file name to parse
     * @return A triple containing the story name, and a start/end chapters (when set).
     *         null when invalid chapter name
     */
    fun parse(name: String): Pair<String, Chapters>? {
        val s = name.split("-")

        val storyName by lazy { s[0].trim() }
        val firstChapter by lazy { s[1].trim().removePrefix("Ch ") }
        val lastChapter by lazy { s[2].trim() }

        return when (s.size) {
            1 -> Pair(dropExt(storyName), NoChapter)
            2 -> dropExt(firstChapter).toIntOrNull()?.let {
                Pair(storyName, OneChapter(it))
            }
            3 -> firstChapter.toIntOrNull()?.let { first ->
                dropExt(lastChapter).toIntOrNull()?.let { last ->
                    Pair(storyName, RangeChapter(first, last))
                }
            }
            else -> null
        }
    }

    private fun dropExt(s: String) = s.removeSuffix(".epub")

    sealed interface Chapters
    object NoChapter : Chapters
    data class OneChapter(val chapter: Int) : Chapters
    data class RangeChapter(val from: Int, val to: Int) : Chapters

    // TODO Test with the actual app if the prefix version actually make sense
    fun formatChapters(ch: Chapters, withPrefix: Boolean = false): String =
        when (ch) {
            NoChapter -> ""
            is OneChapter ->
                if (withPrefix) "Ch ${ch.chapter}"
                else ch.chapter.toString()
            is RangeChapter ->
                if (withPrefix) "Ch ${ch.from}-${ch.to}"
                else "${ch.from}-${ch.to}"
        }
}
