# Migrate Consolidate's pull-to-refresh and bottom sheet to M3

**Priority:** medium
**Area:** `app/src/main/java/eu/monniot/resync/ui/launcher/ConsolidateScreen.kt`
(`ConsolidateView` `:89-183`, `DocumentBottomSheetView` `:185-260`), `app/build.gradle`
**Depends on:** [redesign-00-dependency-and-theme.md](redesign-00-dependency-and-theme.md),
[redesign-08-consolidate-list.md](redesign-08-consolidate-list.md) (touches the same block —
sequencing them avoids a conflict)

## Design reference

Design project `274c396b-cc57-4eb1-8e13-4bea2287765d`, `reSync - Calm Reader v2.dc.html`.

The design has **no frame for the bottom sheet** — it isn't drawn in any state. That part of this
ticket is a mechanical M2→M3 migration with best-effort token mapping, not a redesign. Don't invent
detail the design doesn't specify.

## Problem

Two M2 APIs remain in this file after
[redesign-08](redesign-08-consolidate-list.md), and both changed shape in M3 rather than merely
moving package:

1. **Pull-to-refresh** — `rememberPullRefreshState` (line 144), `Modifier.pullRefresh` (146) and
   `PullRefreshIndicator` (178).
2. **Bottom sheet** — `ModalBottomSheetLayout`/`rememberModalBottomSheetState`/
   `ModalBottomSheetValue` (lines 125-142). M2's version *wraps* the screen content; M3's overlays
   it, so the call-site structure changes rather than the type name.

Also: `import com.google.accompanist.swiperefresh.SwipeRefresh` / `rememberSwipeRefreshState`
(lines 32-33) are imported but unused — the real implementation is the M2 `pullRefresh` above.

## Proposed fix

### 1. Pull-to-refresh

Use `PullToRefreshBox`. **Verified available**: BOM `2026.08.00` pins `material3` to 1.4.0, which
has both `PullToRefreshBox` and `Modifier.pullToRefresh`. No investigation needed.

```kotlin
PullToRefreshBox(
    isRefreshing = refreshing,
    onRefresh = onRefresh,
) {
    LazyColumn(Modifier.fillMaxSize()) { … }
}
```

This replaces the `Box` + `Modifier.pullRefresh` + separately-positioned `PullRefreshIndicator`
with one component that owns its own indicator. `PullToRefreshBox` is
`@ExperimentalMaterial3Api` — opt in at the composable, not module-wide.

`onRefresh` reaches `ConsolidateViewModel.refreshDocuments()` (lines 306-307), which is a
deliberate no-op per `CLAUDE.md`'s reMarkable Cloud removal notes. It stays a no-op — this ticket
is UI-only. That means the indicator will spin and never stop, because nothing ever sets
`isRefreshing` back to false. Guard against that by having `refreshDocuments()` flip
`isRefreshing` true then immediately false, so the gesture visibly completes; keep the existing
`TODO` comment at 304-305 explaining what belongs there.

### 2. Bottom sheet

M3's `ModalBottomSheet` is shown conditionally rather than wrapping the content:

```kotlin
var sheetDocument by remember { mutableStateOf<GroupedDocument?>(null) }

// … content, with rows doing `sheetDocument = doc`

sheetDocument?.let { doc ->
    ModalBottomSheet(onDismissRequest = { sheetDocument = null }) {
        DocumentBottomSheetView(doc)
    }
}
```

This removes the need for `rememberCoroutineScope()` + `modalBottomSheetState.show()` (lines
128, 169-171) and for the `"No document selected"` placeholder (line 137) — a null document now
means no sheet at all, rather than an empty one.

Update the row `onClick` from
[redesign-08](redesign-08-consolidate-list.md)'s `ListItem` to just `sheetDocument = doc`.

### 3. `DocumentBottomSheetView`

Three M2 `ListItem`s (lines 193, 207, 229). The first two map directly —
`overlineText` → `overlineContent`, `text` → `headlineContent`:

- `"Story"` overline + `document.title` headline (drop the explicit `typography.h6`; M3
  `ListItem`'s headline default is already right).
- `"Files to consolidate"` overline + the joined chapter list headline.

The third (lines 229-259) is the consolidate action row: an M2 `ListItem` tinted
`MaterialTheme.colors.primary` with mirrored chevrons on both sides and a centered "Consolidate"
label. The design doesn't draw it. Rather than porting the `ListItem`-retrofit the existing `TODO`
at lines 227-228 complains about, replace it with a full-width M3 `Button`:

```kotlin
Button(onClick = { … }, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
    Text("Consolidate")
}
```

That is what the row was imitating, gets `primary`/`onPrimary` and the pill shape for free, and
resolves the `TODO` — delete it along with the two `Icons.AutoMirrored.Rounded.KeyboardArrow*`
imports (lines 11-14) if nothing else uses them. Keep the `println("Consolidate it !")` body; wiring
it up is the future cloud work's job.

`DocumentBottomSheetView` currently takes a `ColumnScope` receiver purely to enforce vertical
placement (lines 185-190). M3's `ModalBottomSheet` content lambda is also a `ColumnScope`, so the
receiver still works — keep it and its comment.

### 4. Dead dependency

Remove the unused imports at lines 32-33, and — since nothing else in the project references it —
also drop `accompanist-swiperefresh` from `app/build.gradle` and
`gradle/libs.versions.toml` (the `accompanistSwiperefresh` version and the
`accompanist-swiperefresh` library entry). Confirm with
`grep -rn "accompanist" app/src` returning nothing first.

## Acceptance criteria

- [ ] `ConsolidateScreen.kt` imports `androidx.compose.material3.*` and contains **no**
      `androidx.compose.material.*` import — after this ticket the file is fully M3.
- [ ] `ExperimentalMaterialApi` no longer appears in the file; the only opt-ins are
      `ExperimentalMaterial3Api`, applied per-composable.
- [ ] Pull-to-refresh uses `PullToRefreshBox`; pulling down on the document list visibly triggers
      the indicator and it returns to rest. Verify on a device or emulator and record it in the PR.
- [ ] `refreshDocuments()` is still functionally a no-op (fetches nothing) and its `TODO` comment
      is intact.
- [ ] The bottom sheet is M3 `ModalBottomSheet`, shown via a nullable document rather than a
      wrapper; `"No document selected"` no longer appears in the file.
- [ ] The consolidate action is a full-width M3 `Button`; the mirrored-chevron `ListItem` and the
      `TODO` at lines 227-228 are gone.
- [ ] `grep -rn "accompanist" app/ gradle/` returns nothing, and `./gradlew assembleDebug` still
      passes.
- [ ] All five previews (`ConsolidateScreen.kt:262-397`) render without error.
