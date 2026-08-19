# WebView contents debugging is enabled in release builds

**Priority:** medium — security
**Area:** `downloader/Driver.kt`

## Problem

`Driver.installGrabber` (`downloader/Driver.kt:30`) calls

```kotlin
WebView.setWebContentsDebuggingEnabled(true);
```

unconditionally. This is a process-wide, release-inclusive setting: any app with debugging
access (or a connected host running Chrome DevTools) can inspect and script the WebView that
is loading the user's fic pages and carrying their site cookies.

Also note the stray trailing semicolon.

## Proposed fix

```kotlin
if (BuildConfig.DEBUG) WebView.setWebContentsDebuggingEnabled(true)
```

`downloader/` does not currently import `BuildConfig`; if the dependency direction is
unwanted, pass the flag in from the composable that builds the driver.

## Acceptance criteria

- [ ] Release builds do not enable WebView contents debugging.
- [ ] Debug builds still do.
