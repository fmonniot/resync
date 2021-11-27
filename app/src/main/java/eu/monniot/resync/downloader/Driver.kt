package eu.monniot.resync.downloader

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.CompletableDeferred

// Because of its reliance of a WebView, the Driver should be in the same
// composable tree as the WebView. That way, if the view is being removed
// the driver will be too and no leak will occurs.
// The decision to have the WebView been set outside of init is because
// we still want the driver to leave alongside the WebView, not within it
abstract class Driver {
    private var view: WebView? = null
    private val ready = CompletableDeferred<Unit>()

    abstract fun makeUrl(storyId: StoryId, chapterId: ChapterId): String

    abstract fun parseWebPage(source: String, storyId: StoryId, chapterId: ChapterId): Chapter

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
        val jsInterface = JsInterface()
        view?.addJavascriptInterface(jsInterface, "grabber")
        view?.loadUrl(makeUrl(storyId, chapterId))

        val html = jsInterface.waitForHtml()
        return parseWebPage(html, storyId, chapterId)
    }

    private fun makeWebViewClient(): WebViewClient {
        return object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                println("page loaded @ $url")

                view?.loadUrl(
                    "javascript:window.grabber.extractSource(document.querySelector('html').innerHTML);"
                )
            }
        }
    }

    companion object {
        private class JsInterface {

            private val chapterText = CompletableDeferred<String>()

            suspend fun waitForHtml(): String = chapterText.await()

            // JS methods

            @JavascriptInterface
            fun extractSource(html: String) {
                chapterText.complete("<html>${html}</html>")
            }
        }
    }
}
