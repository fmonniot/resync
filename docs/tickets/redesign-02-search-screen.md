# Rebuild the Search screen on M3 components

**Priority:** high
**Area:** `app/src/main/java/eu/monniot/resync/ui/launcher/SearchStoryScreen.kt`,
`app/src/main/java/eu/monniot/resync/downloader/DriverType.kt` (possibly)
**Depends on:** [redesign-00-dependency-and-theme.md](redesign-00-dependency-and-theme.md)

## Design reference

Design project `274c396b-cc57-4eb1-8e13-4bea2287765d`, `reSync - Calm Reader v2.dc.html`, frame
labelled "Search" (present in both the Light and Dark sections).

The screen's top app bar ("reSync") belongs to
[redesign-01-scaffold-and-navigation.md](redesign-01-scaffold-and-navigation.md), not this ticket.

## Problem

`StorySelectionView` (`SearchStoryScreen.kt:74-138`) is a plain `Column` of M2 `TextField`s, a
hand-rolled `RadioButton` + `Row` loop, and a default M2 `Button`. Against the design:

- The two fields need to be M3 filled `TextField`s with supporting text below — "e.g. 39200706"
  and "e.g. 4" (12/16, `onSurfaceVariant`). They have none today.
- Their labels change: "Story Id" → **"Story ID"**, "Chapter Id (optional)" → **"Chapter
  (optional)"**.
- The "Provider" label (`SearchStoryScreen.kt:102`) is styled `subtitle2` and needs to become
  `labelLarge`/`onSurfaceVariant` with 8dp below it.
- Provider choice is an **outlined segmented button row**, not radio buttons: one 40dp-tall
  pill-outlined row split into two segments, selected segment filled `secondaryContainer` with a
  leading check icon, unselected transparent with plain text.
- The Cloudflare warning (`SearchStoryScreen.kt:123-128`) sits inside a tonal
  `surfaceContainerHighest` 12dp-radius card with 16dp internal padding, instead of bare `caption`
  text.
- The Sync button is a full-width 40dp pill with a leading `sync` icon, **pinned to the bottom** of
  the screen (the mock puts a `flex:1` spacer above it). Today it just follows the content.

The column's outer padding in the mock is `8dp` top / `16dp` sides / `16dp` bottom, with `16dp`
gaps between sections.

## Proposed fix

### 1. Text fields

Swap to `androidx.compose.material3.TextField` and add supporting text:

```kotlin
TextField(
    value = storyId.value,
    onValueChange = { storyId.value = it },
    label = { Text("Story ID") },
    supportingText = { Text("e.g. 39200706") },
    isError = storyIdText.isNotEmpty() && !isValidNumericId(storyIdText),
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    modifier = Modifier.fillMaxWidth(),
)
```

The mock's fields span the full content width; add `Modifier.fillMaxWidth()` (today they are
intrinsically sized). Keep the `isError` / `keyboardOptions` wiring exactly as-is — the validation
helpers (`isValidNumericId`, `isValidOptionalNumericId`, `canSyncStory`, lines 53-71) are pure and
must not be touched by this ticket, and they are covered by existing unit tests.

Do not pass a `colors =` override; `TextFieldDefaults.colors()` already produces the design's
`surfaceContainerHighest` container, `primary` focus indicator and `primary` floated label.

### 2. Segmented button row

Replace the `DriverType.values().forEach { … RadioButton … }` block (lines 103-121) with:

```kotlin
SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
    DriverType.entries.forEachIndexed { index, driver ->
        SegmentedButton(
            selected = driverType.value == driver,
            onClick = { driverType.value = driver },
            shape = SegmentedButtonDefaults.itemShape(index, DriverType.entries.size),
            label = { Text(driver.websiteName(), maxLines = 1) },
        )
    }
}
```

`SegmentedButton` shows/hides the leading check icon and animates the segment fill via
`SegmentedButtonDefaults.colors()` — don't hand-roll either. `SingleChoiceSegmentedButtonRow` is
`@ExperimentalMaterial3Api` in material3 1.4.0; opt in at the composable, not module-wide.

**Label width.** The mock cheats here: it lets the "Archive of Our Own" segment hug its content and
gives "FanFiction.Net" the remaining space. `SingleChoiceSegmentedButtonRow` weights segments
*equally* — about 156dp each on a 360dp-wide device, minus ~18dp for the check icon and the
segment's own padding. "Archive of our Own" at `labelLarge` will not fit. So:

- Try `websiteName()` first, and check `SearchStoryScreenPreview` at the default preview width.
- If either label ellipsizes, add `DriverType.shortName()` returning `"AO3"` / `"FF.Net"` and use
  it **only** in this row. Leave `websiteName()` untouched — it is the user-facing name on the
  Confirm screen's provider chip
  ([redesign-03](redesign-03-confirm-chapters-screen.md)) and in `DownloadScreen`'s error copy.

Either outcome is acceptable; what is not acceptable is shipping a truncated label.

### 3. Cloudflare warning card

Wrap the existing `Text` (lines 123-128, copy unchanged) in:

```kotlin
Card(
    colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    ),
    shape = RoundedCornerShape(12.dp),
    modifier = Modifier.fillMaxWidth(),
) {
    Text(
        "…",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(16.dp),
    )
}
```

### 4. Sync button

Full-width M3 `Button`, bottom-anchored. M3 `Button` has no `leadingIcon` parameter — put the icon
in the content lambda per M3 convention:

```kotlin
Spacer(Modifier.weight(1f))
Button(
    onClick = onClick,
    enabled = canSyncStory(storyIdText, chapterIdText),
    modifier = Modifier.fillMaxWidth(),
) {
    Icon(Icons.Rounded.Sync, contentDescription = null, Modifier.size(18.dp))
    Spacer(Modifier.width(8.dp))
    Text("Sync")
}
```

The `enabled = canSyncStory(...)` gating must not change. The `sync` icon comes from
[redesign-11-material-symbols-icons.md](redesign-11-material-symbols-icons.md); if that ticket
hasn't landed, ship without the icon (text-only `Button`) rather than substituting an unrelated
glyph, and leave a `TODO` naming ticket 11.

The `Spacer(Modifier.weight(1f))` requires the parent `Column` to have a bounded height — add
`Modifier.fillMaxSize()` to it.

## Acceptance criteria

- [ ] `SearchStoryScreen.kt` imports `androidx.compose.material3.*` and contains no
      `androidx.compose.material.*` import.
- [ ] Both fields are M3 `TextField`s, full-width, labelled "Story ID" and "Chapter (optional)",
      with supporting text "e.g. 39200706" and "e.g. 4" respectively.
- [ ] `RadioButton` no longer appears in the file; provider selection is
      `SingleChoiceSegmentedButtonRow` + `SegmentedButton`, and tapping a segment still sets
      `driverType.value` (the `MutableState` plumbing at `SearchStoryScreen.kt:32` is unchanged).
- [ ] Neither segment label is truncated or ellipsized in `SearchStoryScreenPreview`; if
      `shortName()` was added, `websiteName()` still exists and is still used by
      `ConfirmChapters`.
- [ ] The Cloudflare warning renders inside a `surfaceContainerHighest` card with 12dp corners and
      16dp internal padding; its copy is byte-identical to today's.
- [ ] The Sync button is the last element in the column, full-width, and separated from the content
      above by a `weight(1f)` spacer — i.e. it sits at the bottom of the screen, not directly under
      the warning card.
- [ ] `canSyncStory` / `isValidNumericId` / `isValidOptionalNumericId` are byte-identical to before
      this ticket, and `./gradlew test` passes (their unit tests must not need changing).
- [ ] `SearchStoryScreenPreview` (line 140) renders without error; add a dark-theme variant
      (`ReSyncTheme(darkTheme = true)`) alongside it.
