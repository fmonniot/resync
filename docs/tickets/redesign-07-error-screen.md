# Rebuild the Error screen with a generic message, retry and copyable details

**Priority:** high
**Area:** `app/src/main/java/eu/monniot/resync/ui/downloader/DownloadScreen.kt`
(`DisplayDownloadError` `:711-742`, `DownloadScreen` `:66-133`)
**Depends on:** [redesign-00-dependency-and-theme.md](redesign-00-dependency-and-theme.md)

## Design reference

Design project `274c396b-cc57-4eb1-8e13-4bea2287765d`, `reSync - Calm Reader v2.dc.html`, frames
**"Error"** and **"Error — technical details expanded"**.

Centered column, 24dp horizontal padding, no app bar and no navigation bar: a 56dp `errorContainer`
circle holding a filled `error` icon at 28dp in `onErrorContainer`; 24dp gap; `"Something went
wrong"` at `headlineSmall`; 8dp gap; a generic `bodyMedium`/`onSurfaceVariant` message; 24dp gap; a
full-width `Button` **"Try again"**; 8dp gap; three `TextButton`s stacked — **"Close"**, **"Copy
error details"**, and **"Technical details"** with an `expand_more`/`expand_less` chevron.

When expanded, a full-width `surfaceContainerHighest` 12dp card appears 8dp below, **left-aligned**
(the rest of the screen is centered), containing monospace 12px/1.7 `onSurfaceVariant` text. The
expanded frame also scrolls and switches from centered to top-aligned with 32dp top padding, since
the content no longer fits.

## Problem

`DisplayDownloadError` dumps `error.stackTraceToString()` as visible body text (line 740), above it
`"$storyId; $chapterId; DriverType($driverType)"` (line 735) — which renders the value classes'
`toString`, e.g. `StoryId(id=27855042)`. There is no retry, no copy, and no way to dismiss: the
comment at `DownloadScreen.kt:73-74` notes the error path deliberately skips `onDone()` so "the
user can choose when to close the app", but nothing in the UI lets them.

## Proposed fix

### 1. Retry

`DownloadState.Error` (line 429) carries only the `Throwable`. Retrying means re-running the
`LaunchedEffect` body with the same `storyId`/`chapterId`/`driverType`, which `DownloadScreen`
already has in scope — key an attempt counter into the effect:

```kotlin
var attempt by remember { mutableIntStateOf(0) }

LaunchedEffect(storyId, chapterId, attempt) { … }
```

and `onRetry = { setState(DownloadState.FetchingFirstChapter(storyId, chapterId)); attempt++ }`.

**Resetting the state is not optional.** `downloadLogic` does not set a state before its first
`driver.readChapter` call (line 154 — see the comment at 153), so without the reset the Error
screen stays on screen for the whole retry and the user gets no feedback that anything happened.

If [redesign-04-cancellation-plumbing.md](redesign-04-cancellation-plumbing.md) has landed, this is
a third key on the same `LaunchedEffect` — the `downloadJob` capture inside it keeps working
unchanged.

### 2. Copy error details

Format, matching the design:

```
Provider  Archive of our Own
Story ID  39200706
Chapter   24

<error.stackTraceToString()>
```

Use `driverType.websiteName()`, `storyId.id` and `chapterId.id` — not the value classes'
`toString`. `chapterId.id` is nullable; render `"—"` when null.

For the clipboard, prefer `androidx.compose.ui.platform.LocalClipboard` — `LocalClipboardManager`
is deprecated as of Compose UI 1.8, and this project resolves Compose UI 1.12.0 via the BOM.
`Clipboard.setClipEntry` is a `suspend` function, so it needs a `rememberCoroutineScope()`:

```kotlin
val clipboard = LocalClipboard.current
val scope = rememberCoroutineScope()
// onCopy = { scope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("reSync error", details))) } }
```

Check the exact `ClipEntry` construction against the resolved version before assuming this
compiles; if the API differs, use whatever the non-deprecated equivalent is rather than falling
back to `LocalClipboardManager`.

Android shows its own copy confirmation from API 33 onward, but `minSdkVersion` here is 30. Show a
`Toast` only below 33, so the button is never silent:

```kotlin
if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
    Toast.makeText(context, "Error details copied", Toast.LENGTH_SHORT).show()
}
```

### 3. Technical details disclosure

`var expanded by remember { mutableStateOf(false) }`, chevron swapping
`expand_more`/`expand_less`, revealing the monospace card. Use `FontFamily.Monospace` and
`bodySmall` with `lineHeight = 20.sp`.

Ship with an instant show/hide; the `AnimatedVisibility` expansion and chevron rotation are
[redesign-12-motion-and-animation.md](redesign-12-motion-and-animation.md).

Because the expanded state overflows the screen, keep the existing `verticalScroll(state)`
(line 718, 726) and switch the column from `Arrangement.Center` to `Arrangement.Top` with 32dp top
padding when `expanded` — a centered column inside a scroll container clips its top otherwise.

### 4. Close

Wire the "Close" `TextButton` to `onDone`. `DisplayDownloadError` gains `onRetry: () -> Unit` and
`onDone: () -> Unit` parameters; `DownloadScreen.kt:126-131` passes both.

If [redesign-04](redesign-04-cancellation-plumbing.md) has landed, "Close" uses its `onCancel`
rather than `onDone` directly, so a failed download cleans up its cache the same way a cancelled
one does. Use `onCancel` when it exists.

### 5. Generic message

The design's copy is `"We couldn't finish reading this chapter. This is usually temporary — try
again in a moment."` Use it verbatim for every error — do **not** try to classify exceptions into
different messages. The raw exception stays behind "Technical details".

## Acceptance criteria

- [ ] `error.stackTraceToString()` is not rendered unless "Technical details" is expanded; the
      default view shows only the design's generic sentence.
- [ ] "Try again" resets the state to `FetchingFirstChapter` and re-runs the download without
      leaving the screen — verified on a device or emulator by forcing a failure (e.g. airplane
      mode), tapping Try again with connectivity restored, and confirming the download completes.
      Record this in the PR description.
- [ ] "Copy error details" puts the four-part block (provider, story id, chapter, blank line, stack
      trace) on the clipboard, using `LocalClipboard` rather than the deprecated
      `LocalClipboardManager`. Verified by pasting into another app.
- [ ] `assembleDebug` produces no deprecation warning from the clipboard call.
- [ ] "Close" returns to Search (or closes the deep-linked activity), and uses ticket 04's
      `onCancel` if that ticket has landed.
- [ ] The chevron reflects the disclosure state, and the expanded card is `surfaceContainerHighest`
      with 12dp corners, left-aligned monospace text.
- [ ] Neither `StoryId(id=` nor `ChapterId(id=` can appear in any user-visible string.
- [ ] `DisplayDownloadError` uses only `androidx.compose.material3` components.
- [ ] `DisplayDownloadErrorPreview` (`:759-775`) renders without error with the new signature; add
      a second preview with `expanded` defaulted to true so the details card is previewable, plus
      dark variants.
