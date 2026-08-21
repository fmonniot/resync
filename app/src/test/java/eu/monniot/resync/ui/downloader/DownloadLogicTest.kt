package eu.monniot.resync.ui.downloader

import eu.monniot.resync.downloader.Chapter
import eu.monniot.resync.downloader.ChapterId
import eu.monniot.resync.downloader.Driver
import eu.monniot.resync.downloader.DriverType
import eu.monniot.resync.downloader.StoryId
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Covers `downloadLogic` (the orchestration function driving the whole download: fetch, epub
 * build, cache eviction, and the Success summary text) with a minimal fake [Driver] instead of a
 * WebView-backed one. [FakeDriver] still routes through Driver's real on-disk cache read path
 * (see DriverReadChapterCacheHitTest), just with canned Chapter data instead of real HTML/jsoup
 * parsing, so no fixtures or WebView are needed - only chapters this test has pre-seeded into the
 * cache are ever "readable"; anything else fails loudly instead of hanging on a WebView call that
 * will never happen.
 */
class DownloadLogicTest {

    @get:Rule
    val folder: TemporaryFolder = TemporaryFolder()

    private class FakeDriver(
        filesDir: File,
        private val chapters: Map<ChapterId, Chapter>,
    ) : Driver() {
        override val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
        override val tmpChaptersFolder: File = filesDir.resolve("fake")

        override fun makeUrl(storyId: StoryId, chapterId: ChapterId): String =
            error("FakeDriver should only ever serve pre-cached chapters")

        override fun parseWebPage(source: String, storyId: StoryId, chapterId: ChapterId): Chapter =
            chapters[chapterId] ?: error("no fake chapter registered for $chapterId")
    }

    private fun chapter(
        num: Int,
        chapterId: ChapterId,
        totalChapters: Int,
        chapterIndex: Map<Int, ChapterId> = emptyMap(),
        storyName: String = "The Story",
    ): Chapter = Chapter(
        storyId = StoryId(1),
        chapterId = chapterId,
        chapterIndex = chapterIndex,
        num = num,
        chapterName = null,
        storyName = storyName,
        author = "The Author",
        totalChapters = totalChapters,
        content = "chapter $num body",
    )

    private fun driverWith(vararg chapters: Chapter): FakeDriver {
        val driver = FakeDriver(folder.newFolder().resolve("files"), chapters.associateBy { it.chapterId })
        for (c in chapters) {
            val file = driver.storyCacheDir(c.storyId).resolve("${c.chapterId.id ?: "oneshot"}.html")
            file.parentFile?.mkdirs()
            // FakeDriver.parseWebPage ignores this content entirely - only its presence on disk
            // (making readChapter's cache check succeed) matters here.
            file.writeText("ignored")
        }
        return driver
    }

    @Test
    fun oneShot_writesTheEpub_andSummarisesItAsTheWholeStory() = runTest {
        val storyId = StoryId(1)
        val initial = chapter(1, ChapterId(null), totalChapters = 1, storyName = "Solo Story")
        val driver = driverWith(initial)
        val states = mutableListOf<DownloadState>()

        downloadLogic(
            folder.root, storyId, ChapterId(null), DriverType.FanFictionNet, driver,
        ) { states.add(it) }

        val success = states.last() as DownloadState.Success
        assertEquals("Solo Story.epub", success.fileName)
        assertEquals("Solo Story saved as an EPUB, ready to send to reMarkable.", success.summary)
        assertTrue("the epub file should have been written to disk", success.epubFile.exists())
        assertFalse(
            "the story's chapter cache should be cleared once the epub is built",
            driver.storyCacheDir(storyId).exists()
        )
    }

    @Test
    fun singleChapterOfAMultiChapterStory_summarisesJustThatChapter() = runTest {
        val storyId = StoryId(1)
        val index = mapOf(1 to ChapterId(1), 2 to ChapterId(2))
        val initial = chapter(1, ChapterId(1), totalChapters = 2, chapterIndex = index)
        // Chapter 2 is deliberately not seeded: selecting the already-downloaded chapter 1
        // again must not trigger a fetch for anything else.
        val driver = driverWith(initial)
        val states = mutableListOf<DownloadState>()

        downloadLogic(
            folder.root, storyId, ChapterId(1), DriverType.FanFictionNet, driver,
            setState = { state ->
                states.add(state)
                if (state is DownloadState.ConfirmChapters) state.onUserConfirmation(ChapterSelection.One(1))
            },
        )

        val success = states.last() as DownloadState.Success
        assertEquals("The Story - Ch 1.epub", success.fileName)
        assertEquals(
            "The Story — chapter 1 saved as an EPUB, ready to send to reMarkable.",
            success.summary
        )
    }

    @Test
    fun rangeOfChapters_fetchesThemAndSummarisesTheRange() = runTest {
        val storyId = StoryId(1)
        val index = mapOf(1 to ChapterId(1), 2 to ChapterId(2))
        val initial = chapter(1, ChapterId(1), totalChapters = 2, chapterIndex = index)
        val second = chapter(2, ChapterId(2), totalChapters = 2, chapterIndex = index)
        val driver = driverWith(initial, second)
        val states = mutableListOf<DownloadState>()

        downloadLogic(
            folder.root, storyId, ChapterId(1), DriverType.FanFictionNet, driver,
            setState = { state ->
                states.add(state)
                if (state is DownloadState.ConfirmChapters) state.onUserConfirmation(ChapterSelection.Range(1, 2))
            },
        )

        val success = states.last() as DownloadState.Success
        assertEquals("The Story - Ch 1-2.epub", success.fileName)
        assertEquals(
            "The Story — chapters 1–2 saved as an EPUB, ready to send to reMarkable.",
            success.summary
        )
    }
}
