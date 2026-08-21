# Extract a DAO seam so ConsolidateViewModel's instance logic is unit-testable

**Priority:** trivial
**Area:** `app/src/main/java/eu/monniot/resync/ui/launcher/ConsolidateScreen.kt`

## Problem

`ConsolidateViewModel` (`ConsolidateScreen.kt:266-331`) is 0% covered outside its `companion
object` (the pure `group()` function, tested in `ConsolidateViewModelTest`). The instance itself
can't currently be unit-tested: its `init` block (lines 282-291) calls
`RemarkableDatabase.getInstance(application)` directly, which needs a real `Application` and Room
DB — per `CLAUDE.md`'s testing notes, that's `connectedAndroidTest` territory, not
`testDebugUnitTest`. That leaves `documents` (the `dao.getAll().map { group(it) }` wiring, line
291) and `refreshDocuments()` (the `isRefreshing` toggle, lines 297-302) untested, even though
neither actually needs Room — they only need *some* `DocumentsDao`.

This is exactly the seam `Driver.kt` already uses `ChapterReader` for (see that file's own comment
at `Driver.kt:13-17`: "lets that state machine be driven with a fake in unit tests, without a
WebView or a real Context") and CLAUDE.md's testing notes call out the same fix for this kind of
code: "existing TODOs suggest factoring it out as a parameter when touching that area."

## Proposed fix

Give `ConsolidateViewModel` a secondary constructor (or a factory function) that takes a
`DocumentsDao` directly, with the existing `Application`-based constructor resolving the DAO via
`RemarkableDatabase.getInstance(application).documentsDao()` and delegating to it. Then in a new
`ConsolidateViewModelTest` addition, construct it with a fake in-memory `DocumentsDao` (a simple
class backed by a `MutableStateFlow<List<Document>>`, no Room) and assert:
- `documents` reflects `group()` applied to whatever the fake DAO emits.
- `refreshDocuments()` leaves `refreshing` back at `false` after completing (it's a no-op today,
  per the `TODO` at line 294-296, but the visible toggle-and-settle behavior for the pull-to-refresh
  gesture is real and worth locking in before the cloud re-implementation changes it).

## Acceptance criteria

- [ ] `ConsolidateViewModel`'s `documents` flow and `refreshDocuments()` are unit-tested against a
      fake `DocumentsDao`, no Room/Robolectric involved.
- [ ] The `Application`-based constructor is unchanged from the outside (no behavior change for
      `ConsolidateScreen`'s `viewModel()` call at `ConsolidateScreen.kt:39`).
- [ ] `./gradlew test` passes.
