# Add Material 3 dependency and rebuild the theme

**Priority:** high (blocks every other `redesign-*` ticket)
**Area:** `gradle/libs.versions.toml`, `app/build.gradle`,
`app/src/main/java/eu/monniot/resync/ui/Theme.kt`,
`app/src/main/java/eu/monniot/resync/DeepLinkActivity.kt`
**Depends on:** nothing

## Design reference

Claude Design project **"Phase 2 planning questions"** (id `274c396b-cc57-4eb1-8e13-4bea2287765d`),
file `reSync - Calm Reader v2.dc.html`. Pull it with the `claude_design` MCP / `DesignSync` tool
(`list_files` / `get_file`), or open
`https://claude.ai/design/p/274c396b-cc57-4eb1-8e13-4bea2287765d?file=reSync+-+Calm+Reader+v2.dc.html`.
The color tokens are the inline CSS custom properties on that file's "Light" and "Dark" wrapper
`<div>`s.

## Problem

The app is not on Material 3 today. `gradle/libs.versions.toml` declares only `compose-material`
(`androidx.compose.material:material`, i.e. Compose Material **2**) and
`compose-material-icons-core`; there is no `material3` artifact in the catalog. Every screen
imports `androidx.compose.material.*`, and `Theme.kt` builds an M2 `MaterialTheme` with a
placeholder purple palette (`purple200`/`purple500`/`teal200` — never-finished defaults) and
`Shapes(small = 4dp, medium = 4dp, large = 0dp)`.

This ticket establishes the M3 color scheme, typography and shapes that every other `redesign-*`
ticket assumes exists.

## Proposed fix

### 1. Dependency

Add to `gradle/libs.versions.toml`:

```toml
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
```

and `implementation libs.compose.material3` in `app/build.gradle`, next to the existing
`implementation libs.compose.material`.

No version pin needed — the existing `compose-bom` platform covers it. **Verified:** BOM
`2026.08.00` (the version in the catalog) pins `androidx.compose.material3:material3` to **1.4.0**.
Every M3 API referenced across this ticket series exists in 1.4.0; where a ticket names an API,
it has been checked against that version.

Keep the M2 `compose-material` dependency for now — screens migrate one at a time and both
artifacts coexist on the same BOM. Removing it is
[redesign-13-remove-material2.md](redesign-13-remove-material2.md), the last ticket in the series.

### 2. Color scheme

Build `lightColorScheme(...)` / `darkColorScheme(...)` from the values below. The design specifies
these in OKLCH; the sRGB hex conversions are done and given here, so **no conversion work is
needed** — transcribe them.

Light:

| Role | OKLCH | sRGB |
|---|---|---|
| `primary` | `oklch(47% 0.18 276)` | `#4547BD` |
| `onPrimary` | — | `#FFFFFF` |
| `primaryContainer` | `oklch(91% 0.045 276)` | `#D9DFFF` |
| `onPrimaryContainer` | `oklch(24% 0.09 276)` | `#161749` |
| `secondaryContainer` | `oklch(89% 0.05 276)` | `#D1D9FD` |
| `onSecondaryContainer` | `oklch(24% 0.07 276)` | `#171A40` |
| `surface` | `oklch(98.5% 0.004 276)` | `#F9FAFD` |
| `onSurface` | `oklch(19% 0.01 276)` | `#121318` |
| `surfaceContainerLow` | `oklch(96.5% 0.006 276)` | `#F2F3F8` |
| `surfaceContainer` | `oklch(95% 0.008 276)` | `#EDEEF4` |
| `surfaceContainerHigh` | `oklch(92% 0.012 276)` | `#E2E4ED` |
| `surfaceContainerHighest` | `oklch(90% 0.014 276)` | `#DBDDE8` |
| `onSurfaceVariant` | `oklch(42% 0.02 276)` | `#4A4C58` |
| `outline` | `oklch(70% 0.015 276)` | `#9C9EA8` |
| `outlineVariant` | `oklch(84% 0.012 276)` | `#C8CAD3` |
| `error` | `oklch(48% 0.19 25)` | `#B00A1D` |
| `errorContainer` | `oklch(93% 0.04 25)` | `#FFDEDB` |
| `onErrorContainer` | `oklch(28% 0.08 25)` | `#491513` |

Dark:

| Role | OKLCH | sRGB |
|---|---|---|
| `primary` | `oklch(82% 0.09 276)` | `#B4C0FF` |
| `onPrimary` | `oklch(28% 0.10 276)` | `#1E2059` |
| `primaryContainer` | `oklch(34% 0.10 276)` | `#2B306A` |
| `onPrimaryContainer` | `oklch(90% 0.05 276)` | `#D4DCFF` |
| `secondaryContainer` | `oklch(35% 0.06 276)` | `#32375A` |
| `onSecondaryContainer` | `oklch(90% 0.04 276)` | `#D6DCF9` |
| `surface` | `oklch(16% 0.006 276)` | `#0C0D10` |
| `onSurface` | `oklch(93% 0.004 276)` | `#E7E8EA` |
| `surfaceContainerLow` | `oklch(19% 0.008 276)` | `#131417` |
| `surfaceContainer` | `oklch(21% 0.010 276)` | `#17181D` |
| `surfaceContainerHigh` | `oklch(26% 0.014 276)` | `#22242B` |
| `surfaceContainerHighest` | `oklch(30% 0.016 276)` | `#2B2D36` |
| `onSurfaceVariant` | `oklch(74% 0.014 276)` | `#A8AAB4` |
| `outline` | `oklch(46% 0.016 276)` | `#555761` |
| `outlineVariant` | `oklch(33% 0.014 276)` | `#33353D` |
| `error` | `oklch(78% 0.13 25)` | `#FF958D` |
| `errorContainer` | `oklch(34% 0.09 25)` | `#5E211F` |
| `onErrorContainer` | `oklch(90% 0.045 25)` | `#FBD3CF` |

Two of these sit marginally outside the sRGB gamut and were clipped on one channel during
conversion — light `errorContainer` (blue channel 1.026) and dark `error` (red channel 1.007).
The resulting hue shift is well under one perceptual step; use the hex values above rather than
re-deriving them, so everything downstream compares against one set of numbers.

The design only specifies these roles, not the full `ColorScheme`. Fill the remainder
(`secondary`, `tertiary`, `surfaceTint`, `inverseSurface`, `scrim`, …) from M3's tonal-palette
conventions on the same hue (~276°), so anything the design didn't style (`Switch`, `Snackbar`,
`AlertDialog`) still looks coherent. Set `surfaceTint = primary`.

### 3. Typography

Use Compose Material 3's default `Typography()` unmodified. `Roboto` is the Android system font,
so no `FontFamily` wiring is needed either.

For reference, these are the design's text styles and the M3 default that covers each — use this
table when a screen ticket names a style, **not** as a set of overrides to write:

| Design | M3 default |
|---|---|
| 24px / 32px, w400 (screen titles: "The Long Way Home", "Story ready") | `headlineSmall` |
| 22px / 28px, w400 (app-bar titles: "reSync", "Documents", "Settings") | `titleLarge` |
| 16px / 24px, w400 (field values, list headlines) | `bodyLarge` |
| 16px / 24px, w500 ("Choose specific chapters" card header) | `titleMedium` |
| 14px / 20px, w400 (bylines, supporting lines, notice copy) | `bodyMedium` |
| 14px / 20px, w500 (button labels, chip labels, section headers) | `labelLarge` |
| 12px / 16px, w400 (text-field supporting text) | `bodySmall` |
| 12px / 16px, w500, +0.5px tracking (nav-bar labels) | `labelMedium` |
| 11px / 16px, w500 ("OR" divider label) | `labelSmall` |

### 4. Shapes

Start from `androidx.compose.material3.Shapes()` defaults and override nothing here. The design's
radii already land on them: M3 `Button` is pill-shaped by default, cards are 12dp (`medium`),
chips are 8dp (`small`), filled-text-field top corners are 4dp (`extraSmall`). If a screen ticket
finds a mismatch, it overrides locally at the call site rather than changing the global `Shapes`.

### 5. `ReSyncTheme`

Update `Theme.kt` to build `androidx.compose.material3.MaterialTheme` with the new
`ColorScheme`/`Typography`/`Shapes`, keyed on `isSystemInDarkTheme()` as today. Keep the signature
`ReSyncTheme(darkTheme: Boolean = ..., content: @Composable () -> Unit)` unchanged.

Delete the now-unused `purple200`/`purple500`/`purple700`/`teal200`, `shapes` and `typography`
top-level vals.

### 6. Fix the two theme consumers

Both activities call `ReSyncTheme`, and one reads M2 theme values inside it:

- **`DeepLinkActivity.kt:24-25`** wraps its content in
  `androidx.compose.material.Surface(color = MaterialTheme.colors.background)` — the **M2**
  `MaterialTheme` object. Once `ReSyncTheme` is M3, this still compiles but paints the M2 *default*
  background (plain white / black), so the deep-link flow — the app's primary entry point per
  `CLAUDE.md` — renders on an unthemed surface. Switch to
  `androidx.compose.material3.Surface(color = MaterialTheme.colorScheme.background)` and update the
  two imports.
- **`LauncherActivity.kt`** — check it for the same pattern and fix it the same way if present.

This is not optional cleanup: skipping it leaves the app visibly broken in its main flow.

## Acceptance criteria

- [ ] `libs.compose.material3` is declared in the catalog, added in `app/build.gradle`, and
      resolves to `1.4.0` (`./gradlew :app:dependencies --configuration debugRuntimeClasspath`
      shows `androidx.compose.material3:material3:1.4.0`).
- [ ] `Theme.kt` imports `androidx.compose.material3.*` and no longer imports
      `androidx.compose.material.*`.
- [ ] `ReSyncTheme` builds an M3 `MaterialTheme`; every hex value in the two tables above appears
      verbatim in the light/dark `ColorScheme` (grep for e.g. `0xFF4547BD` and `0xFFB4C0FF`).
- [ ] `purple200`, `purple500`, `purple700`, `teal200`, the top-level `shapes` val and the
      top-level `typography` val no longer exist anywhere in the codebase.
- [ ] No `androidx.compose.material.MaterialTheme.colors` reference remains in `DeepLinkActivity`
      or `LauncherActivity` (grep for `MaterialTheme.colors` in `*Activity.kt` returns nothing).
- [ ] `./gradlew assembleDebug`, `./gradlew lint` and `./gradlew test` all pass — the M2-based
      screens still compile unchanged against the still-present M2 dependency.
- [ ] A temporary `@Preview` swatch composable (delete it before opening the PR) renders
      `primary`, `surface`, `surfaceContainerHighest`, `secondaryContainer` and `errorContainer`
      in both light and dark, and each matches the hex in the tables.
