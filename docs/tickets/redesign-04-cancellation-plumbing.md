# Add a cancellation path to the download flow

**Priority:** high (unblocks the back arrow in
[redesign-03](redesign-03-confirm-chapters-screen.md) and the close icon in
[redesign-05](redesign-05-downloading-screen.md))
**Area:** `app/src/main/java/eu/monniot/resync/ui/downloader/DownloadScreen.kt`
**Depends on:** nothing (behavior only — no M3 components, can land before or after
[redesign-00](redesign-00-dependency-and-theme.md))

## Design reference

Design project `274c396b-cc57-4eb1-8e13-4bea2287765d`, `reSync - Calm Reader v2.dc.html`. Two
frames show an exit affordance the app cannot currently honour: **"Confirm chapters"** (back arrow
in the header) and **"Downloading"** (close icon in the app bar).

This ticket adds no UI. It exists so those two tickets have a real callback to wire, instead of
shipping dead controls.

## Decision (resolved — do not re-open)

**Cancel unwinds the download and discards partial work.** Cancelling clears the story's cached
chapter HTML and returns to the Search screen. The alternative — keeping the cache so a re-run
resumes — was considered and rejected: nothing would ever clean that cache up if the user never
retries, and the resume-across-rate-limits behavior it mirrors is scoped to a *live* download, not
an abandoned one.

## Problem

`downloadLogic` and `selectChaptersToDownload` (`DownloadScreen.kt:144-373`) run to completion.
`selectChaptersToDownload` is driven forward only by `onUserConfirmation` completing a
`CompletableDeferred` (line 236-250); there is no path that unwinds it. The only way out today is
backgrounding the app — which `KeepScreenOn` and the WebView-driven fetch actively discourage
(`DownloadScreen.kt:60-64`).

## Proposed fix

### 1. Make the download job cancellable

`DownloadScreen`'s `LaunchedEffect` (`DownloadScreen.kt:66-81`) already runs the whole flow in a
coroutine. Capture its `Job` so a callback can cancel it:

```kotlin
var downloadJob by remember { mutableStateOf<Job?>(null) }

LaunchedEffect(key1 = storyId, key2 = chapterId) {
    downloadJob = coroutineContext[Job]
    try {
        driver.ready()
        downloadLogic(context, storyId, chapterId, driverType, driver, setState)
        onDone()
    } catch (e: CancellationException) {
        throw e            // see step 2
    } catch (e: Throwable) {
        Log.e(TAG, "Error caught when downloading story", e)
        setState(DownloadState.Error(e))
    }
}
```

Structured concurrency does the rest: cancelling that job propagates into every `suspend` call in
the chain, including `CompletableDeferred.await()` (line 250), the `delay()` calls in the
rate-limit loop (`readWithRateLimit`, line 387) and the inter-chapter pacing delays (lines 320,
354). No cancellation flag needs threading through `selectChaptersToDownload`.

### 2. Do not swallow `CancellationException`

The existing handler is `catch (e: Throwable)` (line 76), which **also catches
`CancellationException`** — so without the explicit rethrow above, cancelling a download would
render the Error screen instead of exiting. This is the single most likely way to get this ticket
subtly wrong.

While here, replace the `println` + `e.printStackTrace()` at lines 77-78 with the `Log.e` call
shown above; the file already has a `TAG` (line 33) and imports `android.util.Log`.

### 3. Cancel callback

Add to `DownloadScreen`:

```kotlin
val onCancel: () -> Unit = {
    downloadJob?.cancel()
    clearChapterCache(driver.storyCacheDir(storyId))
    onDone()
}
```

`clearChapterCache` / `storyCacheDir` are the same pair `downloadLogic` already uses on the success
path (line 173).

Pass `onCancel` into the two composables that need it:

- `ConfirmChapters(…, onCancel = onCancel)` — `DownloadScreen.kt:113-120`
- `DownloadingRemainingChapters(…, onCancel = onCancel)` — `DownloadScreen.kt:121-125`

Both gain an `onCancel: () -> Unit` parameter. They do not have to *render* it in this ticket —
[redesign-03](redesign-03-confirm-chapters-screen.md) and
[redesign-05](redesign-05-downloading-screen.md) add the arrow and close icon. If those tickets
land first, they wire to this parameter; if this one lands first, the parameter sits unused for a
commit or two, which is fine and expected.

### 4. `onDone` semantics

`onDone` already means "leave this screen": `SearchStoryScreen.kt:44` sets
`storySelected.value = false`, and `DeepLinkActivity.kt:29` calls `finish()`. Cancelling reuses it
unchanged — both entry points behave sensibly (back to the form; close the deep-linked activity).

## Acceptance criteria

- [ ] `DownloadScreen`'s `LaunchedEffect` rethrows `CancellationException` before the general
      `catch (e: Throwable)`; cancelling never produces `DownloadState.Error`.
- [ ] `DownloadScreen` exposes an `onCancel` lambda that cancels the download job, calls
      `clearChapterCache(driver.storyCacheDir(storyId))`, and calls `onDone()`.
- [ ] `ConfirmChapters` and `DownloadingRemainingChapters` both take an `onCancel: () -> Unit`
      parameter.
- [ ] `println(...)` and `e.printStackTrace()` no longer appear in `DownloadScreen.kt`; the error
      path logs through `Log.e(TAG, …)`.
- [ ] Manual verification on a device or emulator, recorded in the PR description: start a
      multi-chapter AO3 download, trigger `onCancel` (temporarily from a debug button if
      tickets 03/05 haven't landed), and confirm all three of — the app returns to Search; the
      Error screen does **not** appear; `filesDir/ao3/<storyId>/` is gone afterwards.
- [ ] `./gradlew assembleDebug` and `./gradlew test` pass. In particular the existing
      `selectChaptersToDownload` unit tests still pass unchanged — this ticket must not alter its
      signature.
