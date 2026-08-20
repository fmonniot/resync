# Add Material 3 dependency and rebuild the theme

**Priority:** high (blocks every other `redesign-*` ticket)
**Area:** `gradle/libs.versions.toml`, `app/build.gradle`, `app/src/main/java/eu/monniot/resync/ui/Theme.kt`

## Design reference

Claude Design project **"Phase 2 planning questions"** (id `274c396b-cc57-4eb1-8e13-4bea2287765d`),
file `reSync - Calm Reader v2.dc.html`. Pull it with the `claude_design` MCP / `DesignSync` tool
(`list_files` / `get_file`), or open
`https://claude.ai/design/p/274c396b-cc57-4eb1-8e13-4bea2287765d?file=reSync+-+Calm+Reader+v2.dc.html`.
The color tokens below are transcribed from the inline CSS custom properties on that file's
"Light" and "Dark" wrapper `<div>`s — re-check them against the live file if it has moved on.

## Problem

The app is not on Material 3 today. `gradle/libs.versions.toml` only declares
`compose-material` (`androidx.compose.material:material`, i.e. Compose Material **2**) and
`compose-material-icons-core`; there is no `material3` artifact in the catalog at all. Every
screen imports `androidx.compose.material.*` and `Theme.kt` builds an M2 `MaterialTheme` with a
placeholder purple palette (`purple200`/`purple500`/`teal200` — clearly never-finished defaults,
not a real palette) and `Shapes(small = 4dp, medium = 4dp, large = 0dp)`.

This ticket is the prerequisite for the whole redesign: it establishes the M3 color scheme,
typography, and shapes that every other `redesign-*` ticket assumes exists.

## Proposed fix

1. **Dependency.** Add `androidx.compose.material3:material3` to `gradle/libs.versions.toml`
   (covered by the existing `compose-bom` platform, so no version pin needed — add
   `compose-material3 = { group = "androidx.compose.material3", name = "material3" }`) and
   `implementation libs.compose.material3` in `app/build.gradle` next to the existing
   `implementation libs.compose.material` line.

   Keep the M2 `compose-material` dependency in place for now — screens migrate one at a time
   across the other `redesign-*` tickets, and both artifacts can coexist on the same BOM. The
   **last** ticket in this series (once every screen is off `androidx.compose.material.*`) should
   remove `compose-material` and the `ExperimentalMaterialApi`-gated M2 APIs
   (`ModalBottomSheetLayout`, `pullRefresh`) it currently backs — track that as a follow-up, don't
   leave both permanently.

2. **Color scheme.** Convert the design's OKLCH tokens to `androidx.compose.material3.ColorScheme`
   (`lightColorScheme(...)` / `darkColorScheme(...)`). Compose `Color` takes sRGB, not OKLCH, so
   each token needs converting (e.g. via `colorjs.io` or any OKLCH→sRGB converter) — don't
   hand-wave the conversion, sample a few and sanity-check them against the rendered design.

   Light:
   ```
   primary                oklch(47% 0.18 276)      onPrimary            #ffffff
   primaryContainer       oklch(91% 0.045 276)      onPrimaryContainer   oklch(24% 0.09 276)
   secondaryContainer     oklch(89% 0.05 276)       onSecondaryContainer oklch(24% 0.07 276)
   surface                oklch(98.5% 0.004 276)    onSurface            oklch(19% 0.01 276)
   surfaceContainerLow    oklch(96.5% 0.006 276)    onSurfaceVariant     oklch(42% 0.02 276)
   surfaceContainer       oklch(95% 0.008 276)      outline              oklch(70% 0.015 276)
   surfaceContainerHigh   oklch(92% 0.012 276)      outlineVariant       oklch(84% 0.012 276)
   surfaceContainerHighest oklch(90% 0.014 276)
   error                  oklch(48% 0.19 25)        errorContainer       oklch(93% 0.04 25)
                                                     onErrorContainer     oklch(28% 0.08 25)
   ```

   Dark:
   ```
   primary                oklch(82% 0.09 276)       onPrimary            oklch(28% 0.10 276)
   primaryContainer       oklch(34% 0.10 276)       onPrimaryContainer   oklch(90% 0.05 276)
   secondaryContainer     oklch(35% 0.06 276)       onSecondaryContainer oklch(90% 0.04 276)
   surface                oklch(16% 0.006 276)      onSurface            oklch(93% 0.004 276)
   surfaceContainerLow    oklch(19% 0.008 276)      onSurfaceVariant     oklch(74% 0.014 276)
   surfaceContainer       oklch(21% 0.010 276)      outline              oklch(46% 0.016 276)
   surfaceContainerHigh   oklch(26% 0.014 276)      outlineVariant       oklch(33% 0.014 276)
   surfaceContainerHighest oklch(30% 0.016 276)
   error                  oklch(78% 0.13 25)        errorContainer       oklch(34% 0.09 25)
                                                     onErrorContainer     oklch(90% 0.045 25)
   ```

   The mock only specifies these roles (not the full 30+ role `ColorScheme`) — fill in the
   remaining roles (`secondary`, `tertiary`, `surfaceTint`, `inverseSurface`, etc.) with M3's
   tonal-palette conventions derived from the same hue (~276°) rather than inventing arbitrary
   colors, so anything the mock didn't explicitly style (e.g. `Switch`, `Snackbar`) still looks
   coherent.

3. **Typography.** The mock's type scale (12/16, 14/20, 16/24, 22/28, 24/32 px/line-height,
   weights 400/500/700, `Roboto`) already matches Compose Material 3's default `Typography()`
   scale (`labelSmall`, `bodyMedium`/`labelLarge`, `bodyLarge`/`titleMedium`, `titleLarge`,
   `headlineSmall`). Default `Typography()` likely needs no per-style overrides — confirm this by
   comparing each mock text style to its nearest M3 default before overriding anything. `Roboto`
   is Android's system font, so no custom `FontFamily` wiring should be needed either.

4. **Shapes.** Design radii mostly land on M3 defaults: buttons are full-height-rounded pills
   (M3 default `Shapes().extraLarge`/full — M3 `Button` is pill-shaped by default already), cards
   are 12dp (`medium`), chips are 8dp (`small`), filled-text-field top corners are 4dp
   (`extraSmall`). Start from `androidx.compose.material3.Shapes()` defaults and only override
   where a screen ticket calls out a mismatch.

5. Update `ReSyncTheme` (`Theme.kt`) to build `androidx.compose.material3.MaterialTheme` with the
   new `ColorScheme`/`Typography`/`Shapes`, keyed on `isSystemInDarkTheme()` as today. Leave the
   function signature (`ReSyncTheme(darkTheme: Boolean = ..., content: @Composable () -> Unit)`)
   unchanged so `DeepLinkActivity.kt` and `LauncherActivity.kt` don't need touching in this ticket.

## Acceptance criteria

- [ ] `libs.compose.material3` resolves and is used by `Theme.kt`.
- [ ] `ReSyncTheme` builds an M3 `MaterialTheme` with light/dark `ColorScheme`s matching the
      converted tokens above (spot-check a handful of hex values against the mock's rendered
      screenshots).
- [ ] `./gradlew assembleDebug` and `./gradlew test` still pass (existing M2-based screens keep
      compiling unchanged against the still-present M2 dependency).
- [ ] A throwaway `@Preview` (can be deleted after) visually confirms primary/surface/error roles
      render close to the mock in both light and dark.
