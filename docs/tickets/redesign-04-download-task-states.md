# Rebuild Downloading/Success/Error as full-screen M3 task states

**Priority:** high
**Area:** `app/src/main/java/eu/monniot/resync/ui/downloader/DownloadScreen.kt`
(`FetchingFirstChapterView`, `DownloadingRemainingChapters`, `DisplayDownloadError`, and a new
Success composable)
**Depends on:** [redesign-00-dependency-and-theme.md](redesign-00-dependency-and-theme.md)

## Design reference

Design project `274c396b-cc57-4eb1-8e13-4bea2287765d`, `reSync - Calm Reader v2.dc.html`, frames
"Downloading", "Success", "Error", "Error — technical details expanded".

## Problem / scope note — read before implementing

This ticket has **two open product decisions**, not just restyling. Resolve them (with the user/
maintainer if unclear) before writing code:

1. **The mock's Downloading screen has a close icon in its app bar; the app has no cancel path.**
   `downloadLogic`/`selectChaptersToDownload` (`DownloadScreen.kt:144-373`) run to completion with
   no cancellation hook — the only way out today is backgrounding the app (which
   `KeepScreenOn`/the WebView-driven fetch actively discourages, per `DownloadScreen.kt:60-64`).
   Wiring the close icon to something real means threading a cancel signal through the suspend
   chain (structured-concurrency cancellation of the `LaunchedEffect` at `DownloadScreen.kt:66-81`
   would propagate automatically if the icon triggers it, but the half-downloaded epub/cache
   state on cancel needs a decision: discard the partial `chaptersInEpub` and cached chapter
   files, or leave them for a future resume). Don't wire a close icon that silently does nothing.

2. **There is no "Success" state today.** `DownloadState` (`DownloadScreen.kt:407-430`) has only
   `FetchingFirstChapter`, `ConfirmChapters`, `DownloadingRemainingChapters`, `Error` — no
   `Success`. Today `downloadLogic` **automatically** fires the Android share intent
   (`DownloadScreen.kt:191-207`, `context.startActivity(Intent.createChooser(...))`) and then
   `DownloadScreen`'s `LaunchedEffect` immediately calls `onDone()`
   (`DownloadScreen.kt:73-75`), bouncing straight back to the search screen — the user never sees
   a dedicated confirmation screen. The mock's Success screen has an explicit **"Share to
   reMarkable"** button instead of an automatic share-sheet trigger, plus a separate "Done"
   button. Implementing this mock as drawn means changing `downloadLogic` to *stop* auto-sharing,
   add a `DownloadState.Success(epubFile: File, fileName: String)` variant, and move the
   `Intent.ACTION_SEND` construction (lines 185-207) into the "Share to reMarkable" button's
   `onClick`. That's a real behavior change users will notice (one extra tap after every
   download) — confirm it's actually wanted, not just visually implied by the mock, before
   building it.

## Proposed fix (once the above is resolved)

1. **Downloading — full-screen, no nav bar.** `FetchingFirstChapterView` and
   `DownloadingRemainingChapters` are currently centered `Column`s with no app bar. Add a header
   row: `IconButton(close)` + story title (`onSurface`, 22/28), then center the progress content
   below. `DownloadScreen`'s top-level composable (`DownloadScreen.kt:83-133`) already renders
   states unconditionally inside a `Box` with no `NavigationBar` — confirm it stays nav-bar-less
   for these states (it already is, since `LauncherScreen` only shows `SearchStoryScreen`,
   `DownloadScreen` is reached *through* that with no nav bar of its own — verify this holds after
   the M3 nav bar migration in
   [redesign-01-navigation-bar.md](redesign-01-navigation-bar.md) doesn't accidentally wrap it).

2. **Determinate progress ring.** `DownloadingRemainingChapters` already uses
   `CircularProgressIndicator(progress = currentlyDownloading.toFloat() / totalToDownloads, ...)`
   (`DownloadScreen.kt:671-675`) — migrate to the M3 `CircularProgressIndicator` overload (lambda
   `progress = { ... }`, not a direct `Float` param — the M2 direct-value overload is deprecated in
   M3) and center a `"$currentlyDownloading/$totalToDownloads chapters"` label inside the ring
   (already done at lines 676-680, keep it). `FetchingFirstChapterView`'s indicator
   (`DownloadScreen.kt:459-464`) has no determinate progress (it's the initial single-chapter
   fetch) — keep it indeterminate (`CircularProgressIndicator()` with no `progress`), matching
   M3's default.

3. **Rate-limit notice.** The `notice` text (`DownloadScreen.kt:683-691`,
   `DownloadingRemainingChaptersPreview`/`NoticePreview` at lines 696-757) should render inside a
   `surfaceContainerHighest` rounded-12dp card below the ring, matching the mock's notice box —
   currently it's bare centered text.

4. **Success screen (new).** Tonal `primaryContainer` 56dp circle with a filled `check` icon
   (`onPrimaryContainer`), `headlineSmall` "Story ready", `bodyMedium` summary line
   (`"$storyName — $chapterRange saved as an EPUB, ready to send to reMarkable."`), full-width
   `Button` "Share to reMarkable" (leading `share` icon), text button "Done" below it. Needs the
   `DownloadState.Success` variant and `downloadLogic` change described in the scope note above.

5. **Error screen.** `DisplayDownloadError` (`DownloadScreen.kt:711-742`) currently dumps
   `error.stackTraceToString()` unconditionally as visible body text — replace with the mock's
   structure: tonal `errorContainer` 56dp circle + filled `error` icon, `headlineSmall` "Something
   went wrong", a **generic** user-facing message (not the raw stack trace), full-width `Button`
   "Try again", text buttons "Close" and "Copy error details", and a collapsible "Technical
   details" section (same `AnimatedVisibility`-expand pattern as
   [redesign-03-confirm-chapters-screen.md](redesign-03-confirm-chapters-screen.md)'s chapter
   picker) that reveals the monospace stack trace in a `surfaceContainerHighest` card only when
   expanded. This needs new plumbing:
   - "Try again" has no equivalent today — `DownloadState.Error` (`DownloadScreen.kt:429`) only
     carries the `Throwable`; retrying means re-invoking `downloadLogic` from
     `DownloadScreen.kt:71` with the same `storyId`/`chapterId`/`driverType`, which the composable
     already has in scope (it's the caller of `downloadLogic`) — wire the button to re-run the
     `LaunchedEffect` body, e.g. by keying a retry counter into `LaunchedEffect`'s keys.
   - "Copy error details" needs `ClipboardManager` (`LocalClipboardManager.current` /
     `LocalContext.current.getSystemService(ClipboardManager::class.java)`) to copy a formatted
     block: provider, story id, chapter, and the stack trace (the mock's example dump format is a
     good template — `Provider`, `Story ID`, `Chapter`, blank line, exception line, then indented
     `at ...` frames).
   - "Close" maps to the existing `onDone`-style unwind back to search (same decision as the
     Downloading close icon above — make sure both use the same mechanism).

## Acceptance criteria

- [ ] Cancellation behavior for the Downloading close icon is implemented and documented (not a
      no-op `IconButton`), or explicitly deferred with a `TODO` referencing this ticket if scoped
      out.
- [ ] `DownloadState` gains a `Success` case; `downloadLogic` no longer auto-fires the share
      intent — sharing happens from the Success screen's button — **or** this change is explicitly
      rejected/deferred with a one-line rationale recorded in this ticket before implementation
      starts.
- [ ] Error screen shows a generic message by default with stack trace behind "Technical details";
      "Try again" re-runs the download without navigating away; "Copy error details" populates the
      clipboard.
- [ ] `FetchFirstPreview`, `DownloadingRemainingChaptersPreview`,
      `DownloadingRemainingChaptersNoticePreview`, `DisplayDownloadErrorPreview`
      (`DownloadScreen.kt:469-775`) all updated/added to cover the new Success state and match the
      mock states.
