# Rebuild the chapter-confirmation screen on M3 components

**Priority:** high
**Area:** `app/src/main/java/eu/monniot/resync/ui/downloader/DownloadScreen.kt` (`ConfirmChapters`,
`DownloadScreen.kt:483-596`)
**Depends on:** [redesign-00-dependency-and-theme.md](redesign-00-dependency-and-theme.md)

## Design reference

Design project `274c396b-cc57-4eb1-8e13-4bea2287765d`, `reSync - Calm Reader v2.dc.html`, frames
"Confirm chapters" (collapsed), "Confirm — range expanded", "Confirm — single chapter".

## Problem / scope note

`ConfirmChapters` today is a bare `Column` with no app bar at all — there's no back/cancel
affordance rendered anywhere in the current chapter-confirmation flow. The mock adds a back arrow
+ story title in a small top app bar. **Before restyling, decide what the back arrow does**: the
app doesn't use Navigation-Compose (see
[redesign-08-motion-and-animation.md](redesign-08-motion-and-animation.md) for why that matters),
and `selectChaptersToDownload` (`DownloadScreen.kt:221-373`) has no cancellation path — it's
purely driven forward by `onUserConfirmation`. Wiring a real back action means either (a) adding a
cancellation callback that unwinds back to `SearchStoryScreen`'s `storySelected = false`, or (b)
rendering the arrow non-functional for now. Pick one explicitly rather than leaving it a dead
`IconButton`.

## Proposed fix

1. **Header.** Add a small top-app-bar-style header: `IconButton(arrow_back)` + row, then the
   story title (`storyName`) below at `headlineSmall` (matches mock's 24/32 style), on a
   `surfaceContainer` background — this is a static header, not `SmallTopAppBar`'s scroll-behavior
   machinery (nothing here scrolls), so a plain `Row`/`Column` with the right colors/typography is
   enough; don't reach for `TopAppBar` unless a scroll-collapse behavior is actually wanted.

2. **Byline + chips.** Replace the current `Text("By") + Text(authorName)` /
   `Text("$totalChapters chapters")` / `Text("From: ...")` block (`DownloadScreen.kt:500-515`)
   with: byline text, then a `Row` of two `AssistChip`s (outlined, `8.dp` corner, `onSurface`
   label) reading `"$totalChapters chapters"` and the provider's display name (`websiteName()`).
   One-shot stories (`totalChapters == 1`, currently rendered as `"One Shot"` text at line 513)
   aren't shown in the mock at all — the mock's confirm screen only appears for multi-chapter
   stories per `SearchStoryScreen`'s existing flow (`DownloadScreen` skips straight past
   `ConfirmChapters` when `initialChapter.totalChapters == 1`, see
   `DownloadScreen.kt:232-234`) — keep that behavior, just note the "One Shot" label has no mock
   equivalent to match against.

3. **Primary action.** "Download entire story" as a full-width pill `Button` (replaces the
   `Surface(elevation=1.dp) { Button(...) }` wrapper at lines 531-542 — drop the `Surface`
   entirely, M3 buttons don't need an elevated wrapper).

4. **Divider.** Replace the plain `Text("OR")` (line 544) with the mock's `Row` of two
   `HorizontalDivider`s flanking a small `"OR"` label — purely cosmetic, keep the `"OR"` copy.

5. **Chapter picker card.** This is the biggest structural change. The mock's "Choose specific
   chapters" section is a `surfaceContainerHighest` rounded-12dp card with a header row (label +
   `expand_more`/`expand_less` chevron) that **expands/collapses** — collapsed by default, showing
   only the header; expanded reveals the range control and a secondary `Button`
   ("Download chapters 12–42" / "Download chapter 24"). Today's `Surface(elevation=1.dp)` block
   (`DownloadScreen.kt:546-593`) is always expanded, with no collapse state and no header/chevron
   at all. Add an `expanded` boolean `remember { mutableStateOf(false) }`, gate the slider content
   in `AnimatedVisibility` (animation details belong to
   [redesign-08-motion-and-animation.md](redesign-08-motion-and-animation.md); this ticket can
   ship with an instant show/hide and let that ticket add the expand/collapse motion + chevron
   rotation).

6. **Single control instead of two sliders.** Today there are two stacked M2 `Slider`s — "From"
   (`chapterStart`, lines 574-581) and "To" (`chapterEnd`, lines 584-591) — each its own labelled
   row. The mock renders **one** two-thumb range track (M3 `RangeSlider`) with the numeric bounds
   (`1` / `totalChapters`) as small labels underneath, and the secondary button's label already
   reflects the current selection. Replace both `Slider`s with one
   `RangeSlider(value = chapterStart.toFloat()..chapterEnd.toFloat(), onValueChange = { range ->
   chapterStart = range.start.toInt(); chapterEnd = range.endInclusive.toInt() }, valueRange = 1f..totalChapters.toFloat())`.
   Note this changes the existing constraint that "To" can't go below "From"
   (`DownloadScreen.kt:589`, `chapterStart.toFloat()..totalChapters.toFloat()`) — `RangeSlider`
   enforces `start <= endInclusive` itself, so that clamp becomes redundant, but double check the
   single-chapter case (`chapterStart == chapterEnd`, both thumbs coincide) still produces
   `ChapterSelection.One` via the existing branch at `DownloadScreen.kt:559-563` rather than a
   degenerate `Range(n, n)`.

7. **Secondary button.** "Download chapters X–Y" / "Download chapter X" as a `secondaryContainer`
   `FilledTonalButton`, matching the mock's `onSecondaryContainer`-on-`secondaryContainer` styling
   (distinct from the primary pill button's `primary`/`onPrimary`).

## Acceptance criteria

- [ ] `ConfirmChapters` renders a header with story title; back-arrow behavior is either wired to
      a real cancel path or explicitly left inert with a one-line comment saying so — not silently
      dead.
- [ ] Chapter count and provider are shown as `AssistChip`s, not plain text.
- [ ] "Choose specific chapters" starts collapsed and toggles via the chevron.
- [ ] Chapter range selection uses a single `RangeSlider`, not two independent `Slider`s; dragging
      either thumb still can't cross the other.
- [ ] `chapterStart == chapterEnd` still yields `ChapterSelection.One`, verified via
      `ConfirmChaptersLightPreview`/`ConfirmChaptersDarkPreview` (`DownloadScreen.kt:599-635`) and
      manual interaction.
- [ ] Secondary CTA is `FilledTonalButton` (secondaryContainer), primary CTA is `Button` (primary).
