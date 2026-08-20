# Remove the Compose Material 2 dependency

**Priority:** low (closes out the series)
**Area:** `gradle/libs.versions.toml`, `app/build.gradle`, any file still importing
`androidx.compose.material.*`
**Depends on:** [redesign-01](redesign-01-scaffold-and-navigation.md) through
[redesign-11](redesign-11-material-symbols-icons.md) — every screen must be off M2 first.

## Design reference

None — this is cleanup. [redesign-00](redesign-00-dependency-and-theme.md) deliberately kept the M2
dependency so screens could migrate one at a time; this ticket removes it once nothing needs it.

## Problem

`app/build.gradle` carries both `implementation libs.compose.material` (Compose Material **2**) and
`implementation libs.compose.material3`. Leaving both permanently means two component libraries on
the classpath, two `MaterialTheme` objects that autocomplete identically and silently read
different color schemes, and continued use of `@ExperimentalMaterialApi` for components M3 has
stable equivalents of.

That two-`MaterialTheme` hazard is not hypothetical: it is exactly the bug
[redesign-00](redesign-00-dependency-and-theme.md) step 6 had to fix in `DeepLinkActivity`, where
M2's `MaterialTheme.colors.background` compiled fine inside an M3 theme and painted the wrong
color.

## Proposed fix

1. **Audit first.** `grep -rn "androidx.compose.material\." app/src | grep -v "androidx.compose.material.icons"`
   must come back empty. If it doesn't, the named files still have migration work — stop and finish
   the owning ticket rather than force-porting here.

   The `androidx.compose.material.icons.*` exclusion is deliberate: those come from
   `material-icons-core` / `material-icons-extended`, which are separate artifacts in the same
   group and stay ([redesign-11](redesign-11-material-symbols-icons.md)).

2. **Remove the dependency.** Delete `implementation libs.compose.material` from
   `app/build.gradle` and the `compose-material` entry from `gradle/libs.versions.toml`. Keep
   `compose-material-icons-core` and `compose-material-icons-extended`.

3. **Drop the opt-ins.** `@OptIn(ExperimentalMaterialApi::class)` should already be gone with
   `ConsolidateScreen.kt`'s migration ([redesign-09](redesign-09-consolidate-refresh-and-sheet.md));
   confirm. Then check whether `freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"` in
   `app/build.gradle` is still needed — it has been unnecessary since Kotlin 1.6 for
   `@OptIn`-annotated call sites, and the remaining `@ExperimentalMaterial3Api` usages are
   per-composable annotations, not module-wide flags. Remove it if `assembleDebug` and `test` still
   pass without it; leave it if they don't.

4. **Out of scope:** `implementation libs.material`
   (`com.google.android.material:material`) is the Android **Views** Material Components library,
   not Compose Material 2 — a different artifact backing the XML theme that `AppCompatActivity`
   needs. Leave it alone. Likewise `androidx.appcompat`.

## Acceptance criteria

- [ ] `grep -rn "androidx.compose.material\." app/src | grep -v "androidx.compose.material.icons"`
      returns nothing.
- [ ] `grep -rn "ExperimentalMaterialApi" app/src` returns nothing.
- [ ] `compose-material` appears in neither `gradle/libs.versions.toml` nor `app/build.gradle`;
      `compose-material-icons-core` and `compose-material-icons-extended` remain in both.
- [ ] `./gradlew :app:dependencies --configuration debugRuntimeClasspath` shows no
      `androidx.compose.material:material:` line (the `material-icons-*` lines are expected).
- [ ] `./gradlew assembleDebug`, `./gradlew lint` and `./gradlew test` all pass.
- [ ] Manual smoke test on a device or emulator, recorded in the PR description: all three launcher
      tabs render, and a full download runs end to end through Confirm → Downloading → Success.
