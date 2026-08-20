# Rebuild the Consolidate screen on M3 components

**Priority:** medium (Consolidate is already labelled experimental/placeholder per `CLAUDE.md`,
and doesn't do anything until the reMarkable Cloud reimplementation lands — restyle it, but don't
block the higher-traffic screens on it)
**Area:** `app/src/main/java/eu/monniot/resync/ui/launcher/ConsolidateScreen.kt`
**Depends on:** [redesign-00-dependency-and-theme.md](redesign-00-dependency-and-theme.md)

## Design reference

Design project `274c396b-cc57-4eb1-8e13-4bea2287765d`, `reSync - Calm Reader v2.dc.html`, frames
"Consolidate — list", "Consolidate — empty", "Consolidate — no account".

## Problem

`ConsolidateView` (`ConsolidateScreen.kt:91-183`) is built entirely on `ExperimentalMaterialApi`
M2 components: `ModalBottomSheetLayout`/`rememberModalBottomSheetState`, M2 `ListItem`
(`text`/`secondaryText`/`icon`/`overlineText`/`trailing` params), and `PullRefreshIndicator`/
`rememberPullRefreshState`/`Modifier.pullRefresh`. The M3 equivalents differ in shape, not just
package: M3 `ListItem` takes `headlineContent`/`supportingContent`/`leadingContent`/
`overlineContent`/`trailingContent`, and M3 pull-to-refresh is
`PullToRefreshBox`/`Modifier.pullToRefresh` (API shape changed between M2 and M3, this is a real
migration).

Also note: `import com.google.accompanist.swiperefresh.*` (`ConsolidateScreen.kt:32-33`) is
imported but appears unused in the file (the actual pull-to-refresh is the M2
`pullRefresh`/`PullRefreshIndicator` at lines 144-178) — worth removing as dead weight while
touching this file, separately from the M3 migration itself.

## Proposed fix

1. **List rows.** Replace the M2 `ListItem(text = ..., secondaryText = ..., ...)` at
   `ConsolidateScreen.kt:158-173` with M3 `ListItem(headlineContent = { Text(doc.title) },
   supportingContent = { Text(chaptersText) }, leadingContent = { Icon(description icon) },
   modifier = Modifier.clickable { ... })`, matching the mock's 72dp-min-height row with a leading
   `description` icon, title at `bodyLarge`/`onSurface`, chapters line at `bodyMedium`/
   `onSurfaceVariant`.

2. **Empty states.** The mock splits two states the current code conflates:
   - **No documents yet** (has account, empty list): mock shows a centered `inbox` icon (48dp,
     `onSurfaceVariant`) + `"No documents yet"` (`bodyLarge`/500/`onSurface`) +
     `"Pull down to refresh"` (`bodyMedium`/`onSurfaceVariant`). Today this is just
     `Text("No documents yet, pull to refresh")` as a single `LazyColumn` item
     (`ConsolidateScreen.kt:148-153`) with no icon and no vertical centering.
   - **No account** (`ViewState.NoAccount`, lines 98-115): mock shows a centered `cloud_off` icon
     + `"No reMarkable account set"` + explanatory text about using the Share sheet. Today this
     uses `Icons.Rounded.Warning` at low alpha and only the title text, no explanatory copy —
     add the "Use the Share sheet after downloading a story to send it to reMarkable for now."
     line, matching the copy already used in `SettingsScreen.kt:31-32`.

   Both empty states need vertical+horizontal centering (`Arrangement.Center` +
   `Modifier.fillMaxSize()`, not just `fillMaxWidth()` as today) to match the mock's full-bleed
   centered layout.

3. **Pull-to-refresh.** Migrate `rememberPullRefreshState`/`Modifier.pullRefresh`/
   `PullRefreshIndicator` (lines 144, 146, 178) to M3's `PullToRefreshBox` wrapping the
   `LazyColumn`, or `Modifier.pullToRefresh` + `LoadingIndicator`/`PullToRefreshDefaults.Indicator`
   depending on the BOM's M3 version — check what's available in the resolved
   `compose-bom` version from
   [redesign-00-dependency-and-theme.md](redesign-00-dependency-and-theme.md) since this API
   moved around across M3 releases.

4. **Bottom sheet.** Migrate `ModalBottomSheetLayout`/`rememberModalBottomSheetState` (lines
   125-142) to M3 `ModalBottomSheet`/`rememberModalBottomSheetState` (the M3 sheet is a separate
   composable that overlays rather than wrapping content, so the call-site structure changes:
   M3's version is shown/hidden via a boolean + `if (showSheet) ModalBottomSheet(onDismissRequest = ...) { ... }`
   rather than wrapping the whole screen). `DocumentBottomSheetView`
   (`ConsolidateScreen.kt:191-260`) uses M2 `ListItem` internally too (lines 193, 207, 229) —
   migrate those alongside the main list rows in step 1. The consolidate-action row (lines
   229-259, background-tinted `ListItem` with mirrored chevrons) has no direct mock equivalent —
   the mock doesn't show the bottom sheet's contents in any frame — so restyle it with the same
   M3 tokens (primary background, onPrimary icons/text) as a best-effort match rather than
   inventing new mock detail.

5. Remove the unused `com.google.accompanist.swiperefresh` import (line 32-33) while in this file.

## Acceptance criteria

- [ ] Document rows use M3 `ListItem` with a leading `description` icon.
- [ ] Empty-with-account and no-account states are visually distinct, each centered full-screen,
      matching the mock's icon + title + subtitle layout.
- [ ] Pull-to-refresh uses M3 APIs; manual pull-to-refresh still triggers `onRefresh` (currently a
      no-op via `ConsolidateViewModel.refreshDocuments()`, `ConsolidateScreen.kt:306-307` — that
      stays a no-op per `CLAUDE.md`'s reMarkable Cloud removal notes, this ticket is UI-only).
- [ ] Bottom sheet uses M3 `ModalBottomSheet`.
- [ ] `ConsolidateViewUninitializedPreview`, `ConsolidateViewNoAccountPreview`,
      `ConsolidateViewInitializedNoDocsPreview`, `ConsolidateViewInitializedDocsPreview`,
      `DocumentBottomSheetViewPreview` (`ConsolidateScreen.kt:262-397`) all still compile and match
      their corresponding mock frame (or best-effort M3 styling where the mock has no equivalent).
