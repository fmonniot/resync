package eu.monniot.resync.downloader

import android.content.Context
import android.webkit.WebView
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import kotlin.concurrent.thread

// Using Robolectric to have access to a real android.webkit.WebView in unit tests.
//
// BuildConfig.DEBUG is a compile-time constant baked in per build variant (unit tests
// always run against the debug variant), so it can't be flipped from a test. installGrabber
// instead takes debugBuild as a parameter (defaulting to BuildConfig.DEBUG for production
// callers) so both branches of the release/debug gate are exercised here.
@RunWith(RobolectricTestRunner::class)
class DriverTest {

    @get:Rule
    val folder: TemporaryFolder = TemporaryFolder()

    private fun makeWebView(): WebView = WebView(RuntimeEnvironment.getApplication())

    @Test
    fun installGrabber_isIdempotent_forTheSameWebView() {
        val driver = FanFictionNetDriver(folder.root)
        val view = makeWebView()

        driver.installGrabber(view)
        val clientAfterFirstInstall = view.webViewClient

        driver.installGrabber(view)
        val clientAfterSecondInstall = view.webViewClient

        Assert.assertSame(
            "installGrabber should not reassign the webViewClient when called again with the same WebView",
            clientAfterFirstInstall,
            clientAfterSecondInstall
        )
    }

    @Test
    fun installGrabber_reattaches_whenGivenADifferentWebView() {
        val driver = FanFictionNetDriver(folder.root)
        val firstView = makeWebView()
        val secondView = makeWebView()

        driver.installGrabber(firstView)
        driver.installGrabber(secondView)

        Assert.assertNotNull(secondView.webViewClient)
        Assert.assertTrue(secondView.settings.javaScriptEnabled)
    }

    @Test
    fun installGrabber_enablesWebContentsDebugging_onDebugBuilds() {
        val driver = FanFictionNetDriver(folder.root)
        var enabledWith: Boolean? = null
        driver.webContentsDebuggingSetter = { enabledWith = it }

        driver.installGrabber(makeWebView(), debugBuild = true)

        Assert.assertEquals(true, enabledWith)
    }

    @Test
    fun installGrabber_doesNotEnableWebContentsDebugging_onReleaseBuilds() {
        val driver = FanFictionNetDriver(folder.root)
        var wasCalled = false
        driver.webContentsDebuggingSetter = { wasCalled = true }

        driver.installGrabber(makeWebView(), debugBuild = false)

        Assert.assertFalse(wasCalled)
    }

    // -- readChapter's cache-miss / network-fetch path --
    //
    // readChapter's cache-hit path is covered by DriverReadChapterCacheHitTest without
    // needing a WebView at all. These tests cover the other branch: kicking off the
    // fetch through the WebView, feeding the result back through the "grabber"
    // @JavascriptInterface, and (for the Cloudflare interstitial) retrying instead of
    // giving up. Robolectric's WebView shadow doesn't actually run JavaScript or call
    // onPageFinished for us, so a background thread stands in for "the page loaded and
    // the injected extractor script ran", driving the same JsInterface object readChapter
    // itself created and registered.

    private fun getResourceAsText(path: String): String =
        javaClass.classLoader!!.getResource(path)!!.readText()

    // WebView doesn't expose which JavascriptInterface objects were registered on it.
    // ShadowWebView.getJavascriptInterface(name) (see the ticket this covers) gives it
    // back as Any - Driver's JsInterface is private, so it can't be named here - and
    // reflection stands in for what the injected JS would do: call its
    // @JavascriptInterface extractSource method.
    private fun awaitJsInterface(view: WebView): Any {
        var jsInterface: Any? = Shadows.shadowOf(view).getJavascriptInterface("grabber")
        while (jsInterface == null) {
            Thread.sleep(1)
            jsInterface = Shadows.shadowOf(view).getJavascriptInterface("grabber")
        }
        return jsInterface
    }

    private fun extractSource(jsInterface: Any, html: String) {
        val method = jsInterface.javaClass.getMethod("extractSource", String::class.java)
        method.isAccessible = true
        method.invoke(jsInterface, html)
    }

    // Records every URL loadUrl is called with, so the retry test can prove readChapter
    // reloads the extractor script (rather than giving up) after a simulated Cloudflare
    // interstitial. loadUrl is a regular method on the real android.webkit.WebView class,
    // so overriding it here works the same way it would on any other WebView subclass.
    private class RecordingWebView(context: Context) : WebView(context) {
        val loadedUrls = mutableListOf<String>()

        override fun loadUrl(url: String) {
            synchronized(loadedUrls) { loadedUrls.add(url) }
            super.loadUrl(url)
        }

        fun loadCount(url: String): Int = synchronized(loadedUrls) { loadedUrls.count { it == url } }
    }

    @Test
    fun readChapter_cacheMiss_fetchesViaTheWebView_andWritesThroughToTheDiskCache() = runTest {
        val driver = FanFictionNetDriver(folder.root)
        val view = makeWebView()
        driver.installGrabber(view)

        val storyId = StoryId(3384712)
        val chapterId = ChapterId(23)
        val rawHtml = getResourceAsText("ffnet/s-3384712-23.html")

        // Stands in for "the page loaded, the extractor script ran, and the JS bridge
        // handed the HTML back" - runs on a real thread since it must happen concurrently
        // with readChapter suspending on jsInterface.waitForHtml() below.
        val feeder = thread {
            val jsInterface = awaitJsInterface(view)
            extractSource(jsInterface, rawHtml)
        }

        val chapter = driver.readChapter(storyId, chapterId)
        feeder.join()

        // JsInterface.extractSource wraps whatever the JS bridge hands back in an <html>
        // tag (document.querySelector('html').innerHTML strips it on the real page), so
        // that's what readChapter actually parses and caches - not the raw fixture.
        val wrappedHtml = "<html>$rawHtml</html>"
        assertEquals(driver.parseWebPage(wrappedHtml, storyId, chapterId), chapter)
        assertEquals(
            "the fetched HTML should be written through to the on-disk cache",
            wrappedHtml,
            driver.storyCacheDir(storyId).resolve(chapterCacheFileName(chapterId)).readText()
        )
    }

    @Test
    fun readChapter_cloudflareInterstitial_retriesTheExtractorScript_thenSucceeds() = runTest {
        val driver = FanFictionNetDriver(folder.root)
        val view = RecordingWebView(RuntimeEnvironment.getApplication())
        driver.installGrabber(view)

        val storyId = StoryId(3384712)
        val chapterId = ChapterId(23)
        // FanFictionNetDriver.parseWebPage throws WaitAndTryAgain when it sees this
        // fixture's Cloudflare interstitial markup ("DDoS protection by").
        val cloudflareHtml = getResourceAsText("ffnet/s-cloudflare.html")
        val realHtml = getResourceAsText("ffnet/s-3384712-23.html")

        val feeder = thread {
            val jsInterface = awaitJsInterface(view)

            // First response looks like FF.Net's Cloudflare interstitial: readChapter
            // should treat this as WaitAndTryAgain and retry, not propagate the exception.
            extractSource(jsInterface, cloudflareHtml)

            // Wait for readChapter's retry branch to reload the extractor script before
            // feeding the real chapter through - it's the same JsInterface instance the
            // whole time (its internal CompletableDeferred is reset, not replaced), so
            // feeding the real HTML too early would land on the already-completed first
            // deferred and be silently dropped.
            while (view.loadCount(EXTRACT_SOURCE_URL) < 1) {
                Thread.sleep(1)
            }
            extractSource(jsInterface, realHtml)
        }

        val chapter = driver.readChapter(storyId, chapterId)
        feeder.join()

        val wrappedHtml = "<html>$realHtml</html>"
        assertEquals(driver.parseWebPage(wrappedHtml, storyId, chapterId), chapter)
        assertEquals(
            "expected exactly one retry: the extractor script reloaded once after the Cloudflare interstitial",
            1,
            view.loadCount(EXTRACT_SOURCE_URL)
        )
    }

    companion object {
        // Mirrors Driver's own private extractSourceUrl companion constant, which isn't
        // visible outside Driver - duplicated here so the retry test can recognize when
        // readChapter reloads the extractor script.
        private const val EXTRACT_SOURCE_URL =
            "javascript:window.grabber.extractSource(document.querySelector('html').innerHTML);"
    }
}
