# Exclude @Preview-only composables from the coverage report

**Priority:** trivial
**Area:** `app/build.gradle`,
`app/src/main/java/eu/monniot/resync/ui/downloader/DownloadScreen.kt` and the other screen files
under `app/src/main/java/eu/monniot/resync/ui/launcher/`

## Problem

`app/build.gradle`'s `jacocoTestReport` task already excludes generated code (R, BuildConfig,
Room's `*_Impl`, Compose's `ComposableSingletons`/`LiveLiterals`) "so it doesn't dilute the
coverage numbers" (see the comment above `coverageExcludes`). `@Preview`-annotated composables are
the same kind of dilution by the same logic: they're Android Studio design-time tooling, never
invoked by the app or by any test, and were never meant to be — writing tests for them would be
pure box-checking, the opposite of what this coverage push is for.

There's a real amount of this: across the five screen files, `@Preview` functions account for
roughly 414 raw source lines (measured by scanning for `@Preview` blocks), the majority — 254 lines
across 16 preview functions — in `DownloadScreen.kt` alone. That's counted against every screen's
percentage today with zero chance of ever being covered.

## Proposed fix

Move each file's `@Preview` functions out into a sibling `*Previews.kt` file in the same package
(e.g. `DownloadScreenPreviews.kt`, `ConsolidateScreenPreviews.kt`), importing whatever internal
symbols they need. Then extend `coverageExcludes` in `app/build.gradle` with a pattern matching
those files' compiled class names (Kotlin top-level functions in `FooPreviews.kt` compile to a
`FooPreviewsKt` class — exclude `**/*PreviewsKt.class` and `**/*PreviewsKt$*.class`), the same
mechanism already used for the generated-code excludes.

Keep this purely mechanical — no test-writing, no behavior change. Do it first, before the other
tickets in this batch, so their "did coverage improve" acceptance criteria are measured against a
cleaner baseline.

## Acceptance criteria

- [ ] Every `@Preview` function has moved to a same-package `*Previews.kt` file; nothing else in
      the original files changed.
- [ ] `coverageExcludes` in `app/build.gradle` excludes the new preview files' compiled classes.
- [ ] Android Studio's preview pane still resolves all previews (moving files doesn't break
      `@Preview` resolution as long as the function stays in the same package).
- [ ] `./gradlew jacocoTestReport`'s overall percentage increases (denominator shrinks, numerator
      unchanged) — confirm by comparing `app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml`
      before/after.
- [ ] `./gradlew test` and `./gradlew lint` pass.
