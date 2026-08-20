# Migrate to Material Symbols-equivalent icons

**Priority:** medium (needed for full visual parity, but each screen ticket can ship with
placeholder/closest-existing icons and swap in this ticket's icons afterward — sequence flexibly)
**Area:** `gradle/libs.versions.toml`, `app/build.gradle`, all `ui/**` screens, plus
`app/src/main/java/eu/monniot/resync/ui/icons/LibraryBooks.kt`
**Depends on:** [redesign-00-dependency-and-theme.md](redesign-00-dependency-and-theme.md)

## Design reference

Design project `274c396b-cc57-4eb1-8e13-4bea2287765d`, `reSync - Calm Reader v2.dc.html` +
`M3NavBar.dc.html`. Both load Google's `Material+Symbols+Rounded` variable webfont
(`<link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Rounded:opsz,wght,FILL,GRAD@24,400,0..1,0...">`)
and render icons as `<span style="font-family:'Material Symbols Rounded'">glyph_name</span>` with
`font-variation-settings:'FILL' 1` toggled for the filled/outline states (nav bar active state,
check/error icons in circles).

## Problem

A web variable font can't be dropped into Compose `Icon()` directly — `Icon` expects an
`ImageVector` (or a `Painter`), not a font glyph. `libs.versions.toml:35` only declares
`compose-material-icons-core`, the minimal icon set; the specific glyphs the mock uses (`sync`,
`inbox`, `cloud_off`, `description`, `error`, `expand_more`/`expand_less`, `share`, `close`) mostly
aren't in `-core`.

Also: the app has a custom hand-drawn icon, `LibraryBooks.kt`, used for the Consolidate nav item
(`LauncherScreen.kt:20`) — the mock's Consolidate nav icon is Material Symbols' `sync` glyph
instead (see `M3NavBar.dc.html`'s `isConsolidate`/`notConsolidate` blocks, both using `sync`), not
a library/books icon. That's a semantic change in what the icon *means*, not just a font swap —
confirm intentional before dropping the custom icon.

## Proposed fix

1. **Dependency.** Add `androidx.compose.material:material-icons-extended` (the extended M2 icon
   pack — safe to use with an M3 `Icon`/`MaterialTheme`, since these are plain `ImageVector`s with
   no M2/M3-specific API surface) to the catalog and `app/build.gradle`. It's a large artifact
   (thousands of icons); if APK size matters, consider R8/shrinker icon-tree-shaking rather than
   avoiding the dependency.

2. **Glyph mapping.** Material Symbols glyph name → nearest `material-icons-extended` equivalent
   (rounded style, matching the mock's `Rounded` font family choice):

   | Mock glyph (FILL 0 / FILL 1) | Compose icon |
   |---|---|
   | `search` | `Icons.Rounded.Search` (M3 has no outline/fill distinction for Search; use one icon for both nav states, or `Icons.Outlined.Search`/`Icons.Rounded.Search` if both exist) |
   | `sync` | `Icons.Rounded.Sync` / `Icons.Outlined.Sync` |
   | `settings` | `Icons.Rounded.Settings` / `Icons.Outlined.Settings` |
   | `check` | `Icons.Rounded.Check` |
   | `expand_more` / `expand_less` | `Icons.Rounded.ExpandMore` / `Icons.Rounded.ExpandLess` (or a single icon + 180° rotation per [redesign-08](redesign-08-motion-and-animation.md)) |
   | `close` | `Icons.Rounded.Close` |
   | `arrow_back` | `Icons.AutoMirrored.Rounded.ArrowBack` (mind RTL — the codebase already uses `AutoMirrored` variants elsewhere, e.g. `ConsolidateScreen.kt:11-12`) |
   | `share` | `Icons.Rounded.Share` |
   | `error` (filled, in a tonal circle) | `Icons.Rounded.Error` or `Icons.Filled.Error` |
   | `description` | `Icons.Rounded.Description` (or `AutoMirrored` if it has directional strokes) |
   | `inbox` | `Icons.Rounded.Inbox` |
   | `cloud_off` | `Icons.Rounded.CloudOff` |
   | `chevron_right` | `Icons.AutoMirrored.Rounded.KeyboardArrowRight` (already imported in `ConsolidateScreen.kt:12`) |

   For nav-bar-style fill/outline swaps (`FILL 0` ↔ `FILL 1`), `material-icons-extended` doesn't
   ship true variable-fill glyphs — use `Icons.Outlined.*` for `FILL 0` and `Icons.Rounded.*` (or
   `Icons.Filled.*`) for `FILL 1` where both variants exist; where only one exists, that glyph
   doesn't get a fill-swap animation and should just stay static (acceptable degradation from the
   web mock, not worth adding a custom vector for).

3. **Consolidate nav icon.** Decide whether to drop `LibraryBooks.kt`
   (`ui/icons/LibraryBooks.kt`) in favor of `sync`, matching the mock — if so, delete the file and
   its usage at `LauncherScreen.kt:20`; if the maintainer prefers keeping the books metaphor,
   record that as a deliberate mock deviation rather than an oversight.

## Acceptance criteria

- [ ] `material-icons-extended` (or equivalent) is available and used for every glyph in the table
      above, across the screens touched by
      [redesign-01](redesign-01-navigation-bar.md)–[redesign-06](redesign-06-settings-screen.md).
- [ ] Nav bar active/inactive icon states use distinct outline/filled icons where both exist.
- [ ] `LibraryBooks.kt`'s fate (kept or removed) is a recorded decision, not an accidental leftover.
