# JaCoco reports 0% on Activity subclasses despite passing Robolectric tests

**Priority:** low
**Area:** `app/build.gradle` (jacoco config), `app/src/main/java/eu/monniot/resync/DeepLinkActivity.kt`

## Problem

`DeepLinkActivityTest` (`@RunWith(RobolectricTestRunner::class)`) calls
`DeepLinkActivity.parsePath(...)` six times, with passing assertions, including cases that only
succeed by executing every line of `parsePath` (`DeepLinkActivity.kt:40-69`). Yet
`jacocoTestReport.xml` shows `eu/monniot/resync/DeepLinkActivity` and its `$Companion` at a flat
0/16 each — every single line, including ones the test provably executes, marked `nc` (not
covered) in the HTML report.

This isn't a real gap to fix with more tests — writing more `DeepLinkActivity` tests won't move
this number, because the number is already wrong for tests that exist today. Contrast with
`DriverTest` (also `@RunWith(RobolectricTestRunner::class)`, also Robolectric-only), where
`installGrabber`'s lines *do* show as covered. The difference is that `DeepLinkActivity` extends
`AppCompatActivity`; `Driver` extends nothing Android-specific. That points at a known class of
Robolectric/JaCoco interaction problem: classes that Robolectric has to load through its own
sandboxed `InstrumentingClassLoader` (roughly, anything extending an Android framework class that
needs shadow rewriting) get a separate `Class` object from the one JaCoco's agent instrumented,
so JaCoco's probes never fire for it — but this needs confirming, not assuming.

Left unnoticed, this will waste effort on some future ticket that adds `LauncherActivity` tests (or
more `DeepLinkActivity` tests) expecting the jacoco percentage to reflect them.

## Proposed fix

This is investigate-first, fix-maybe:

1. Confirm the hypothesis: check whether `jacoco.toolVersion` (currently `0.8.12`) + the Robolectric
   version in use have a documented incompatibility here, and whether it's specific to
   `AppCompatActivity`/`Activity` subclasses or broader.
2. If there's a known fix (a JaCoco/Robolectric config flag, an `inclnolocationclasses` setting, a
   newer/older combination of versions), apply it and confirm `DeepLinkActivity`'s existing,
   already-passing tests start showing as covered.
3. If there's no practical fix, document the limitation directly in `app/build.gradle` next to
   `jacocoTestReport` (mirroring the existing `coverageExcludes` comment style) and in `CLAUDE.md`'s
   testing notes, so nobody plans coverage work against Activity subclasses expecting the number to
   move.

Low priority: `DeepLinkActivity`/`LauncherActivity` are both thin — 16 and 4 lines respectively —
so the numeric impact either way is small. This ticket is about not misleading whoever picks up
coverage work next, not about the coverage number itself.

## Acceptance criteria

- [ ] Either `DeepLinkActivity`'s tested lines show as covered in `jacocoTestReport`, or the
      limitation is documented in both `app/build.gradle` and `CLAUDE.md` with the specific
      class/pattern affected.
- [ ] `./gradlew test` passes.
