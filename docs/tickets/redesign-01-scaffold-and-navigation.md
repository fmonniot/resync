# Migrate the launcher scaffold, top app bar and bottom navigation to M3

**Priority:** high
**Area:** `app/src/main/java/eu/monniot/resync/ui/launcher/LauncherScreen.kt`
**Depends on:** [redesign-00-dependency-and-theme.md](redesign-00-dependency-and-theme.md)

## Design reference

Design project `274c396b-cc57-4eb1-8e13-4bea2287765d`:

- `M3NavBar.dc.html` — the nav bar component, imported into every phone frame of the main mock via
  `<dc-import name="M3NavBar" active="…">`.
- `reSync - Calm Reader v2.dc.html` — frames "Search", "Consolidate — list", "Settings" for the top
  app bar.

## Problem

`LauncherScreen.kt` is M2 on all three counts:

1. **`Scaffold` (line 30)** is `androidx.compose.material.Scaffold`. Dropping an M3 `NavigationBar`
   into its `bottomBar` slot compiles, but the surrounding scaffold still supplies M2 content
   padding and an M2 background — the migration has to include the `Scaffold` itself.
2. **`topBar = {}` (line 31)** — there is no top app bar anywhere in the app. All three mock frames
   have one: a 64dp row with a `titleLarge` (22/28, `onSurface`) title at 16dp start padding,
   reading **"reSync"** on Search, **"Documents"** on Consolidate, **"Settings"** on Settings. Note
   the Consolidate title is *not* the nav item's label.
3. **`BottomNavigation`/`BottomNavigationItem` (lines 42-64)** — plain `Icon`+`Text`, no
   active-indicator pill, no fill/outline icon swap, M2 elevation/shadow instead of a flat tonal
   background.

Per `M3NavBar.dc.html` the nav bar is 80dp tall on a `surfaceContainer` background, with three
items each rendering a 64×32dp / 16dp-radius `secondaryContainer` pill behind the icon when
active, `onSecondaryContainer` icon + `onSurface` label when active, and `onSurfaceVariant` for
both when inactive.

## Proposed fix

### 1. Scaffold

Swap `androidx.compose.material.Scaffold` for `androidx.compose.material3.Scaffold`. M3's
`Scaffold` takes `content` as a trailing lambda receiving `PaddingValues`, same as today; keep
applying it via `Modifier.padding(padding)` on the `Box`.

### 2. Top app bar

Give `LauncherScreenItem` a `topBarTitle: String` alongside `sectionName`, since "Consolidate" the
nav label and "Documents" the screen title differ:

```kotlin
enum class LauncherScreenItem(
    val sectionName: String,
    val topBarTitle: String,
    val icon: ImageVector,
) {
    Search("Search", "reSync", …),
    Consolidate("Consolidate", "Documents", …),
    Settings("Settings", "Settings", …),
}
```

Render it with M3 `TopAppBar(title = { Text(selectedItem.topBarTitle) })`. `TopAppBar`'s default
`titleLarge` typography and 64dp height already match the design; do **not** wire
`scrollBehavior` — nothing on these screens collapses, and the mock shows a static bar.

The mock draws the Search and Settings app bars on `surface` (same as the body) and the
Consolidate one on `surface` too — so `TopAppBarDefaults.topAppBarColors()` defaults are correct
and no color override is needed. (Only the *Confirm chapters* header sits on `surfaceContainer`,
and that screen is not part of this scaffold — see
[redesign-03-confirm-chapters-screen.md](redesign-03-confirm-chapters-screen.md).)

### 3. Navigation bar

Replace `BottomNavigation`/`BottomNavigationItem` with M3 `NavigationBar`/`NavigationBarItem`:

```kotlin
NavigationBar {
    LauncherScreenItem.entries.forEach { item ->
        NavigationBarItem(
            selected = selectedItem == item,
            onClick = { selectedItem = item },
            icon = { Icon(item.icon, contentDescription = null) },
            label = { Text(item.sectionName) },
        )
    }
}
```

`NavigationBarItem` already animates the active-indicator pill and applies
`NavigationBarItemDefaults.colors()` (indicator `secondaryContainer`, selected icon
`onSecondaryContainer`, selected label `onSurface`, unselected `onSurfaceVariant`) — that is all
of the visual work, with no custom drawing and no `colors =` override. Do not pass one; the
defaults are what the design specifies.

### 4. Out of scope

The filled/outline icon swap per selection state is **not** free — it needs a second `ImageVector`
per item, which is
[redesign-11-material-symbols-icons.md](redesign-11-material-symbols-icons.md). Ship this ticket
with the single `icon` field above (same vector in both states); ticket 11 adds the
`iconOutline`/`iconFilled` pair. Animating the swap is
[redesign-12-motion-and-animation.md](redesign-12-motion-and-animation.md).

## Acceptance criteria

- [ ] `LauncherScreen.kt` imports `androidx.compose.material3.*` and contains no
      `androidx.compose.material.*` import.
- [ ] `Scaffold`, `TopAppBar`, `NavigationBar` and `NavigationBarItem` are all the M3 versions;
      `BottomNavigation` and `BottomNavigationItem` no longer appear in the file.
- [ ] `LauncherScreenItem` has a `topBarTitle` field; selecting Search / Consolidate / Settings
      shows "reSync" / "Documents" / "Settings" respectively in the top app bar.
- [ ] No `colors =` argument is passed to `NavigationBar`, `NavigationBarItem` or `TopAppBar` —
      the M3 defaults carry the design's tokens.
- [ ] Selecting an item shows the pill indicator behind its icon (visible in
      `LauncherSearchPreview` / `LauncherSettingsPreview`, `LauncherScreen.kt:70-95`).
- [ ] `LauncherSearchPreview` and `LauncherSettingsPreview` both render without error in the
      Preview pane; add a `LauncherConsolidatePreview` alongside them so all three nav states and
      all three app-bar titles are previewable.
- [ ] `./gradlew assembleDebug` and `./gradlew test` pass.
