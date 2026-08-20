package eu.monniot.resync.downloader

import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

// Covers the chapter-HTML on-disk cache: each driver getting its own subdirectory (so
// ff.net's small integer chapter ids and AO3's 8-digit ones can never collide), the
// null-chapter-id (AO3 one-shot) filename no longer being the literal string "null.html",
// and the eviction function that clears a story's cache once its epub has been built.
//
// Plain JUnit (no Robolectric): everything exercised here is pure Kotlin/File APIs.
class ChapterCacheTest {

    @get:Rule
    val folder: TemporaryFolder = TemporaryFolder()

    @Test
    fun ao3AndFfnetDrivers_useDifferentCacheSubdirectories() {
        val ao3 = ArchiveOfOurOwnDriver(folder.root)
        val ffnet = FanFictionNetDriver(folder.root)

        val ao3Dir = ao3.storyCacheDir(StoryId(1))
        val ffnetDir = ffnet.storyCacheDir(StoryId(1))

        Assert.assertNotEquals(
            "the two drivers must not resolve the same story to the same cache directory",
            ao3Dir,
            ffnetDir
        )
    }

    @Test
    fun chapterCacheFileName_forAO3OneShot_isNotTheLiteralStringNullHtml() {
        // AO3 one-shots are fetched/parsed with ChapterId(null) - see
        // ArchiveOfOurOwnDriver.makeUrl.
        val fileName = chapterCacheFileName(ChapterId(null))

        Assert.assertNotEquals("null.html", fileName)
        Assert.assertEquals("oneshot.html", fileName)
    }

    @Test
    fun chapterCacheFileName_forRegularChapter_usesTheChapterId() {
        val fileName = chapterCacheFileName(ChapterId(42))

        Assert.assertEquals("42.html", fileName)
    }

    @Test
    fun clearChapterCache_removesAllCachedChapters_forTheStory() {
        val driver = ArchiveOfOurOwnDriver(folder.root)
        val storyDir = driver.storyCacheDir(StoryId(35336083))
        storyDir.mkdirs()
        storyDir.resolve("oneshot.html").writeText("<html></html>")
        storyDir.resolve("86665087.html").writeText("<html></html>")

        Assert.assertTrue(storyDir.exists())

        clearChapterCache(storyDir)

        Assert.assertFalse(
            "the story's cache directory should be gone after clearing it",
            storyDir.exists()
        )
    }

    @Test
    fun clearChapterCache_doesNotTouchOtherStories_inTheSameDriverCache() {
        val driver = ArchiveOfOurOwnDriver(folder.root)
        val storyToClear = driver.storyCacheDir(StoryId(1))
        val otherStory = driver.storyCacheDir(StoryId(2))
        storyToClear.mkdirs()
        otherStory.mkdirs()
        storyToClear.resolve("oneshot.html").writeText("<html></html>")
        otherStory.resolve("oneshot.html").writeText("<html></html>")

        clearChapterCache(storyToClear)

        Assert.assertFalse(storyToClear.exists())
        Assert.assertTrue(
            "clearing one story's cache must not remove a sibling story's cache",
            otherStory.resolve("oneshot.html").exists()
        )
    }

    @Test
    fun clearChapterCache_onAMissingDirectory_doesNotThrow() {
        val neverCreated = folder.root.resolve("does-not-exist")

        clearChapterCache(neverCreated)
    }
}
