# Rebuild the chapter-confirmation screen on M3 components

**Priority:** high
**Area:** `app/src/main/java/eu/monniot/resync/ui/downloader/DownloadScreen.kt` (`ConfirmChapters`,
`DownloadScreen.kt:483-596`)
**Depends on:** [redesign-00-dependency-and-theme.md](redesign-00-dependency-and-theme.md),
[redesign-04-cancellation-plumbing.md](redesign-04-cancellation-plumbing.md) (supplies the back
arrow's `onCancel`)

## Design reference

Design project `274c396b-cc57-4eb1-8e13-4bea2287765d`, `reSync - Calm Reader v2.dc.html`, three
frames: **"Confirm chapters"** (collapsed), **"Confirm — range expanded"**, **"Confirm — single
chapter"**.

Read all three before starting — the collapsed and expanded states differ by more than a
disclosure toggle (see step 5).

## Problem

`ConfirmChapters` today is a bare `Column`: story title at `h3`, a "By {author}" row, plain
`Text` for the chapter count and provider, an elevated `Surface` wrapping a "Synchronise entire
story" button, a plain `Text("OR")`, and a second elevated `Surface` holding a "Synchronise
specific chapters" button above two stacked M2 `Slider`s. There is no app bar and no cancel
affordance anywhere in the flow.

Copy changes in this ticket (the design uses "Download", the code says "Synchronise"):

| Today | Design |
|---|---|
| `"Synchronise entire story"` | `"Download entire story"` |
| `"Synchronise specific chapters (12 - 42)"` | `"Download chapters 12–42"` (en dash, no space) |
| `"Synchronise specific chapters (24)"` | `"Download chapter 24"` |

## Proposed fix

### 1. Header

A `surfaceContainer` block containing a 64dp row with a 48dp `IconButton(arrow_back)` at 4dp start
padding, then the story title below it at `headlineSmall` (24/32, `onSurface`) with 16dp
horizontal and 16dp bottom padding.

This is a static header — nothing on this screen scrolls-to-collapse — so build it as a
`Column`/`Row` with the right colors and typography rather than reaching for `TopAppBar` and its
`scrollBehavior` machinery.

The back arrow calls the `onCancel` callback added by
[redesign-04-cancellation-plumbing.md](redesign-04-cancellation-plumbing.md); `ConfirmChapters`
gains an `onCancel: () -> Unit` parameter and `DownloadScreen.kt:113-120` passes it through. If
ticket 04 has not landed yet, **do not ship a dead `IconButton`** — either land ticket 04 first, or
omit the arrow entirely and leave a `TODO` naming it.

### 2. Byline and chips

Replace the `Text("By") + Text(authorName)` row and the two plain `Text`s
(`DownloadScreen.kt:500-515`) with:

- `"by $authorName"` at `bodyMedium`/`onSurfaceVariant` (note: lowercase "by", one string, no
  italics — the design drops today's `FontStyle.Italic`).
- 16dp gap, then a `Row(horizontalArrangement = Arrangement.spacedBy(8.dp))` of two chips:
  `"$totalChapters chapters"` and `driverType.websiteName()`. The design draws them 32dp tall,
  8dp radius, 1dp `outlineVariant` border, `labelLarge`/`onSurface` label.

Use `AssistChip(onClick = { }, label = { … })` — its defaults already give the 8dp shape, 32dp
height and outline. These chips are **display-only** in the design; `AssistChip` requires an
`onClick`, so pass an empty lambda and add `enabled = true` (a disabled chip would grey the label).
Comment the empty lambda so it doesn't read as an unfinished TODO.

Alignment: the design left-aligns everything in the content column. Today the whole screen is
`horizontalAlignment = Alignment.CenterHorizontally` (line 496) — change it to the default
(`Alignment.Start`).

### 3. One-shot stories

`totalChapters == 1` renders as `"One Shot"` today (line 513) and has no design equivalent —
because this screen is unreachable for one-shots: `selectChaptersToDownload` skips
`ConfirmChapters` entirely when `initialChapter.totalChapters == 1`
(`DownloadScreen.kt:232-234`). Keep that behavior. Drop the `"One Shot"` branch and render the
chip unconditionally as `"$totalChapters chapters"`.

### 4. Primary action

`"Download entire story"` as a full-width M3 `Button`. Delete the `Surface(elevation = 1.dp)`
wrapper at lines 531-542 — M3 buttons carry their own container and need no elevated wrapper.

### 5. The disclosure — collapsed and expanded are different components

This is the part the design is easy to misread. **Collapsed** ("Confirm chapters" frame): there is
**no card and no "OR" divider**. Under the primary button, separated by 8dp, sits a bare
text-button row — `"Choose specific chapters"` at `labelLarge`/**`primary`** with an
`expand_more` chevron, also `primary`, on a transparent 40dp pill.

**Expanded** (both other frames): the primary button is followed by a 16dp-margin `Row` of two
`HorizontalDivider`s flanking an `"OR"` label (`labelSmall`, `onSurfaceVariant`), and *then* a
`surfaceContainerHighest` 12dp card with 16dp padding containing:

1. a header row — `"Choose specific chapters"` at `titleMedium`/`onSurface` on the left, an
   `expand_less` chevron in `primary` on the right;
2. the range control (step 6);
3. the `1` / `totalChapters` bound labels (`bodySmall`, `onSurfaceVariant`), 12dp below;
4. the secondary button (step 7).

So the disclosure swaps between a `TextButton` and a `Card`, and the "OR" divider appears only in
the expanded state. Implement it as:

```kotlin
var expanded by remember { mutableStateOf(false) }
if (!expanded) {
    TextButton(onClick = { expanded = true }, Modifier.fillMaxWidth()) { … }
} else {
    OrDivider()
    Card(…) { … }
}
```

Ship this ticket with an instant show/hide.
[redesign-12-motion-and-animation.md](redesign-12-motion-and-animation.md) adds the
`AnimatedVisibility` expansion and the chevron rotation; do not build those here.

### 6. One `RangeSlider` instead of two `Slider`s

Today: two stacked M2 `Slider`s, "From" (`chapterStart`, lines 574-581) and "To" (`chapterEnd`,
lines 584-591), each in its own labelled row. The design shows **one** two-thumb track.

```kotlin
RangeSlider(
    value = chapterStart.toFloat()..chapterEnd.toFloat(),
    onValueChange = { range ->
        chapterStart = range.start.roundToInt()
        chapterEnd = range.endInclusive.roundToInt()
    },
    valueRange = 1f..totalChapters.toFloat(),
    steps = (totalChapters - 2).coerceAtLeast(0),
)
```

**The `steps` argument is required, not optional.** Without it the slider is continuous, and
`chapterStart == chapterEnd` — the only path to `ChapterSelection.One` (`DownloadScreen.kt:559-563`)
— becomes practically unreachable by dragging, silently turning every single-chapter selection into
a degenerate `Range(n, n)`. `steps` counts the values *between* the endpoints, hence
`totalChapters - 2`.

`RangeSlider` enforces `start <= endInclusive` itself, which makes today's
`valueRange = chapterStart.toFloat()..totalChapters.toFloat()` clamp on the "To" slider (line 589)
redundant — drop it.

Keep the existing initial values: `chapterStart = initialChapterNumber`,
`chapterEnd = totalChapters` (lines 554-555).

The design's "Confirm — single chapter" frame is drawn as a *single*-thumb slider. That is
shorthand for two coincident thumbs, not a second control — there is one `RangeSlider` in both
states.

Below the track, a `Row(Arrangement.SpaceBetween)` with `"1"` and `"$totalChapters"` at
`bodySmall`/`onSurfaceVariant`. These are the track bounds, not the current selection — the
selection is reflected in the button label.

### 7. Secondary action

`FilledTonalButton` (which defaults to `secondaryContainer`/`onSecondaryContainer`, distinct from
the primary button's `primary`/`onPrimary`), full width, label:

```kotlin
if (chapterStart == chapterEnd) "Download chapter $chapterStart"
else "Download chapters $chapterStart–$chapterEnd"   // U+2013 en dash
```

Its `onClick` keeps today's branch (lines 557-565) unchanged: `ChapterSelection.One(chapterStart)`
when the bounds are equal, `ChapterSelection.Range(chapterStart, chapterEnd)` otherwise.

## Acceptance criteria

- [ ] `DownloadScreen.kt`'s `ConfirmChapters` uses only `androidx.compose.material3` components
      (`Button`, `FilledTonalButton`, `TextButton`, `Card`, `AssistChip`, `RangeSlider`,
      `HorizontalDivider`, `IconButton`); no `Surface(elevation = …)` remains in it.
- [ ] The back arrow invokes a real `onCancel` that returns to Search — or it is absent, with a
      `TODO` naming ticket 04. A rendered `IconButton` with an empty `onClick` fails this
      criterion.
- [ ] Chapter count and provider render as two `AssistChip`s; `"One Shot"` no longer appears in the
      file.
- [ ] Collapsed state shows a `TextButton` with `primary`-colored label and chevron, **no** card
      and **no** "OR" divider. Expanding reveals the "OR" divider *and* the
      `surfaceContainerHighest` card. Both states are covered by previews.
- [ ] Exactly one `RangeSlider` and zero `Slider`s appear in `ConfirmChapters`; the `RangeSlider`
      passes a non-null `steps` argument.
- [ ] Dragging the thumbs together and tapping the secondary button produces
      `ChapterSelection.One`, verified by temporarily logging the value passed to
      `onUserConfirmation` on a device or emulator. Record the observed value in the PR
      description.
- [ ] Button labels read exactly `"Download entire story"`, `"Download chapters X–Y"` (en dash) and
      `"Download chapter X"`; `"Synchronise"` no longer appears anywhere in `DownloadScreen.kt`.
- [ ] `ConfirmChaptersLightPreview` / `ConfirmChaptersDarkPreview` (`DownloadScreen.kt:599-635`)
      render without error, and a third preview covers the expanded state.
