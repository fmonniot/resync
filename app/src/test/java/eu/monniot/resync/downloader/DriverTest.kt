package eu.monniot.resync.downloader

import android.webkit.WebView
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

// Using Robolectric to have access to a real android.webkit.WebView in unit tests.
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
}
