# Add the extended icon pack and map the design's Material Symbols glyphs

**Priority:** medium
**Area:** `gradle/libs.versions.toml`, `app/build.gradle`, all `ui/**` screens
**Depends on:** [redesign-00-dependency-and-theme.md](redesign-00-dependency-and-theme.md)

**Sequencing:** land this **after**
[redesign-01](redesign-01-scaffold-and-navigation.md)–[redesign-10](redesign-10-settings-screen.md).
Every screen ticket names the icons it wants and says to ship without them if this hasn't landed;
doing it last means one pass over already-migrated files instead of conflicting edits across six
in-flight branches.

## Design reference

Design project `274c396b-cc57-4eb1-8e13-4bea2287765d`, `reSync - Calm Reader v2.dc.html` and
`M3NavBar.dc.html`. Both load Google's `Material Symbols Rounded` variable webfont and render icons
as `<span style="font-family:'Material Symbols Rounded'">glyph_name</span>`, with
`font-variation-settings:'FILL' 1` on the nav bar's active item and on the success/error circle
icons.

## Problem

A web variable font can't be handed to Compose's `Icon()`, which takes an `ImageVector` or a
`Painter`. `libs.versions.toml:35` declares only `compose-material-icons-core`, the minimal set;
most of the glyphs the design uses (`sync`, `inbox`, `cloud_off`, `description`, `error`,
`expand_more`/`expand_less`, `share`, `close`) aren't in it.

## Proposed fix

### 1. Dependency

Add `androidx.compose.material:material-icons-extended` to the catalog and
`implementation libs.compose.material.icons.extended` to `app/build.gradle`.

**Verified:** BOM `2026.08.00` pins it to **1.7.8**, so it resolves with no version pin — the same
arrangement as the existing `compose-material-icons-core` entry, which the BOM also pins to 1.7.8.

Two things to know about this artifact, so nobody is surprised later:

- It is **frozen and deprecated**. Google stopped updating the icon packs after Compose Material
  1.7; the BOM keeps pinning 1.7.8 while `compose-material` itself has moved to 1.12.0. The icon
  set is a 2024-era snapshot. Every glyph in the table below exists in it, but a *newly added*
  Material Symbol would not.
- These are plain `ImageVector`s with no M2/M3-specific API surface, so using them with an M3
  `Icon`/`MaterialTheme` is fine despite the `androidx.compose.material` group id. This is not a
  reason to keep the M2 `compose-material` dependency —
  [redesign-13](redesign-13-remove-material2.md) removes that one and keeps this one.

It is a large artifact. If APK size becomes a problem, reach for R8 icon tree-shaking rather than
dropping the dependency; don't pre-optimise here.

### 2. Glyph mapping

| Design glyph | Compose icon | Used by |
|---|---|---|
| `search` | `Icons.Rounded.Search` | nav bar |
| `sync` | `Icons.Rounded.Sync` | Search screen's Sync button |
| `settings` | `Icons.Rounded.Settings` | nav bar |
| `check` | `Icons.Rounded.Check` | segmented button, Success circle |
| `expand_more` / `expand_less` | `Icons.Rounded.ExpandMore` / `Icons.Rounded.ExpandLess` | Confirm disclosure, Error disclosure |
| `close` | `Icons.Rounded.Close` | Downloading header |
| `arrow_back` | `Icons.AutoMirrored.Rounded.ArrowBack` | Confirm header |
| `share` | `Icons.Rounded.Share` | Success screen |
| `error` | `Icons.Rounded.Error` | Error circle |
| `description` | `Icons.Rounded.Description` | Consolidate list rows |
| `inbox` | `Icons.Rounded.Inbox` | Consolidate empty state |
| `cloud_off` | `Icons.Rounded.CloudOff` | Consolidate no-account state |
| `chevron_right` | `Icons.AutoMirrored.Rounded.KeyboardArrowRight` | Settings rows |

Use the `AutoMirrored` variants where listed — they flip under RTL, and the codebase already uses
them (`ConsolidateScreen.kt:11-12`).

### 3. Nav bar fill/outline swap

The design swaps `FILL 0` → `FILL 1` on the active nav item. `material-icons-extended` has no
variable-fill glyphs, so approximate with the two style families: `Icons.Outlined.*` for inactive,
`Icons.Rounded.*` (or `Icons.Filled.*`) for active.

`LauncherScreenItem` (`LauncherScreen.kt:18-22`) holds one `ImageVector` per item today; give it
two — `iconOutline` and `iconFilled` — and select on `selected` in `NavigationBarItem`'s `icon`
lambda. Where a glyph has no meaningfully different outlined variant, use the same vector for both
states; that item simply doesn't animate. That is acceptable degradation from the web mock and not
worth hand-drawing a vector for. Animating the crossfade is
[redesign-12-motion-and-animation.md](redesign-12-motion-and-animation.md).

### 4. Consolidate nav icon — resolved

The design uses `sync` for the Consolidate nav item. **Keep the existing books metaphor instead**:
`ui/icons/LibraryBooks.kt` stays (or swaps to `Icons.Rounded.LibraryBooks` /
`Icons.Outlined.LibraryBooks` from the extended pack, now that it's available, which would let the
file be deleted). Consolidate is about grouping documents, and `sync` already does duty as the
Search screen's primary action — reusing it for a nav destination would make one glyph mean two
different things.

This is a deliberate, recorded deviation from the design, not an oversight. Note it in the PR
description so it doesn't get "fixed" later.

## Acceptance criteria

- [ ] `libs.compose.material.icons.extended` is in the catalog and `app/build.gradle`, and
      `./gradlew :app:dependencies --configuration debugRuntimeClasspath` shows
      `androidx.compose.material:material-icons-extended:1.7.8`.
- [ ] Every row of the mapping table is used at its listed call site; no screen still carries a
      `TODO` referring to this ticket (grep for `redesign-11` in `app/src` returns nothing).
- [ ] `LauncherScreenItem` has `iconOutline` and `iconFilled`, and the nav bar renders the filled
      variant for the selected item.
- [ ] The Consolidate nav item still uses a books icon, and the PR description records the
      deviation from the design. If `LibraryBooks.kt` was replaced by
      `Icons.Rounded.LibraryBooks`, the file is deleted; if it was kept, nothing references a
      `sync` glyph in `LauncherScreen.kt`.
- [ ] `./gradlew assembleDebug` and `./gradlew lint` pass.
