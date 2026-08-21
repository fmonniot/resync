package eu.monniot.resync.downloader

enum class DriverType {
    ArchiveOfOurOwn,
    FanFictionNet;

    fun websiteName() = when(this) {
        ArchiveOfOurOwn -> "Archive of our Own"
        FanFictionNet -> "FanFiction.Net"
    }

    /**
     * Short label used in the Search screen's segmented button row, where
     * [SingleChoiceSegmentedButtonRow] weights both segments equally and the full
     * [websiteName] doesn't fit at `labelLarge`. Not used elsewhere -- [websiteName] remains
     * the user-facing name on the Confirm screen and in DownloadScreen's error copy.
     */
    fun shortName() = when(this) {
        ArchiveOfOurOwn -> "AO3"
        FanFictionNet -> "FF.Net"
    }
}
