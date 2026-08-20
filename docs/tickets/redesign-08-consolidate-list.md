# Rebuild the Consolidate list rows and empty states on M3

**Priority:** medium (Consolidate is experimental per `CLAUDE.md` and does nothing until the
reMarkable Cloud reimplementation lands — restyle it, but don't block the higher-traffic screens
on it)
**Area:** `app/src/main/java/eu/monniot/resync/ui/launcher/ConsolidateScreen.kt`
(`ConsolidateView` `:91-183`, `ConsolidateViewModel` `:278-336`)
**Depends on:** [redesign-00-dependency-and-theme.md](redesign-00-dependency-and-theme.md)

## Design reference

Design project `274c396b-cc57-4eb1-8e13-4bea2287765d`, `reSync - Calm Reader v2.dc.html`, frames
**"Consolidate — list"**, **"Consolidate — empty"**, **"Consolidate — no account"**.

The "Documents" top app bar on all three frames belongs to
[redesign-01-scaffold-and-navigation.md](redesign-01-scaffold-and-navigation.md), not this ticket.

## Problem

`ConsolidateView` is built on `ExperimentalMaterialApi` M2 components. This ticket covers the list
rows and the empty states; the pull-to-refresh and bottom-sheet migrations are
[redesign-09-consolidate-refresh-and-sheet.md](redesign-09-consolidate-refresh-and-sheet.md).

M3's `ListItem` is not a package rename — the parameters changed shape:
`text`/`secondaryText`/`icon`/`overlineText`/`trailing` become
`headlineContent`/`supportingContent`/`leadingContent`/`overlineContent`/`trailingContent`.

### The state the app is actually in

`ConsolidateViewModel.isInitialized` (line 282) is set to `ViewState.NotInitialized` and **never
updated** — there is no code path that changes it. So the only UI a user can reach today is
`Text("TODO: Select a folder")` (lines 117-121). The design has no frame for that state, and all
three of its Consolidate frames are unreachable.

Since the cloud integration was removed there is genuinely no account, and `ViewState.NoAccount`
already maps exactly onto the design's "no account" frame. **Change the initial value to
`ViewState.NoAccount`** (line 282) so the reachable screen tells the truth and matches the design.

Leave the `NotInitialized` branch and its folder-selection `TODO` in place — `CLAUDE.md` lists this
screen as an anchor point for the future cloud work, and a folder picker is plausibly part of it.
Just restyle its `Text` to `bodyLarge`/`onSurfaceVariant` centered, so it isn't visually broken if
something reaches it.

## Proposed fix

### 1. List rows

Replace the M2 `ListItem` at lines 158-173:

```kotlin
ListItem(
    headlineContent = { Text(doc.title) },
    supportingContent = { Text(doc.chapters.joinToString { FileName.formatChapters(it) }) },
    leadingContent = { Icon(Icons.Rounded.Description, contentDescription = null) },
    modifier = Modifier.clickable { … },
)
```

M3 `ListItem`'s two-line defaults already give the design's 72dp min height, `bodyLarge`/`onSurface`
headline, `bodyMedium`/`onSurfaceVariant` supporting line and `onSurfaceVariant` leading icon — no
`colors =` override.

Keep the existing `onClick` body (lines 167-172) exactly as-is; it belongs to
[redesign-09](redesign-09-consolidate-refresh-and-sheet.md), which changes how the sheet is shown.

`Icons.Rounded.Description` comes from
[redesign-11-material-symbols-icons.md](redesign-11-material-symbols-icons.md). If that hasn't
landed, omit `leadingContent` entirely rather than substituting an unrelated glyph, and leave a
`TODO` naming ticket 11.

Leave the `TODO`s at lines 156 and 161-162 (sorting, chapter-range joining) in place — out of scope.

### 2. Empty states

The design splits two states the current code conflates. Both are centered on **both** axes over
the full screen — today the `NoAccount` column is only `fillMaxWidth()` (line 100), so
`Arrangement.Center` has no effect and the content sits at the top.

Factor one private composable, since the two differ only in their three values:

```kotlin
@Composable
private fun ConsolidateEmptyState(icon: ImageVector, title: String, subtitle: String) { … }
```

`Modifier.fillMaxSize()`, `Arrangement.Center`, `Alignment.CenterHorizontally`, 32dp horizontal
padding, `textAlign = TextAlign.Center`; 48dp icon in `onSurfaceVariant`, 16dp gap, title at
`titleMedium` (16/24/500) `onSurface`, 4dp gap, subtitle at `bodyMedium`/`onSurfaceVariant`.

| State | Icon | Title | Subtitle |
|---|---|---|---|
| `ViewState.NoAccount` | `cloud_off` | `"No reMarkable account set"` | `"Use the Share sheet after downloading a story to send it to reMarkable for now."` |
| `ViewState.Ok`, empty list | `inbox` | `"No documents yet"` | `"Pull down to refresh"` |

Today's `NoAccount` state uses `Icons.Rounded.Warning` scaled to half the screen width at 18% alpha
(lines 104-112) and has no subtitle at all — replace the whole block. The 48dp fixed size and the
`onSurfaceVariant` tint replace the alpha trick; M3's color roles make it unnecessary.

The "no documents" state currently renders as a single `LazyColumn` item (lines 148-153), which is
why the `TODO` at line 149 notes the pull gesture is hard to trigger. Keep it inside the
`LazyColumn` — [redesign-09](redesign-09-consolidate-refresh-and-sheet.md) needs a scrollable
container for pull-to-refresh to work — but give the item `Modifier.fillParentMaxSize()` so it
fills the viewport and the gesture has somewhere to start. That resolves the `TODO`; delete it.

## Acceptance criteria

- [ ] Document rows use M3 `ListItem` with `headlineContent`/`supportingContent`, and a leading
      `description` icon (or none, with a `TODO` naming ticket 11).
- [ ] `ConsolidateViewModel`'s `isInitialized` starts at `ViewState.NoAccount`, so launching the
      app and tapping Consolidate shows the design's no-account frame rather than
      `"TODO: Select a folder"`. Verify on a device or emulator and record it in the PR.
- [ ] The no-account and no-documents states are visually distinct, each centered on both axes over
      the full screen, each with icon + title + subtitle per the table above.
- [ ] `Icons.Rounded.Warning` and the `.copy(alpha = 0.18f)` tint no longer appear in the file.
- [ ] The no-documents item uses `fillParentMaxSize()` and the `TODO` at line 149 is deleted.
- [ ] `ConsolidateViewNoAccountPreview` and `ConsolidateViewInitializedNoDocsPreview` visibly
      differ from each other, and `ConsolidateViewInitializedDocsPreview` shows three rows
      (`ConsolidateScreen.kt:346-397`). All render without error; add dark variants of the two
      empty states.
- [ ] `ConsolidateViewModelTest` still passes (`./gradlew testDebugUnitTest --tests
      "*.ConsolidateViewModelTest"`).
- [ ] `./gradlew assembleDebug` passes. `ConsolidateScreen.kt` will still import
      `androidx.compose.material.*` after this ticket — the remaining M2 usage is
      [redesign-09](redesign-09-consolidate-refresh-and-sheet.md)'s.
