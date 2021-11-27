package eu.monniot.resync.ui.downloader

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

