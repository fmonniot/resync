package eu.monniot.resync.downloader

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Covers Driver.readChapter's on-disk cache read path: when a chapter's HTML is already cached
 * (see Driver.storyCacheDir/chapterCacheFileName), it's read and parsed straight from disk
 * without touching the WebView at all - no Robolectric or WebView instance needed here, unlike
 * DriverTest which covers the WebView-attachment side of Driver.
 */
class DriverReadChapterCacheHitTest {

    @get:Rule
    val folder: TemporaryFolder = TemporaryFolder()

    private fun getResourceAsText(path: String): String =
        javaClass.classLoader!!.getResource(path)!!.readText()

    private fun seedCache(driver: Driver, storyId: StoryId, chapterId: ChapterId, html: String) {
        val file = driver.storyCacheDir(storyId).resolve(chapterCacheFileName(chapterId))
        file.parentFile?.mkdirs()
        file.writeText(html)
    }

    @Test
    fun readChapter_ao3OneShot_returnsTheCachedChapter_withoutAWebView() = runTest {
        val driver = ArchiveOfOurOwnDriver(folder.root)
        val storyId = StoryId(35336083)
        val chapterId = ChapterId(null)
        val html = getResourceAsText("ao3/works-35336083.html")
        seedCache(driver, storyId, chapterId, html)

        val actual = driver.readChapter(storyId, chapterId)

        assertEquals(driver.parseWebPage(html, storyId, chapterId), actual)
    }

    @Test
    fun readChapter_ffnetChapter_returnsTheCachedChapter_withoutAWebView() = runTest {
        val driver = FanFictionNetDriver(folder.root)
        val storyId = StoryId(3384712)
        val chapterId = ChapterId(23)
        val html = getResourceAsText("ffnet/s-3384712-23.html")
        seedCache(driver, storyId, chapterId, html)

        val actual = driver.readChapter(storyId, chapterId)

        assertEquals(driver.parseWebPage(html, storyId, chapterId), actual)
    }
}
