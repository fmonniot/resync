package eu.monniot.resync.downloader

enum class DriverType {
    ArchiveOfOurOwn,
    FanFictionNet;

    fun websiteName() = when(this) {
        ArchiveOfOurOwn -> "Archive of our Own"
        FanFictionNet -> "FanFiction.Net"
    }
}
