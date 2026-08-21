package eu.monniot.resync.downloader

import org.junit.Assert.assertEquals
import org.junit.Test

class DriverTypeTest {

    @Test
    fun websiteName_archiveOfOurOwn() {
        assertEquals("Archive of our Own", DriverType.ArchiveOfOurOwn.websiteName())
    }

    @Test
    fun websiteName_fanFictionNet() {
        assertEquals("FanFiction.Net", DriverType.FanFictionNet.websiteName())
    }

    @Test
    fun shortName_archiveOfOurOwn() {
        assertEquals("AO3", DriverType.ArchiveOfOurOwn.shortName())
    }

    @Test
    fun shortName_fanFictionNet() {
        assertEquals("FF.Net", DriverType.FanFictionNet.shortName())
    }
}
