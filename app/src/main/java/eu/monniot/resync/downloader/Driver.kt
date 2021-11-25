package eu.monniot.resync.downloader

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import eu.monniot.resync.ui.downloader.Chapter
import eu.monniot.resync.ui.downloader.StoryId
import eu.monniot.resync.ui.downloader.ChapterId
import kotlinx.coroutines.CompletableDeferred


abstract class Driver {
    private var view: WebView? = null
    private val ready = CompletableDeferred<Unit>()

    abstract fun makeUrl(storyId: StoryId, chapterId: ChapterId): String

    protected abstract val chapterTextScript: String
    protected abstract val storyNameScript: String
    protected abstract val authorNameScript: String
    protected abstract val chapterNameScript: String
    protected abstract val totalChaptersScript: String
    protected abstract val chapterNumScript: String

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

    suspend fun readChapter(storyId: StoryId, chapterId: ChapterId): Chapter {
        val jsInterface = JsInterface(storyId, chapterId)
        view?.addJavascriptInterface(jsInterface, "grabber")
        view?.loadUrl(makeUrl(storyId, chapterId))
        return jsInterface.waitForChapter()
    }

    private fun makeWebViewClient(): WebViewClient {
        return object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                println("page loaded @ $url")

                view?.loadUrl(chapterTextScript)
                view?.loadUrl(storyNameScript)
                view?.loadUrl(authorNameScript)
                view?.loadUrl(chapterNameScript)
                view?.loadUrl(totalChaptersScript)
                view?.loadUrl(chapterNumScript)
            }
        }
    }

    companion object {
        private class JsInterface(
            private val storyId: StoryId,
            private val chapterId: ChapterId
        ) {

            private val chapterText = CompletableDeferred<String>()
            private val chapterName = CompletableDeferred<String?>()
            private val storyName = CompletableDeferred<String>()
            private val authorName = CompletableDeferred<String>()
            private val totalChapters = CompletableDeferred<Int>()
            private val chapterNum = CompletableDeferred<Int>()

            suspend fun waitForChapter(): Chapter {
                return Chapter(
                    storyId,
                    chapterId,
                    chapterNum.await(),
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

                val n = html?.toIntOrNull()
                if (n == null) {
                    // FF.Net: One-chapter story don't have a valid html (it returns the author instead)
                    totalChapters.complete(1)
                } else {
                    totalChapters.complete(n)
                }
            }

            @JavascriptInterface
            fun onChapterNumber(html: String?) {
                println("onChapterNumber: $html")

                val n = html?.toIntOrNull()
                if (n == null) {
                    // FF.Net: url might not contains the number, in which case it's the first one
                    chapterNum.complete(1)
                } else {
                    chapterNum.complete(n)
                }
            }
        }
    }
}
