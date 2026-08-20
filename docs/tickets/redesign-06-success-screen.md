# Add the Success screen and move sharing behind an explicit button

**Priority:** high
**Area:** `app/src/main/java/eu/monniot/resync/ui/downloader/DownloadScreen.kt` (`downloadLogic`
`:144-208`, `DownloadState` `:407-430`, `DownloadScreen` `:66-133`, and a new `DownloadSuccess`
composable)
**Depends on:** [redesign-00-dependency-and-theme.md](redesign-00-dependency-and-theme.md)

## Design reference

Design project `274c396b-cc57-4eb1-8e13-4bea2287765d`, `reSync - Calm Reader v2.dc.html`, frame
**"Success"**.

Centered column, 24dp horizontal padding, no app bar and no navigation bar: a 56dp
`primaryContainer` circle holding a filled `check` icon at 28dp in `onPrimaryContainer`; 24dp gap;
`"Story ready"` at `headlineSmall`; 8dp gap; a `bodyMedium`/`onSurfaceVariant` summary line; 24dp
gap; a full-width `Button` with a leading `share` icon reading **"Share to reMarkable"**; 8dp gap;
a `TextButton` reading **"Done"**. All text centered.

## Decision (resolved — do not re-open)

**Build the Success screen as drawn.** Sharing becomes an explicit user action instead of an
automatic side effect.

This is a real behavior change: today `downloadLogic` fires the Android share sheet itself
(`DownloadScreen.kt:191-207`) and `DownloadScreen`'s `LaunchedEffect` immediately calls `onDone()`
(lines 73-75), bouncing straight back to Search — the user never sees a confirmation. After this
ticket there is one extra tap per download, and both entry points gain a visible "the file exists"
moment they don't have today.

## Proposed fix

### 1. New state

```kotlin
data class Success(
    val epubFile: File,
    val fileName: String,
    val storyName: String,
    val summary: String,
) : DownloadState
```

`summary` is the design's second line. Build it in `downloadLogic`, where `chaptersInEpub` and
`wholeStory` are both in scope (line 156):

| Case | Summary |
|---|---|
| `wholeStory` | `"$storyName saved as an EPUB, ready to send to reMarkable."` |
| one chapter | `"$storyName — chapter $n saved as an EPUB, ready to send to reMarkable."` |
| a range | `"$storyName — chapters $first–$last saved as an EPUB, ready to send to reMarkable."` |

Em dash `—` (U+2014) after the title, en dash `–` (U+2013) in the range, matching the design.
`storyName` is `chaptersInEpub.first().storyName`; the bounds are the min and max of
`chaptersInEpub.map { it.num }` (the list is sorted at line 370).

### 2. `downloadLogic` stops sharing

Keep everything through writing the file (lines 164-182: `makeEpub`, `FileName.make`,
`clearChapterCache`, `epubFile.writeBytes`). Then **delete** the `FileProvider` /
`Intent.ACTION_SEND` / `context.startActivity` block (lines 184-207) and end with:

```kotlin
setState(DownloadState.Success(epubFile, fileName, storyName, summary))
```

Update the flow comment at lines 136-143 — step 4 is no longer "build epub and upload to rm
cloud".

Moving the intent out of `downloadLogic` also shrinks its `Context` dependency, which `CLAUDE.md`
flags as what makes this function untestable. Don't chase that further here, but don't reintroduce
it either.

### 3. `DownloadScreen` stops auto-exiting

Remove the `onDone()` call at `DownloadScreen.kt:73-75` and its comment. Reaching `Success` is now
the end of the flow; leaving is the user's tap on "Done".

Add the branch to the `when (state)` at lines 108-132:

```kotlin
is DownloadState.Success -> DownloadSuccess(
    summary = state.summary,
    onShare = { shareEpub(context, state.epubFile, state.fileName) },
    onDone = onDone,
)
```

### 4. `shareEpub`

A private top-level function in this file holding the code deleted in step 2 verbatim — the
`FileProvider.getUriForFile` call with the `eu.monniot.resync.fileprovider` authority, the
`ACTION_SEND` intent with its `ClipData`, `EXTRA_STREAM`, `EXTRA_SUBJECT`, `EXTRA_TEXT` and
`FLAG_GRANT_READ_URI_PERMISSION`, and `context.startActivity(Intent.createChooser(...))`. Do not
rewrite it; the flags and the `ClipData`/`EXTRA_STREAM` duplication are load-bearing for the
reMarkable app (see the comment at line 199).

### 5. `DownloadSuccess` composable

```kotlin
@Composable
fun DownloadSuccess(summary: String, onShare: () -> Unit, onDone: () -> Unit)
```

The 56dp circle is a `Box` with `Modifier.size(56.dp).clip(CircleShape).background(
MaterialTheme.colorScheme.primaryContainer)` and a centered 28dp `Icon`. The scale-in entrance
animation belongs to
[redesign-12-motion-and-animation.md](redesign-12-motion-and-animation.md) — render it statically
here.

`check` and `share` icons come from
[redesign-11-material-symbols-icons.md](redesign-11-material-symbols-icons.md); `Icons.Rounded.Check`
is already in `material-icons-core`, so only `share` may need to wait. If it does, ship the button
text-only with a `TODO` naming ticket 11 rather than substituting an unrelated glyph.

## Acceptance criteria

- [ ] `DownloadState` has a `Success` case, and `DownloadScreen`'s `when (state)` handles it (the
      `when` is exhaustive over the sealed interface, so this is compiler-enforced).
- [ ] `Intent.ACTION_SEND`, `FileProvider` and `startActivity` appear in exactly one place in
      `DownloadScreen.kt` — inside `shareEpub` — and `downloadLogic` no longer references any of
      them.
- [ ] `downloadLogic` ends by calling `setState(DownloadState.Success(...))`.
- [ ] `DownloadScreen`'s `LaunchedEffect` no longer calls `onDone()`; the only `onDone()` call
      reachable from a successful download is the "Done" `TextButton`.
- [ ] The summary line renders all three cases correctly. Verify with three previews (whole story,
      single chapter, range) rather than on-device, since the state is trivially constructible.
- [ ] Manual verification on a device or emulator, recorded in the PR description: complete a
      download, confirm the share sheet does **not** open by itself, tap "Share to reMarkable" and
      confirm it opens with the epub attached, then tap "Done" and confirm the app returns to
      Search.
- [ ] The same flow through `DeepLinkActivity` ends with "Done" calling `finish()` — i.e. the
      deep-linked activity closes rather than showing the Search screen.
- [ ] `DownloadSuccess` uses only `androidx.compose.material3` components.
- [ ] `./gradlew assembleDebug` and `./gradlew test` pass.
