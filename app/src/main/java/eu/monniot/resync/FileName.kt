package eu.monniot.resync

import eu.monniot.resync.downloader.Chapter


object FileName {

    fun make(chapters: List<Chapter>, wholeStory: Boolean): String =
        if (wholeStory) {
            // The whole story is present in this file, whether it took one chapter
            // (a one-shot) or several to do so: either way there is nothing partial
            // about it, so it gets the bare name.
            "${chapters[0].storyName}.epub"
        } else if (chapters.size > 1) {
            "${chapters[0].storyName} - Ch ${chapters.first().num}-${chapters.last().num}.epub"
        } else {
            "${chapters[0].storyName} - Ch ${chapters[0].num}.epub"
        }

    // Mirrors `make` above: a bare "<Story>.epub", or "<Story> - Ch <n>.epub" /
    // "<Story> - Ch <from>-<to>.epub" for a partial download. The story name is
    // matched non-greedily so a dash in the title itself doesn't get mistaken for
    // the " - Ch " separator; the chapter suffix is anchored to the very end of the
    // string (right before ".epub") so only a real trailing chapter marker matches.
    // The inner "from-to" dash tolerates surrounding whitespace to also accept
    // documents written with a spaced-out range (e.g. "Ch 2 - 3").
    private val NAME_REGEX =
        Regex("""^(?<name>.*?)(?: - Ch (?<from>\d+)(?: *- *(?<to>\d+))?)?\.epub$""")

    /**
     * @param name The file name to parse
     * @return A triple containing the story name, and a start/end chapters (when set).
     *         null when invalid chapter name
     */
    fun parse(name: String): Pair<String, Chapters>? {
        val match = NAME_REGEX.matchEntire(name) ?: return null
        val groups = match.groups as MatchNamedGroupCollection

        val storyName = groups["name"]?.value ?: return null
        val from = groups["from"]?.value?.toIntOrNull()
        val to = groups["to"]?.value?.toIntOrNull()

        val chapters = when {
            from == null -> NoChapter
            to == null -> OneChapter(from)
            else -> RangeChapter(from, to)
        }

        return Pair(storyName, chapters)
    }

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
