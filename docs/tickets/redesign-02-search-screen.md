# Rebuild the Search screen on M3 components

**Priority:** high
**Area:** `app/src/main/java/eu/monniot/resync/ui/launcher/SearchStoryScreen.kt`
**Depends on:** [redesign-00-dependency-and-theme.md](redesign-00-dependency-and-theme.md)

## Design reference

Design project `274c396b-cc57-4eb1-8e13-4bea2287765d`, `reSync - Calm Reader v2.dc.html`,
frame labelled "Search" (light + dark).

## Problem

`StorySelectionView` (`SearchStoryScreen.kt:74-138`) is a plain `Column` of M2 `TextField`s, a
hand-rolled `RadioButton` + `Row` loop for provider selection, and a default M2 `Button`. The
mock specifies:

- Two M3 filled `TextField`s ("Story ID" — shown focused/filled with the value already floated as
  a label; "Chapter (optional)" — shown unfocused) each with a supporting-text caption below
  ("e.g. 39200706" / "e.g. 4"). The current fields have no supporting text at all.
- Provider choice as an **outlined segmented button row**, not radio buttons: a single 40dp-tall
  pill-outlined row split into two segments, selected segment filled `secondaryContainer` with a
  leading check icon, unselected segment transparent with plain text.
- The Cloudflare warning text (`SearchStoryScreen.kt:123-128`) sitting inside a tonal
  `surfaceContainerHighest` rounded-12dp card instead of bare `caption` text.
- The Sync button as a full-width pill `Button` with a leading `sync` icon, pinned to the bottom
  (the mock's phone frame has the button anchored via `flex:1` spacer above it, i.e. the button
  sits at the bottom of the screen rather than immediately under the content).

## Proposed fix

1. Swap `TextField` → `androidx.compose.material3.TextField`, add
   `supportingText = { Text("e.g. 39200706") }` (and `"e.g. 4"` for chapter). Keep the existing
   `isError`/`keyboardOptions` wiring (`SearchStoryScreen.kt:88-89`, `97-98`) — validation logic
   (`isValidNumericId`, `isValidOptionalNumericId`, `canSyncStory`, lines 53-71) is pure and
   untouched by this ticket.

2. Replace the `DriverType.values().forEach { ... RadioButton ... }` block
   (`SearchStoryScreen.kt:103-121`) with `SingleChoiceSegmentedButtonRow` +
   `SegmentedButton(shape = SegmentedButtonDefaults.itemShape(...), selected = ..., onClick = ...,
   icon = { if (selected) Icon(Icons.Filled.Check, ...) })`, one segment per `DriverType`. This is
   a real component swap, not a restyle — `SegmentedButton` handles the check-icon
   show/hide and segment fill color via `SegmentedButtonDefaults.colors()`.

3. Wrap the Cloudflare warning `Text` (lines 123-128) in a `Card` (or plain `Surface`) with
   `colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)`,
   `shape = RoundedCornerShape(12.dp)`, internal `16.dp` padding.

4. Restyle the `Button` (lines 130-136) as a full-width M3 `Button` with
   `leadingIcon`-style `Icon` + `Text` inside its content lambda (M3 `Button` has no dedicated
   `leadingIcon` param — put an `Icon` then `Spacer(8.dp)` then `Text` inside the trailing
   composable lambda, per M3 convention). Anchor it to the screen bottom with a `Spacer(Modifier.weight(1f))`
   above it inside the `Column`, matching the mock's layout (today the button just follows the
   content with no bottom-anchoring).

## Acceptance criteria

- [ ] Both text fields show supporting text and use M3 `TextField`.
- [ ] Provider selection is a `SingleChoiceSegmentedButtonRow`/`SegmentedButton`, not radio
      buttons; selecting a provider still updates `driverType.value` (existing `MutableState`
      plumbing in `SearchStoryScreen.kt:32` unchanged).
- [ ] Cloudflare warning renders inside a tonal `surfaceContainerHighest` card.
- [ ] Sync button is bottom-anchored, full-width, pill-shaped, with a leading `sync` icon; the
      `enabled = canSyncStory(...)` gating (line 132) is unchanged.
- [ ] `SearchStoryScreenPreview` (line 140) still compiles and visually matches the mock.
