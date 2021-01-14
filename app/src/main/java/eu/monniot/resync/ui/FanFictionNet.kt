package eu.monniot.resync.ui

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow


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

// This Screen is used to get a story (or a specific chapter if asked) from
// the https://m.fanfiction.net platform.
@Composable
fun GetChaptersView(
    storyId: StoryId,
    chapterSelection: ChapterSelection,
    c: CompletableDeferred<List<Chapter>>,
    currentChapterDownloading: MutableStateFlow<ChapterNum>? = null
) {
    val driver = Driver()

    LaunchedEffect(subject = storyId) {
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
    AndroidView(viewBlock = ::WebView, modifier = Modifier.size(1.dp, 1.dp)) { webView ->
        driver.installGrabber(webView)
    }
}


// TODO Could we refactor this API to not have to create a new WebView every time but instead
// share a driver/webview instance for multiple calls ?
// Also at the same time we should see how to not download same chapter multiple times in the
// DownloadScreen component
class Driver {
    private var view: WebView? = null
    private val ready = CompletableDeferred<Unit>()

    @SuppressLint("SetJavaScriptEnabled")
    fun installGrabber(view: WebView) {
        this.view = view
        view.settings.javaScriptEnabled = true
        view.webViewClient = makeWebViewClient()
        ready.complete(Unit)
    }

    suspend fun ready() {
        this.ready.await()
    }

    suspend fun readChapter(storyId: StoryId, chapterNum: ChapterNum): Chapter {
        val jsInterface = JsInterface(chapterNum)
        view?.addJavascriptInterface(jsInterface, "grabber")
        view?.loadUrl("https://m.fanfiction.net/s/${storyId}/${chapterNum}")
        return jsInterface.waitForChapter()
    }

    private fun makeWebViewClient(): WebViewClient {
        return object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                println("page loaded @ $url")
                val scriptText =
                    "javascript:window.grabber.%s(document.querySelector('%s').innerText);"
                val scriptHtml =
                    "javascript:window.grabber.%s(new XMLSerializer().serializeToString(document.querySelector('%s')));"
                val scriptChapterName =
                    "javascript:window.grabber.onChapterName((Array.apply([], document.querySelector('#content').childNodes || []).filter(n => n.nodeType === 3).pop() || {}).textContent);"
                val scriptTotalChapters =
                    "javascript:window.grabber.onTotalChapters((Array.apply([], document.querySelector('#top > div[align]').childNodes || []).filter(n => n.href !== undefined && n.text !== 'Next »').map(n => n.textContent) || []).pop())"

                view?.loadUrl(scriptHtml.format("onChapterText", ".storycontent"))
                view?.loadUrl(scriptText.format("onStoryName", "#content > div > b"))
                view?.loadUrl(scriptText.format("onAuthorName", "#content > div > a"))
                view?.loadUrl(scriptChapterName)
                view?.loadUrl(scriptTotalChapters)
            }
        }
    }

}

class JsInterface(
    private val chapterNum: ChapterNum
) {

    private val chapterText = CompletableDeferred<String>()
    private val chapterName = CompletableDeferred<String?>()
    private val storyName = CompletableDeferred<String>()
    private val authorName = CompletableDeferred<String>()
    private val totalChapters = CompletableDeferred<Int>()

    suspend fun waitForChapter(): Chapter {
        return Chapter(
            chapterNum,
            chapterName.await(),
            storyName.await(),
            authorName.await(),
            totalChapters.await(),
            chapterText.await(),
        )
    }

    // JS methods

    @JavascriptInterface
    fun onChapterText(html: String?) {
        println("onChapterText: [redacted]")
        if (html == null) {
            println("WARN: Got empty story text")
        } else {
            chapterText.complete(html)
        }
    }

    @JavascriptInterface
    fun onChapterName(html: String?) {
        println("onChapterName: |$html|")
        if (html == null) {
            println("WARN: Got empty chapter name")
        } else {
            // One-chapter story don't have a chapter name
            if (html.isBlank()) {
                chapterName.complete(null)
            } else {
                chapterName.complete(html.trim())
            }
        }
    }

    @JavascriptInterface
    fun onStoryName(html: String?) {
        println("onStoryName: $html")
        if (html == null) {
            println("WARN: Got empty story name")
        } else {
            storyName.complete(html)
        }
    }

    @JavascriptInterface
    fun onAuthorName(html: String?) {
        println("onAuthorName: $html")
        if (html == null) {
            println("WARN: Got empty author name")
        } else {
            authorName.complete(html)
        }
    }

    @JavascriptInterface
    fun onTotalChapters(html: String?) {
        println("onTotalChapters: $html")

        if (html == "undefined") {
            // One-chapter story
            totalChapters.complete(1)
            return
        }

        val n = html?.toIntOrNull()
        if (n == null) {
            println("WARN: Invalid last chapters: $html")
        } else {
            totalChapters.complete(n)
        }
    }
}

data class Chapter(
    val num: ChapterNum, val chapterName: String?, val storyName: String,
    val author: String, val totalChapters: Int, val content: String
)
