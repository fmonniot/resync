package eu.monniot.resync.downloader

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import eu.monniot.resync.ui.Chapter
import eu.monniot.resync.ui.ChapterNum
import eu.monniot.resync.ui.StoryId
import kotlinx.coroutines.CompletableDeferred


abstract class Driver {
    private var view: WebView? = null
    private val ready = CompletableDeferred<Unit>()

    abstract fun makeUrl(storyId: StoryId, chapterNum: ChapterNum): String

    protected abstract val chapterTextScript: String
    protected abstract val storyNameScript: String
    protected abstract val authorNameScript: String
    protected abstract val chapterNameScript: String
    protected abstract val totalChaptersScript: String

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
        view?.loadUrl(makeUrl(storyId, chapterNum))
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
            }
        }
    }

    companion object {
        private class JsInterface(
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

                val n = html?.toIntOrNull()
                if (n == null) {
                    // One-chapter story don't have a valid html (it returns the author instead)
                    totalChapters.complete(1)
                } else {
                    totalChapters.complete(n)
                }
            }
        }
    }
}
