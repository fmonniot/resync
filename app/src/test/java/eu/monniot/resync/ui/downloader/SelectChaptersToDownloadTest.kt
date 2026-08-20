package eu.monniot.resync.ui.downloader

import eu.monniot.resync.downloader.Chapter
import eu.monniot.resync.downloader.ChapterId
import eu.monniot.resync.downloader.ChapterReader
import eu.monniot.resync.downloader.Driver
import eu.monniot.resync.downloader.DriverType
import eu.monniot.resync.downloader.StoryId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the chapter-selection/rate-limit-retry state machine extracted out of
 * `downloadLogic` in DownloadScreen.kt. These drive [selectChaptersToDownload] (and, for the
 * rate-limit path, [readWithRateLimit] transitively) with a fake [ChapterReader] instead of a
 * WebView-backed [Driver], so no Context/WebView is needed.
 */
class SelectChaptersToDownloadTest {

    private fun chapter(
        num: Int,
        totalChapters: Int,
        chapterIndex: Map<Int, ChapterId> = emptyMap(),
        chapterId: ChapterId = ChapterId(num),
    ): Chapter = Chapter(
        storyId = StoryId(1),
        chapterId = chapterId,
        chapterIndex = chapterIndex,
        num = num,
        chapterName = "Chapter $num",
        storyName = "Story",
        author = "Author",
        totalChapters = totalChapters,
        content = "content $num",
    )

    private fun fakeReader(vararg chapters: Pair<ChapterId, Chapter>): ChapterReader {
        val byId = chapters.toMap()
        return object : ChapterReader {
            override suspend fun readChapter(storyId: StoryId, chapterId: ChapterId): Chapter =
                byId[chapterId] ?: error("no fake chapter registered for $chapterId")
        }
    }

    private fun confirmWith(selection: ChapterSelection): (DownloadState) -> Unit = { state ->
        if (state is DownloadState.ConfirmChapters) {
            state.onUserConfirmation(selection)
        }
    }

    @Test
    fun oneShot_doesNotAskTheUser_andReturnsTheInitialChapterAsTheWholeStory() = runTest {
        val initial = chapter(1, totalChapters = 1)

        val states = mutableListOf<DownloadState>()

        val (chapters, wholeStory) = selectChaptersToDownload(
            initial,
            DriverType.FanFictionNet,
            fakeReader(),
            setState = { states.add(it) },
        )

        assertEquals(listOf(initial), chapters)
        assertTrue(wholeStory)
        assertTrue("a one-shot should never prompt the user", states.isEmpty())
    }

    @Test
    fun allSelection_fetchesEveryOtherKnownChapter_sortedByNumber() = runTest {
        val index = mapOf(1 to ChapterId(1), 2 to ChapterId(2), 3 to ChapterId(3))
        val initial = chapter(1, totalChapters = 3, chapterIndex = index, chapterId = ChapterId(1))

        val reader = fakeReader(
            ChapterId(2) to chapter(2, totalChapters = 3),
            ChapterId(3) to chapter(3, totalChapters = 3),
        )

        // FanFictionNet is used (rather than ArchiveOfOurOwn) so the 1s inter-chapter delay
        // that AO3 downloads incur doesn't matter here; the delay behavior itself isn't what
        // this test is about.
        val (chapters, wholeStory) = selectChaptersToDownload(
            initial,
            DriverType.FanFictionNet,
            reader,
            setState = confirmWith(ChapterSelection.All),
        )

        assertEquals(listOf(1, 2, 3), chapters.map { it.num })
        assertTrue(wholeStory)
    }

    @Test
    fun rangeSelection_fetchesOnlyTheRequestedChapters() = runTest {
        val index = mapOf(
            1 to ChapterId(1),
            2 to ChapterId(2),
            3 to ChapterId(3),
            4 to ChapterId(4),
        )
        val initial = chapter(1, totalChapters = 4, chapterIndex = index, chapterId = ChapterId(1))

        val reader = fakeReader(
            ChapterId(2) to chapter(2, totalChapters = 4),
            ChapterId(3) to chapter(3, totalChapters = 4),
        )

        val (chapters, wholeStory) = selectChaptersToDownload(
            initial,
            DriverType.FanFictionNet,
            reader,
            setState = confirmWith(ChapterSelection.Range(1, 3)),
        )

        assertEquals(listOf(1, 2, 3), chapters.map { it.num })
        // Chapter 4 exists and wasn't selected, so this isn't the whole story.
        assertEquals(false, wholeStory)
    }

    @Test
    fun oneSelection_ofANonInitialChapter_replacesTheInitialChapter() = runTest {
        val index = mapOf(1 to ChapterId(1), 2 to ChapterId(2), 3 to ChapterId(3))
        val initial = chapter(1, totalChapters = 3, chapterIndex = index, chapterId = ChapterId(1))

        val reader = fakeReader(ChapterId(2) to chapter(2, totalChapters = 3))

        val (chapters, wholeStory) = selectChaptersToDownload(
            initial,
            DriverType.FanFictionNet,
            reader,
            setState = confirmWith(ChapterSelection.One(2)),
        )

        assertEquals(listOf(2), chapters.map { it.num })
        assertEquals(false, wholeStory)
    }

    @Test
    fun rateLimit_retriesUntilTheDriverSucceeds_andSurfacesANotice() = runTest {
        val index = mapOf(1 to ChapterId(1), 2 to ChapterId(2))
        val initial = chapter(1, totalChapters = 2, chapterIndex = index, chapterId = ChapterId(1))

        var attempts = 0
        val reader = object : ChapterReader {
            override suspend fun readChapter(storyId: StoryId, chapterId: ChapterId): Chapter {
                if (chapterId != ChapterId(2)) error("unexpected chapter requested: $chapterId")

                attempts += 1
                // Fail twice with AO3's rate-limit signal, then succeed on the third attempt.
                if (attempts < 3) throw Driver.Companion.RateLimited
                return chapter(2, totalChapters = 2)
            }
        }

        val notices = mutableListOf<String>()

        val (chapters, wholeStory) = selectChaptersToDownload(
            initial,
            DriverType.FanFictionNet,
            reader,
            setState = { state ->
                if (state is DownloadState.DownloadingRemainingChapters) {
                    state.notice?.let { notices.add(it) }
                }
                if (state is DownloadState.ConfirmChapters) {
                    state.onUserConfirmation(ChapterSelection.All)
                }
            },
        )

        assertEquals(3, attempts)
        assertEquals(listOf(1, 2), chapters.map { it.num })
        assertTrue(wholeStory)
        assertTrue("should have surfaced at least one rate-limit notice", notices.isNotEmpty())
        assertTrue(notices.all { it.contains("AO3 rate limit hit") })
    }

    @Test
    fun rateLimit_givesUpAndThrows_afterExhaustingRetries() = runTest {
        val index = mapOf(1 to ChapterId(1), 2 to ChapterId(2))
        val initial = chapter(1, totalChapters = 2, chapterIndex = index, chapterId = ChapterId(1))

        val reader = object : ChapterReader {
            override suspend fun readChapter(storyId: StoryId, chapterId: ChapterId): Chapter {
                throw Driver.Companion.RateLimited
            }
        }

        try {
            selectChaptersToDownload(
                initial,
                DriverType.FanFictionNet,
                reader,
                setState = confirmWith(ChapterSelection.All),
            )
            org.junit.Assert.fail("expected RateLimited to be thrown after exhausting retries")
        } catch (e: Driver.Companion.RateLimited) {
            // expected
        }
    }
}
