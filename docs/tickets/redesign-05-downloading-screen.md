# Rebuild the Downloading screen as a full-screen M3 task state

**Priority:** high
**Area:** `app/src/main/java/eu/monniot/resync/ui/downloader/DownloadScreen.kt`
(`FetchingFirstChapterView` `:433-467`, `DownloadingRemainingChapters` `:637-694`, and
`DownloadState.DownloadingRemainingChapters` `:423-427`)
**Depends on:** [redesign-00-dependency-and-theme.md](redesign-00-dependency-and-theme.md),
[redesign-04-cancellation-plumbing.md](redesign-04-cancellation-plumbing.md) (supplies the close
icon's `onCancel`)

## Design reference

Design project `274c396b-cc57-4eb1-8e13-4bea2287765d`, `reSync - Calm Reader v2.dc.html`, frame
**"Downloading"**.

Layout, top to bottom: a 64dp app-bar row with a 48dp `close` icon button then the story title at
`titleLarge` (22/28, `onSurface`); then a vertically-and-horizontally centered column holding a
**64dp determinate progress ring**, 24dp gap, `"Fetching story"` at `titleLarge`, 4dp gap,
`"15 of 42 chapters"` at `bodyMedium`/`onSurfaceVariant`, 24dp gap, and the rate-limit notice
inside a `surfaceContainerHighest` 12dp card with 16dp padding and centered text.

There is **no navigation bar** on this frame — it is a full-screen task state.

## Problem

`DownloadingRemainingChapters` is a centered `Column` with no app bar: `"Fetching Story"` at `h6`,
a 100dp `CircularProgressIndicator` with the progress text stacked *inside* it via a `Box`
(lines 658-681), and the notice as bare centered text (lines 683-691).

Three specific gaps beyond restyling:

1. **The story title isn't available.** `DownloadState.DownloadingRemainingChapters`
   (`DownloadScreen.kt:423-427`) carries only `currentlyDownloading`, `totalToDownloads` and
   `notice`. The design's header shows the story name, so the state needs a `storyName` field.
2. **The progress label moves.** Today it is centered *inside* the ring (lines 676-680). The design
   puts it below, as a separate line under the `"Fetching story"` title.
3. **`FetchingFirstChapterView` cannot show a title.** The story name is not known until the first
   chapter is fetched and parsed — that is what this state is *doing*. The design has no frame for
   it. See step 4 for what to render instead.

## Proposed fix

### 1. Add `storyName` to the state

```kotlin
data class DownloadingRemainingChapters(
    val storyName: String,
    val currentlyDownloading: Int,
    val totalToDownloads: Int,
    val notice: String?,
) : DownloadState
```

Every construction site is inside `selectChaptersToDownload`, which has `initialChapter.storyName`
in scope. There are four: lines 276 and 281 in the `ChapterSelection.One` branch, and the
`setDlState` lambdas at 293-301 (`All`) and 336-344 (`Range`). Thread `initialChapter.storyName`
into each. Update
`app/src/test/java/eu/monniot/resync/ui/downloader/SelectChaptersToDownloadTest.kt` accordingly —
if it asserts on emitted `DownloadState`s, those assertions gain the new field.

### 2. Shared header

Both this screen and `FetchingFirstChapterView` need the same 64dp row. Factor it into one private
composable in this file:

```kotlin
@Composable
private fun TaskStateHeader(title: String?, onCancel: () -> Unit) { … }
```

`IconButton(onClick = onCancel)` with `Icons.Rounded.Close` in a 48dp box at 4dp start padding,
then the title at `titleLarge`/`onSurface` when non-null.

The close icon calls the `onCancel` from
[redesign-04-cancellation-plumbing.md](redesign-04-cancellation-plumbing.md). **Do not ship a close
icon with an empty `onClick`** — land ticket 04 first, or omit the header entirely with a `TODO`
naming it.

### 3. Downloading body

- `CircularProgressIndicator(progress = { currentlyDownloading.toFloat() / totalToDownloads }, modifier = Modifier.size(64.dp))`
  — the **lambda** overload. The direct `Float` overload the code uses today (line 672) is
  deprecated in M3. Material3 1.4.0's determinate indicator draws the track gap the design shows;
  no custom drawing needed. Sweeping the value with `animateFloatAsState` belongs to
  [redesign-12-motion-and-animation.md](redesign-12-motion-and-animation.md) — set it directly here.
- Delete the `Box` at lines 658-681 that stacks the label over the ring.
- `"Fetching story"` at `titleLarge` (sentence case — today it reads `"Fetching Story"`).
- `"$currentlyDownloading of $totalToDownloads chapters"` at `bodyMedium`/`onSurfaceVariant`
  (today: `"$currentlyDownloading/$totalToDownloads\nchapters"`).
- The `notice` block moves inside a `surfaceContainerHighest` 12dp `Card` with 16dp padding and
  `textAlign = TextAlign.Center`, `bodyMedium`/`onSurfaceVariant`. Keep the `if (notice != null)`
  guard and the string `ao3RLNotice` produces — its wording is not this ticket's business (see
  [comment-drift.md](comment-drift.md)).

The existing `TODO` at lines 665-670 about 0- vs 1-indexed progress is a real bug but **out of
scope**; leave the comment in place.

### 4. `FetchingFirstChapterView`

Same `TaskStateHeader`, with `title = null` — there is no story name yet. Below it, centered:

- `CircularProgressIndicator(modifier = Modifier.size(64.dp))` — no `progress` argument, so it
  animates indeterminately, which is M3's default and correct for a single unknown-duration fetch.
- `"Looking up story"` at `titleLarge` (sentence case; today `"Looking up Story"` at `h6`).
- `"id: ${storyId.id} · chapter: ${chapterId.id}"` at `bodyMedium`/`onSurfaceVariant`, replacing
  today's parenthesised `"(id: … | Chapter: …)"`.

Order matters: the design puts the ring *above* the text on the Downloading frame, whereas today
`FetchingFirstChapterView` puts it below (lines 443-464). Match the design — ring first.

### 5. Confirm the nav bar stays absent

`DownloadScreen`'s top-level composable (`DownloadScreen.kt:83-133`) renders states inside a bare
`Box` with no `NavigationBar`, and it is reached *through* `SearchStoryScreen`, which is itself
inside `LauncherScreen`'s `Scaffold`. Verify after
[redesign-01](redesign-01-scaffold-and-navigation.md) that the M3 `Scaffold` migration did not put
`DownloadScreen` inside the scaffold's content slot in a way that leaves the nav bar visible. If it
does, that is a bug in this ticket's scope to report, not to silently work around.

## Acceptance criteria

- [ ] `DownloadState.DownloadingRemainingChapters` has a `storyName: String` field, populated from
      `initialChapter.storyName` at all four construction sites in `selectChaptersToDownload`.
- [ ] `SelectChaptersToDownloadTest` passes (`./gradlew testDebugUnitTest --tests
      "*.SelectChaptersToDownloadTest"`).
- [ ] Both `FetchingFirstChapterView` and `DownloadingRemainingChapters` render a header with a
      `close` `IconButton` bound to a real `onCancel` — or neither does, with a `TODO` naming
      ticket 04. A rendered close icon with an empty `onClick` fails this criterion.
- [ ] The Downloading screen shows the progress ring, then `"Fetching story"`, then
      `"N of M chapters"` as three stacked elements. No text is rendered inside the ring, and no
      `Box` overlays them.
- [ ] `CircularProgressIndicator` is called with the lambda `progress = { … }` overload in
      `DownloadingRemainingChapters`, and with no `progress` argument in `FetchingFirstChapterView`.
      `./gradlew assembleDebug` produces no deprecation warning for either call.
- [ ] The rate-limit notice renders inside a `surfaceContainerHighest` 12dp card.
- [ ] Both composables use only `androidx.compose.material3` components.
- [ ] `FetchFirstPreview` (`:469-481`), `DownloadingRemainingChaptersPreview` (`:696-709`) and
      `DownloadingRemainingChaptersNoticePreview` (`:744-757`) render without error with the new
      signatures; add dark-theme variants of the two Downloading previews.
