package eu.monniot.resync

import android.net.Uri
import eu.monniot.resync.downloader.ChapterId
import eu.monniot.resync.downloader.DriverType
import eu.monniot.resync.downloader.StoryId
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// Using Robolectric to have access to Uri in unit tests
@RunWith(RobolectricTestRunner::class)
class DeepLinkActivityTest {

    @Test
    fun parsePath_ffnet_withoutChapter() {
        Assert.assertEquals(
            Triple(StoryId(3384712), ChapterId(null), DriverType.FanFictionNet),
            DeepLinkActivity.parsePath(Uri.parse("https://m.fanfiction.net/s/3384712/"))
        )
    }

    @Test
    fun parsePath_ffnet_withChapter() {
        Assert.assertEquals(
            Triple(StoryId(3384713), ChapterId(42), DriverType.FanFictionNet),
            DeepLinkActivity.parsePath(Uri.parse("https://m.fanfiction.net/s/3384713/42"))
        )
    }

    @Test
    fun parsePath_ffnet_withChapterName() {
        Assert.assertEquals(
            Triple(StoryId(3384714), ChapterId(24), DriverType.FanFictionNet),
            DeepLinkActivity.parsePath(Uri.parse("https://m.fanfiction.net/s/3384714/24/Hello"))
        )
    }

    @Test
    fun parsePath_ffnet_invalid() {
        Assert.assertThrows(IllegalArgumentException::class.java) {
            DeepLinkActivity.parsePath(Uri.parse("https://m.fanfiction.net/s/"))
        }
    }

    @Test
    fun parsePath_ao3_withoutChapter() {
        Assert.assertEquals(
            Triple(StoryId(23703567), ChapterId(null), DriverType.ArchiveOfOurOwn),
            DeepLinkActivity.parsePath(Uri.parse("https://archiveofourown.org/works/23703567"))
        )
    }

    @Test
    fun parsePath_ao3_withChapter() {
        Assert.assertEquals(
            Triple(StoryId(34804678), ChapterId(86709106), DriverType.ArchiveOfOurOwn),
            DeepLinkActivity.parsePath(Uri.parse("https://archiveofourown.org/works/34804678/chapters/86709106"))
        )
    }

}