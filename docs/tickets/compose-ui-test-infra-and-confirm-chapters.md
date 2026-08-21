# Add Robolectric Compose UI testing, proven on ConfirmChapters

**Priority:** high
**Area:** `app/build.gradle`, `app/src/main/java/eu/monniot/resync/ui/downloader/DownloadScreen.kt`

## Problem

Coverage is 23.04% (`app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml`, 349/1515
lines). The non-Composable logic in this codebase is already well tested — `downloadLogic`,
`selectChaptersToDownload`, `readWithRateLimit`, the drivers, `FileName`, `Epub` are all at or near
100%. Essentially all of the remaining gap is Composable UI code with zero tests, because there is
currently no way to unit-test a `@Composable` in this project: `app/build.gradle` has no
`androidx.compose.ui:ui-test-junit4` dependency and no Robolectric `graphicsMode` configuration.

`DownloadScreen.kt` alone is 713 lines at 17.8% covered and is the single largest opportunity —
it's also the most-exercised screen in the app (every download goes through it). `ConfirmChapters`
(`DownloadScreen.kt:676-898`) is its most complex piece: a two-thumb `RangeSlider` that must
produce `ChapterSelection.One` when both thumbs land on the same value and
`ChapterSelection.Range` otherwise (lines 871-891), plus a collapse/expand disclosure
(`expanded`, lines 688, 777-795, 797-895) and a "download entire story" shortcut (lines 764-769).
None of this branching is covered by anything today — `SelectChaptersToDownloadTest` drives
`ChapterSelection` values directly, it never exercises the UI that produces them.

## Proposed fix

1. Add to `app/build.gradle`:
   - `testImplementation composeBom` already present; add
     `testImplementation libs.compose.ui.test.junit4` and
     `testImplementation libs.compose.ui.test.manifest` (or `debugImplementation` per the usual
     Compose testing setup — check which the installed Compose BOM version expects).
   - A `testOptions.unitTests.all { it.systemProperty 'robolectric.graphicsMode', 'NATIVE' }` (or
     the equivalent `robolectric.properties` under `app/src/test/resources/`) so Robolectric can
     render Compose content on the JVM.
   - Verify the versions actually resolve and run against this project's Compose BOM/AGP/Kotlin
     versions before relying on them elsewhere — this is new territory for the project's toolchain,
     confirm it rather than assume it from other projects' setups.
2. Write one test file, `ConfirmChaptersTest.kt`, using `createComposeRule()` (Robolectric,
   `@RunWith(RobolectricTestRunner::class)`) that renders `ConfirmChapters` and asserts, per
   interaction, the exact `ChapterSelection` passed to `onUserConfirmation`:
   - "Download entire story" click → `ChapterSelection.All`.
   - Expand ("Choose specific chapters"), drag the range slider so both thumbs meet → `One(n)`.
   - Expand, drag thumbs apart → `Range(start, end)`.
   - The back arrow (`onCancel`, line 717-725) fires when tapped.
3. Confirm the new test actually moves `./gradlew jacocoTestReport`'s number for
   `ui/downloader/DownloadScreen.kt` — that's the proof this approach works before ticket
   [download-screen-remaining-state-composables-coverage](download-screen-remaining-state-composables-coverage.md)
   and
   [search-and-consolidate-screen-compose-coverage](search-and-consolidate-screen-compose-coverage.md)
   build on it.

Rendering the whole composable exercises nearly every line in it (every `Text`/`Modifier` call
runs on composition, not just the ones a test happens to assert on), so a handful of interaction
tests here should cover the bulk of `ConfirmChapters`' ~220 lines — this is the highest-leverage
single ticket in this batch.

## Acceptance criteria

- [ ] `ConfirmChaptersTest.kt` exists and drives all four interactions above through real Compose
      semantics (`onNodeWithText(...).performClick()` / `performTouchInput { … }`), not by calling
      private lambdas directly.
- [ ] `./gradlew testDebugUnitTest` passes, including on a clean run (no reliance on stale
      Robolectric caches).
- [ ] `./gradlew jacocoTestReport` shows `ConfirmChapters` materially covered (spot-check the HTML
      report at `app/build/reports/jacoco/jacocoTestReport/html/eu.monniot.resync.ui.downloader/DownloadScreen.kt.html`).
- [ ] The Compose testing setup (dependencies + graphics mode) is documented in `CLAUDE.md`'s
      "Testing notes" section so the next screen doesn't have to rediscover it.
