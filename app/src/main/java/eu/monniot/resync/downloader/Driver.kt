package eu.monniot.resync.downloader

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay

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
        WebView.setWebContentsDebuggingEnabled(true);
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

        var chapter: Chapter? = null

        while (chapter == null) {
            try {
                println("Loading extract script")
                view?.loadUrl(extractSourceUrl)

                val html = jsInterface.waitForHtml()
                chapter = parseWebPage(html, storyId, chapterId)
            } catch (e: WaitAndTryAgain) {
                println("driver told us to wait and try to extract again. Waiting 5 seconds")
                jsInterface.resetText()
                delay(5000)
            }
        }

        return chapter
    }

    private fun makeWebViewClient(): WebViewClient {
        return object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                println("onPageFinished @ $url")

                view?.loadUrl(extractSourceUrl)
            }
        }
    }

    companion object {
        // TODO Rename to something more telling: this exception says to reload page in a few
        object RateLimited : RuntimeException()
        object WaitAndTryAgain : RuntimeException()

        private const val extractSourceUrl =
            "javascript:window.grabber.extractSource(document.querySelector('html').innerHTML);"

        private class JsInterface {

            private var chapterText = CompletableDeferred<String>()

            suspend fun waitForHtml(): String = chapterText.await()

            fun resetText() {
                chapterText = CompletableDeferred()
            }

            // JS methods

            @JavascriptInterface
            fun extractSource(html: String) {
                chapterText.complete("<html>${html}</html>")
            }
        }
    }
}
