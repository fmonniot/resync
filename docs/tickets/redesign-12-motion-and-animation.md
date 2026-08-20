# Apply the M3 motion spec

**Priority:** low (polish pass — do last, after the components it animates exist)
**Area:** `ui/launcher/LauncherScreen.kt`, `ui/launcher/SearchStoryScreen.kt`,
`ui/downloader/DownloadScreen.kt`, `ui/launcher/ConsolidateScreen.kt`
**Depends on:** [redesign-01](redesign-01-scaffold-and-navigation.md) through
[redesign-11](redesign-11-material-symbols-icons.md) — this animates components those tickets
create, so doing it earlier has nothing to attach to.

## Design reference

Design project `274c396b-cc57-4eb1-8e13-4bea2287765d`, file `Animation Handoff.md` — fetch it with
the `claude_design`/`DesignSync` tool for the full section-by-section spec. It is summarized and
**corrected against this codebase** below; where the two disagree, this ticket wins.

## Two corrections to the handoff doc

### 1. The app has no Navigation-Compose — resolved in favour of `AnimatedContent`

`Animation Handoff.md` §2 and §5 specify screen transitions as
`enterTransition`/`exitTransition` per `composable(...)` in a `NavHost`. **The app doesn't use
Navigation-Compose anywhere.** Screen switching is plain Compose state branching:

- `LauncherScreen.kt:34-38` — `when (selectedItem)` over an enum, no back stack.
- `SearchStoryScreen.kt:35-50` — `if (storySelected.value)` toggling between `StorySelectionView`
  and `DownloadScreen`.
- `DownloadScreen.kt:108-132` — `when (state)` over the `DownloadState` sealed interface, inline in
  one composable.

**Decision: wrap each branch in `AnimatedContent` keyed on the discriminating state.** Adding
Navigation-Compose was considered and rejected — it would restructure three files for transitions
`AnimatedContent` already provides, and the app has no other need for a back stack
(`DownloadScreen`'s `onDone` callback already plays the `popBackStack()` role, and
[redesign-04](redesign-04-cancellation-plumbing.md) hangs cancellation off it). Revisit only if
something later genuinely needs back-stack semantics, such as deep-linking into a mid-flow state.

### 2. `MotionTokens` is not importable

`androidx.compose.material3.tokens` is an `internal` package — the handoff doc's suggestion to pull
`EmphasizedDecelerateCubicBezier` from it won't compile. Declare one constant in a shared spot and
use it everywhere:

```kotlin
val EmphasizedEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
```

Durations: **200ms** for small elements (icons, state layers), **300ms** for screen-level
transitions.

## Proposed fix

1. **Nav bar** (`LauncherScreen.kt`). The active-indicator pill animates for free with
   `NavigationBarItem` — [redesign-01](redesign-01-scaffold-and-navigation.md) already forbids
   passing a `colors =` override that could suppress it, so just confirm. For the icon fill swap,
   wrap each `Icon` in
   `AnimatedContent(targetState = selected, transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(150)) })`,
   using the `iconOutline`/`iconFilled` pair from
   [redesign-11](redesign-11-material-symbols-icons.md).

2. **Search ↔ Download** (`SearchStoryScreen.kt:35-50`). Wrap the `if (storySelected.value)` branch:

   ```kotlin
   AnimatedContent(
       targetState = storySelected.value,
       transitionSpec = {
           (slideInHorizontally { it / 4 } + fadeIn()) togetherWith
               (slideOutHorizontally { -it / 4 } + fadeOut())
       },
   ) { selected -> if (selected) DownloadScreen(…) else StorySelectionView(…) }
   ```

   300ms, `EmphasizedEasing`. **Use the lambda's parameter, not `storySelected.value`**, inside the
   content — reading the state directly makes both the outgoing and incoming frames render the new
   screen, and the transition looks broken.

3. **`DownloadState` transitions** (`DownloadScreen.kt:108-132`). Same shared-axis-X treatment,
   `AnimatedContent(targetState = state, contentKey = { it::class })` around the existing
   `when (state)`. `contentKey` on the class matters: `DownloadingRemainingChapters` emits a new
   instance per chapter, and without it every chapter would trigger a full screen transition.

4. **Search screen fields** (`SearchStoryScreen.kt`). Free with M3 `TextField` (label float and
   indicator color animate on focus) and with `SegmentedButton` (container color and check-icon
   visibility). [redesign-02](redesign-02-search-screen.md) already forbids `colors =` overrides on
   both — confirm none crept in. No animation code to write here.

5. **Chapter-picker disclosure** ([redesign-03](redesign-03-confirm-chapters-screen.md)'s `expanded`
   boolean). Wrap the revealed card in
   `AnimatedVisibility(visible = expanded, enter = expandVertically(tween(300, easing = EmphasizedEasing)) + fadeIn(), exit = shrinkVertically() + fadeOut())`.

   Rotate a single chevron with
   `Modifier.rotate(animateFloatAsState(if (expanded) 180f else 0f).value)` instead of swapping
   `ExpandMore`/`ExpandLess` assets — simpler, and the handoff doc prefers it explicitly. That
   means ticket 03 and ticket 07 can each drop one of their two chevron icons.

   Note ticket 03's collapsed state is a `TextButton` and its expanded state is a `Card`, not one
   container that grows — so `AnimatedVisibility` wraps the card, and the `TextButton` gets its own
   `AnimatedVisibility(visible = !expanded)`.

6. **Error "Technical details" disclosure** ([redesign-07](redesign-07-error-screen.md)). Identical
   treatment to step 5.

7. **`RangeSlider`** ([redesign-03](redesign-03-confirm-chapters-screen.md)). M3 already animates
   thumb scale-up and the press state-layer halo. Confirm no custom `startThumb =` / `endThumb =`
   composable was passed that drops those states. No code to write if the migration used defaults.

8. **Downloading progress ring** ([redesign-05](redesign-05-downloading-screen.md)). Back the
   determinate `progress` with

   ```kotlin
   val animated by animateFloatAsState(
       targetValue = currentlyDownloading.toFloat() / totalToDownloads,
       animationSpec = tween(400),
   )
   CircularProgressIndicator(progress = { animated }, …)
   ```

   Without this the arc jumps a whole chapter at a time instead of sweeping. One line, easy to miss.

9. **Success/Error circle entrance** ([redesign-06](redesign-06-success-screen.md),
   [redesign-07](redesign-07-error-screen.md)). Wrap the tonal circle in
   `AnimatedVisibility` with `scaleIn(initialScale = 0.6f) + fadeIn()`,
   `tween(200, delayMillis = 150)`, so it lands just after step 3's screen transition finishes
   rather than during it.

10. **Consolidate list rows** ([redesign-08](redesign-08-consolidate-list.md)). Confirm rows use
    `Modifier.clickable(...)` — which gets the default ripple/state layer — rather than a custom
    `Modifier.pointerInput` that suppresses it. The handoff doc marks the empty-state entrance
    animation optional and low priority; skip it.

## Acceptance criteria

- [ ] A single `EmphasizedEasing` constant exists and is used by every screen-level transition in
      this ticket; no file imports `androidx.compose.material3.tokens`.
- [ ] Nav bar: the active pill animates, and the icon crossfades between outline and filled.
- [ ] Search↔Download and `DownloadState` transitions are shared-axis-X, ~300ms, `EmphasizedEasing`.
- [ ] The `DownloadState` `AnimatedContent` passes `contentKey = { it::class }` — advancing from
      chapter 3 to chapter 4 does not re-trigger the screen transition. Verify on a device or
      emulator against a multi-chapter story and record it in the PR.
- [ ] Both `AnimatedContent` blocks read their content from the lambda parameter, not from the
      state they're keyed on.
- [ ] The chapter-picker and "Technical details" disclosures animate open and closed, and each uses
      one rotating chevron rather than two swapped vectors.
- [ ] The progress ring sweeps between chapters instead of jumping — visible in the same
      multi-chapter run as above.
- [ ] The Success and Error circles scale in after the screen transition settles, not during it.
- [ ] `./gradlew assembleDebug` and `./gradlew test` pass.
