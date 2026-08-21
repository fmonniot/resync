# Cover Driver.readChapter's network-fetch and rate-limit-retry path

**Priority:** medium
**Area:** `app/src/main/java/eu/monniot/resync/downloader/Driver.kt`

## Problem

`Driver.kt` is 27.4% covered (17/62 lines). `readChapter`'s on-disk cache-hit path is well tested
(`DriverReadChapterCacheHitTest`, `ChapterCacheTest`), and `installGrabber` is covered
(`DriverTest`). But the actual point of the WebView hack — `readChapter`'s cache-miss branch,
`Driver.kt:88-114` — has no test at all:

- `view?.addJavascriptInterface(jsInterface, "grabber")` + `view?.loadUrl(makeUrl(...))` kicking
  off the fetch (lines 88-90).
- The retry loop (lines 94-112): on success, `parseWebPage` + write-through to the on-disk cache
  (lines 96-103); on `WaitAndTryAgain` (Cloudflare interstitial), reset the JS deferred, `delay`,
  and reload the extractor script (lines 104-111) — this loop is the one piece of code that makes
  FF.Net's Cloudflare workaround actually work, and it's currently unverified by anything.

`makeWebViewClient()` (`:117-125`, `onPageFinished` triggering the extractor script) and the
private `JsInterface` (`:135-151`, the `@JavascriptInterface` bridge itself) are the supporting
pieces and are exercised transitively by testing the above.

## Proposed fix

Extend `DriverTest.kt` (already `@RunWith(RobolectricTestRunner::class)`, already has a real
`WebView` via `RuntimeEnvironment.getApplication()`) with tests that drive `readChapter` end to
end against that WebView:

- Happy path: call `readChapter` on a driver with an empty cache, and from the test, invoke the
  registered `"grabber"` JS interface's `extractSource(html)` (reachable via
  `view.getJsInterface(...)` — WebView doesn't expose this directly, so either capture the
  interface object from `addJavascriptInterface` via a small test seam, or use
  `webView.evaluateJavascript` if Robolectric's WebView shadow supports invoking registered JS
  interfaces that way). Assert the returned `Chapter` matches `parseWebPage`'s result and that the
  HTML got written to `storyCacheDir`.
- Retry path: have the fake extraction throw/signal `WaitAndTryAgain` once, then succeed; assert
  `readChapter` retries (a second `loadUrl` to the extractor script) rather than giving up, and
  that it eventually returns.

If driving the real `JsInterface` through Robolectric's WebView shadow proves impractical, the
fallback is to extract the retry loop's control flow (success vs. `WaitAndTryAgain` vs. give-up)
into a small pure function against a fake "fetch HTML" suspend lambda — mirroring how
`selectChaptersToDownload` was pulled out of `downloadLogic` for the same reason (see
`ChapterReader` in this same file, and `SelectChaptersToDownloadTest`). Prefer testing the real
thing first; only reach for the extraction if the WebView route is a dead end.

## Acceptance criteria

- [ ] `readChapter`'s cache-miss success path is covered, including the on-disk cache write.
- [ ] The `WaitAndTryAgain` retry loop is covered: at least one test proves a transient failure is
      retried rather than propagated.
- [ ] `./gradlew test` passes.
- [ ] `Driver.kt`'s line coverage in `jacocoTestReport` moves well past the current 27.4%.
