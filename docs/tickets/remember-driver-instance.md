# Driver is recreated on every recomposition

**Priority:** medium — currently works by accident
**Area:** `ui/downloader/DownloadScreen.kt`, `downloader/Driver.kt`

## Problem

`DownloadScreen.kt:48` builds the driver outside `remember`:

```kotlin
val driver = when (driverType) {
    DriverType.ArchiveOfOurOwn -> ArchiveOfOurOwnDriver(context.filesDir)
    DriverType.FanFictionNet -> FanFictionNetDriver(context.filesDir)
}
```

A new `Driver` is therefore constructed on every recomposition — which includes every
download-progress state change — and `AndroidView`'s update lambda calls
`driver.installGrabber(webView)` with each new instance, reassigning the shared WebView's
`webViewClient` and completing a `ready` deferred nobody awaits. Meanwhile the
`LaunchedEffect` still holds the very first instance, which is the one doing the fetching.

It happens to work because `readChapter` re-registers the `grabber` JS interface on each
call, so the newest `WebViewClient`'s `onPageFinished` still reaches the right object. That
is a coincidence, not a design.

## Proposed fix

```kotlin
val driver = remember(driverType, context) { … }
```

While there, consider making `installGrabber` idempotent / guarding against being called
with a WebView it is already attached to.

## Acceptance criteria

- [ ] Exactly one `Driver` instance exists per `DownloadScreen` composition.
- [ ] `installGrabber` is not re-run on unrelated state changes.
- [ ] A multi-chapter AO3 download still completes end to end on a device.
