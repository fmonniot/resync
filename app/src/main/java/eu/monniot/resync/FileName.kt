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
    fun parse(name: String): Triple<String, Int?, Int?>? {
        val s = name.split("-")

        val storyName by lazy { s[0].trim() }
        val firstChapter by lazy { s[1].trim().removePrefix("Ch ") }
        val lastChapter by lazy { s[2].trim() }

        return when (s.size) {
            1 -> Triple(dropExt(storyName), null, null)
            2 -> Triple(storyName, dropExt(firstChapter).toIntOrNull(), null)
            3 -> Triple(storyName, firstChapter.toIntOrNull(), dropExt(lastChapter).toIntOrNull())
            else -> null
        }
    }

    private fun dropExt(s: String) = s.removeSuffix(".epub")

}
