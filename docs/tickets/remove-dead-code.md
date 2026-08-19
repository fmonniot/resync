# Remove dead code and template leftovers

**Priority:** low
**Area:** repo-wide

## Problem

- `ui/launcher/TestSavingAnimation.kt` — 420 lines, referenced from nowhere in `main`.
- `PreferencesManager.watchCurrentAccount()` (`PreferencesManager.kt:90`) is a bare `TODO()`
  that will throw `NotImplementedError` if anything ever calls it. It has no callers.
- `ExampleUnitTest.kt` and `ExampleInstrumentedTest.kt` are still the Android Studio
  template stubs (`assertEquals(4, 2 + 2)`).
- `ui/launcher/ConsolidateScreen.kt:323` — commented-out `consolidate(story:)`.

## Proposed fix

Delete each of the above (implementing rather than deleting where another ticket covers it).
Anything worth keeping as a note should become a ticket here instead of a commented block.

## Acceptance criteria

- [ ] `./gradlew test assembleDebug` passes after removal.
- [ ] No `TODO()` (the throwing kind) left in `main`.
