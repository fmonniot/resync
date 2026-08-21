# Cover StorySelectionView and ConsolidateView with Compose UI tests

**Priority:** medium
**Area:** `app/src/main/java/eu/monniot/resync/ui/launcher/SearchStoryScreen.kt`,
`app/src/main/java/eu/monniot/resync/ui/launcher/ConsolidateScreen.kt`
**Depends on:** [compose-ui-test-infra-and-confirm-chapters](compose-ui-test-infra-and-confirm-chapters.md)

## Problem

`SearchStoryScreen.kt` is 3.5% covered (its pure predicates `isValidNumericId`/
`isValidOptionalNumericId`/`canSyncStory` are tested in `SearchStoryScreenTest`, but the
`StorySelectionView` composable that uses them, `SearchStoryScreen.kt:116-211`, isn't). It's the
first screen every user sees, and it has a real bug surface: the Sync button's `enabled` (line
199) and each `TextField`'s `isError` (lines 135, 147) are three independent derivations of the
same two text fields — easy for a future edit to desync from `canSyncStory`.

`ConsolidateScreen.kt` is 11.2% covered. `ConsolidateView` (`:85-165`) branches on `ViewState`
(`NoAccount`/`NotInitialized`/`Ok`) and, in the `Ok`/non-empty case, renders a `LazyColumn` of
`GroupedDocument`s where tapping a row opens a `ModalBottomSheet` (`sheetDocument`, lines 113,
149-151, 158-162) showing `DocumentBottomSheetView` (`:212-248`). None of that state machine is
exercised — `ConsolidateViewModelTest` only covers the pure `group()` function, not the view.

## Proposed fix

`StorySelectionView`: render it with a driven `MutableState` for `storyId`/`chapterId`/
`driverType`, and assert:
- Sync button is disabled for an invalid/blank story id, enabled once valid.
- Typing a non-numeric chapter id turns that field's `isError` on without affecting the story id
  field.
- Selecting a different provider segment updates `driverType`'s captured value.

`ConsolidateView`: assert per `ViewState`:
- `NoAccount` and `NotInitialized` each render their static copy (no interaction to test there).
- `Ok` with an empty `documents` list shows the "No documents yet" empty state.
- `Ok` with documents renders one row per `GroupedDocument`, and tapping a row opens the bottom
  sheet showing that document's title and chapters (via `DocumentBottomSheetView`).
- Pull-to-refresh invokes `onRefresh`.

## Acceptance criteria

- [ ] `StorySelectionView`'s Sync-button-enablement and field-error states are covered by
      interaction tests, not just the underlying pure predicates.
- [ ] `ConsolidateView` has a test per `ViewState` branch, including the row-tap → bottom-sheet
      flow.
- [ ] `./gradlew test` passes.
- [ ] Both files' line coverage in `jacocoTestReport` visibly improves over baseline.
