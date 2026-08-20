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
}
