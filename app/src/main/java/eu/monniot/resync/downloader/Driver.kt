package eu.monniot.resync.downloader

import android.annotation.SuppressLint
import android.content.Context
import android.os.Environment
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import eu.monniot.resync.BuildConfig
import kotlinx.coroutines.*
import java.io.File

// Seam for testing: the chapter-selection/rate-limit-retry state machine in
// downloadLogic (ui/downloader/DownloadScreen.kt) only needs this single method, not
// the WebView plumbing the rest of Driver carries. Depending on the interface instead
// of the concrete Driver class lets that state machine be driven with a fake in unit
// tests, without a WebView or a real Context.
interface ChapterReader {
    suspend fun readChapter(storyId: StoryId, chapterId: ChapterId): Chapter
}

// Because of its reliance of a WebView, the Driver should be in the same
// composable tree as the WebView. That way, if the view is being removed
// the driver will be too and no leak will occurs.
// The decision to have the WebView been set outside of init is because
// we still want the driver to leave alongside the WebView, not within it
abstract class Driver : ChapterReader {
    private var view: WebView? = null
    private val ready = CompletableDeferred<Unit>()

    // Seam for testing: production code always goes through the real WebView API,
    // but the BuildConfig.DEBUG gating below is otherwise impossible to unit test
    // since the flag is a compile-time constant baked in by the build variant.
    internal var webContentsDebuggingSetter: (Boolean) -> Unit =
        WebView::setWebContentsDebuggingEnabled

    protected abstract val ioDispatcher: CoroutineDispatcher
    protected abstract val tmpChaptersFolder: File

    abstract fun makeUrl(storyId: StoryId, chapterId: ChapterId): String

    abstract fun parseWebPage(source: String, storyId: StoryId, chapterId: ChapterId): Chapter

    @SuppressLint("SetJavaScriptEnabled")
    fun installGrabber(view: WebView, debugBuild: Boolean = BuildConfig.DEBUG) {
        // Idempotent: AndroidView's update lambda can be invoked again for the same
        // WebView instance (e.g. on unrelated recompositions). Re-running the setup
        // below would needlessly reassign the webViewClient; skip it if this driver
        // is already attached to this exact view.
        if (this.view === view) return

        if (debugBuild) webContentsDebuggingSetter(true)
        this.view = view
        view.settings.javaScriptEnabled = true
        view.webViewClient = makeWebViewClient()
        ready.complete(Unit)
    }

    suspend fun ready() {
        this.ready.await()
    }

    // Could take a Context here and make the temporary folder
    // That would mean less places where the
    override suspend fun readChapter(storyId: StoryId, chapterId: ChapterId): Chapter {
        val tmpChapterFile = tmpChaptersFolder.resolve("${storyId.id}/${chapterId.id}.html")
        val tmpChapter = withContext(ioDispatcher) {
            if (!tmpChapterFile.exists()) return@withContext null

            val html = tmpChapterFile.readText()
            try {
                val c = parseWebPage(html, storyId, chapterId)
                println("Got chapter from local cache (${tmpChapterFile.path})")
                return@withContext c
            } catch (e: Exception) {
                println("Couldn't parse locally stored file. Error: $e")
                return@withContext null
            }
        }

        if (tmpChapter != null) return tmpChapter

        val jsInterface = JsInterface()
        view?.addJavascriptInterface(jsInterface, "grabber")
        view?.loadUrl(makeUrl(storyId, chapterId))

        var chapter: Chapter? = null

        while (chapter == null) {
            try {
                val html = jsInterface.waitForHtml()
                chapter = parseWebPage(html, storyId, chapterId)

                withContext(ioDispatcher) {
                    println("parent file = ${tmpChapterFile.parentFile}")
                    tmpChapterFile.parentFile?.mkdirs()
                    tmpChapterFile.writeText(html)
                }
            } catch (e: WaitAndTryAgain) {
                println("driver told us to wait and try to extract again. Waiting 5 seconds")
                jsInterface.resetText()
                delay(5000)

                println("Loading extract script (again)")
                view?.loadUrl(extractSourceUrl)
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
