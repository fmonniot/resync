# Apply the M3 motion spec (Animation Handoff)

**Priority:** low (polish pass — do last, after the components it animates exist)
**Area:** `ui/launcher/LauncherScreen.kt`, `ui/launcher/SearchStoryScreen.kt`,
`ui/downloader/DownloadScreen.kt`, `ui/launcher/ConsolidateScreen.kt`
**Depends on:** [redesign-01](redesign-01-navigation-bar.md) through
[redesign-06](redesign-06-settings-screen.md) (animates components those tickets create — doing
this first has nothing to attach to)

## Design reference

Design project `274c396b-cc57-4eb1-8e13-4bea2287765d`, file `Animation Handoff.md` — fetch via the
`claude_design`/`DesignSync` MCP tool for the full section-by-section spec; summarized and
corrected against the actual codebase below.

## Problem — the handoff doc's assumption doesn't match the app's navigation model

`Animation Handoff.md` §2 and §5 specify screen-to-screen transitions in terms of
Navigation-Compose: *"Wherever this is driven by Navigation-Compose (`NavHost`): set
`enterTransition`/`exitTransition`/... per `composable(...)`."*

**The app doesn't use Navigation-Compose anywhere.** Screen switching today is plain
Compose state branching:
- `LauncherScreen.kt:34-38` — a `when (selectedItem)` over an enum, no back stack.
- `SearchStoryScreen.kt:35-50` — an `if (storySelected.value)` boolean toggle between
  `StorySelectionView` and `DownloadScreen`.
- `DownloadScreen.kt:108-132` — a `when (state)` over the `DownloadState` sealed interface,
  rendered inline inside one `@Composable fun DownloadScreen`, not separate nav destinations.

None of these have `enterTransition`/`exitTransition` to set — that API doesn't exist without a
`NavHost`. Before implementing §2/§5, make an explicit choice and record it here:

- **(a) Wrap transitions with `AnimatedContent`** keyed on the discriminating state
  (`selectedItem`, `storySelected.value`, `state::class`) with a shared-axis-X
  `transitionSpec`. Minimal-diff, keeps the existing state-machine architecture.
- **(b) Introduce Navigation-Compose** as a real dependency and restructure these three files
  around a `NavHost`. Bigger change, but gets shared-axis transitions "for free" per the
  handoff doc as literally written, and would also give the Downloading/Error screens a real
  back stack to hang the close/cancel decisions from (see
  [redesign-04](redesign-04-download-task-states.md)'s open cancellation question).

Recommendation: **(a)**, since it's a much smaller diff and the app has no other need for a back
stack (`DownloadScreen`'s `onDone` callback pattern already serves the "return to search" role a
`NavHost.popBackStack()` would). Only pick (b) if a future ticket independently needs real
back-stack semantics (e.g. deep-linking into a mid-flow state).

## Proposed fix (assuming option (a) above)

1. **Nav bar** (`Animation Handoff.md` §1, `LauncherScreen.kt`): the active-indicator pill
   animation is free with M3 `NavigationBarItem` (per
   [redesign-01](redesign-01-navigation-bar.md)) — just confirm it's not disabled. Icon
   fill-swap: wrap each `Icon` in `AnimatedContent(targetState = selected, transitionSpec = {
   fadeIn(tween(150)) togetherWith fadeOut(tween(150)) })`, using the outline/filled icon pair
   from [redesign-07](redesign-07-material-symbols-icons.md).

2. **Search ↔ Download transition** (`Animation Handoff.md` §2/§5,
   `SearchStoryScreen.kt:35-50`): wrap the `if (storySelected.value) DownloadScreen(...) else
   StorySelectionView(...)` branch in `AnimatedContent(targetState = storySelected.value,
   transitionSpec = { (slideInHorizontally { it / 4 } + fadeIn()) togetherWith
   (slideOutHorizontally { -it / 4 } + fadeOut()) })`, 300ms, M3 emphasized easing
   (`CubicBezierEasing(0.2f, 0f, 0f, 1f)` if `MotionTokens`/`EmphasizedDecelerateCubicBezier`
   isn't exposed by the resolved Compose Material3 version — check first).

3. **`DownloadState` transitions** (`Animation Handoff.md` §5, `DownloadScreen.kt:108-132`): same
   shared-axis-X treatment, `AnimatedContent(targetState = state, ...)` keyed on `state::class`
   or a discriminating field, wrapping the existing `when (state) { ... }` block.

4. **Search screen fields** (`Animation Handoff.md` §3, `SearchStoryScreen.kt`): mostly free with
   M3 `TextField` (label float + outline/underline color animate on focus by default per
   [redesign-02](redesign-02-search-screen.md)) — confirm no custom `colors =` override hardcodes
   a static border. `SingleChoiceSegmentedButtonRow`/`SegmentedButton` animates its selected-state
   container color and check-icon visibility automatically too — no custom animation code needed
   there either, just don't fight the defaults.

5. **Chapter picker disclosure** (`Animation Handoff.md` §4,
   [redesign-03](redesign-03-confirm-chapters-screen.md)'s `expanded` boolean): wrap the revealed
   content in `AnimatedVisibility(visible = expanded, enter = expandVertically(tween(300,
   easing = EmphasizedEasing)) + fadeIn(), exit = shrinkVertically() + fadeOut())`. Rotate the
   chevron icon with `Modifier.rotate(animateFloatAsState(if (expanded) 180f else 0f).value)`
   rather than swapping between `ExpandMore`/`ExpandLess` vector assets (simpler, and matches the
   handoff doc's explicit preference for rotation over asset-swap). Same pattern applies to the
   Error screen's "Technical details" disclosure
   ([redesign-04](redesign-04-download-task-states.md)).

6. **`RangeSlider` interaction states** (`Animation Handoff.md` §4): M3's default `RangeSlider`
   already animates thumb scale-up and the press state-layer halo — just confirm the migration in
   [redesign-03](redesign-03-confirm-chapters-screen.md) didn't pass a custom `startThumb=`/
   `endThumb=` composable that drops those states.

7. **Downloading progress ring** (`Animation Handoff.md` §6,
   [redesign-04](redesign-04-download-task-states.md)): the determinate
   `CircularProgressIndicator`'s `progress` must be backed by
   `animateFloatAsState(targetValue = currentlyDownloading.toFloat() / totalToDownloads,
   animationSpec = tween(400))`, **not** the raw fraction directly — otherwise the arc jumps
   per-chapter instead of sweeping. This is a one-line but easy-to-miss change in
   `DownloadingRemainingChapters` (`DownloadScreen.kt:671-675`).

8. **Success/Error icon entrance** (`Animation Handoff.md` §7,
   [redesign-04](redesign-04-download-task-states.md)): wrap the tonal circle's check/error
   `Icon` in `scaleIn(initialScale = 0.6f) + fadeIn()` with `tween(200, delayMillis = 150)` so it
   lands just after the screen-entry transition from step 3 finishes, not simultaneously with it.

9. **Consolidate list rows** (`Animation Handoff.md` §8): confirm rows use
   `Modifier.clickable(...)` (default ripple/state-layer) rather than a custom
   `Modifier.pointerInput` that suppresses it — check this didn't regress during
   [redesign-05](redesign-05-consolidate-screen.md)'s `ListItem` migration. Empty-state entrance
   animation is explicitly called out as low-priority/optional in the handoff doc — skip it unless
   it's cheap once everything else here is done.

## Acceptance criteria

- [ ] The NavHost-vs-`AnimatedContent` decision (see Problem section) is recorded here before any
      animation code is written.
- [ ] Nav bar icon fill-swap and active pill both animate.
- [ ] Search↔Download and `DownloadState` transitions use shared-axis-X, ~300ms, emphasized easing.
- [ ] Chapter-picker and error "Technical details" disclosures use `AnimatedVisibility` +
      rotating-chevron, not instant show/hide or asset-swapped chevrons.
- [ ] Downloading progress ring sweeps via `animateFloatAsState`, doesn't jump.
- [ ] Success/Error tonal icon scale-in fires after the screen-entry transition, not concurrently.
