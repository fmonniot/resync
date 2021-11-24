package eu.monniot.resync.ui

import android.webkit.WebView
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import eu.monniot.resync.downloader.FanFictionNetDriver
import kotlinx.coroutines.CompletableDeferred


typealias StoryId = Int
typealias ChapterNum = Int

sealed class ChapterSelection {
    object All : ChapterSelection()
    data class One(val num: ChapterNum) : ChapterSelection()
    data class Range(val start: ChapterNum, val end: ChapterNum) : ChapterSelection()

    fun firstChapter(): ChapterNum = when (this) {
        All -> 1
        is One -> num
        is Range -> start
    }

    fun lastChapter(totalChapters: ChapterNum? = null): ChapterNum? = when (this) {
        All -> if (totalChapters == null) null else {
            if (totalChapters > 1) totalChapters else null
        }
        is One -> null
        is Range -> if (end > firstChapter()) end else null
    }
}

// TODO Could we refactor this API to not have to create a new WebView every time but instead
// share a driver/webview instance for multiple calls ?
// Also at the same time we should see how to not download same chapter multiple times in the
// DownloadScreen component
// This Screen is used to get a story (or a specific chapter if asked) from
// the https://m.fanfiction.net platform.
@Composable
fun GetChaptersView(
    storyId: StoryId,
    chapterSelection: ChapterSelection,
    c: CompletableDeferred<List<Chapter>>,
    currentChapterDownloading: MutableState<ChapterNum>? = null
) {
    val driver = FanFictionNetDriver()

    LaunchedEffect(key1 = storyId) {
        println("Drawing GetChaptersView(storyId=$storyId, chapterSelection=$chapterSelection)")

        // wait for the driver to be attached to a running webview
        driver.ready()

        println("driver ready, reading first chapter")
        val firstChapterNum = chapterSelection.firstChapter()
        currentChapterDownloading?.value = firstChapterNum
        val chapter = driver.readChapter(storyId, firstChapterNum)

        val chapters = mutableListOf(chapter)

        // Now check if we need to download remaining chapters or not
        val endChapterNum = chapterSelection.lastChapter(chapter.totalChapters)
        if (endChapterNum != null) {
            // + 1 because we already have firstChapterNum,
            // basically I want ]first, last] instead of [first, last]
            for (i in (firstChapterNum + 1)..endChapterNum) {
                currentChapterDownloading?.value = i
                chapters.add(driver.readChapter(storyId, i))
            }
        }

        c.complete(chapters)
    }

    // We need a web view to grab the fiction content
    AndroidView(factory = ::WebView, modifier = Modifier.size(1.dp, 1.dp)) { webView ->
        driver.installGrabber(webView)
    }
}

data class Chapter(
    val num: ChapterNum, val chapterName: String?, val storyName: String,
    val author: String, val totalChapters: Int, val content: String
)
