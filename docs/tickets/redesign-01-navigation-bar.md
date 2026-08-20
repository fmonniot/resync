# Migrate the bottom navigation bar to M3 NavigationBar

**Priority:** high
**Area:** `app/src/main/java/eu/monniot/resync/ui/launcher/LauncherScreen.kt`
**Depends on:** [redesign-00-dependency-and-theme.md](redesign-00-dependency-and-theme.md)

## Design reference

Design project `274c396b-cc57-4eb1-8e13-4bea2287765d`, component file `M3NavBar.dc.html`
(imported into the main mock via `<dc-import name="M3NavBar" active="...">` on every phone
frame). Fetch both files with the `claude_design`/`DesignSync` MCP tool.

## Problem

`LauncherScreen.kt:41-65` uses M2 `BottomNavigation`/`BottomNavigationItem` with plain
`Icon`+`Text` — no active-indicator pill, no fill/outline icon swap, default M2 elevation/shadow
instead of a flat tonal `surfaceContainer` background.

The mock's nav bar (`M3NavBar.dc.html`) is 80dp tall, `surfaceContainer` background, three items
each rendering a 64×32dp pill (`secondaryContainer`, `16dp` corner radius) behind the icon when
active, filled (`FILL 1`) icon + `onSurface` label when active vs. outline icon + `onSurfaceVariant`
label when inactive.

## Proposed fix

Replace `BottomNavigation`/`BottomNavigationItem` with M3 `NavigationBar`/`NavigationBarItem`:

```kotlin
NavigationBar {
    NavigationBarItem(
        selected = selectedItem == LauncherScreenItem.Search,
        onClick = { selectedItem = LauncherScreenItem.Search },
        icon = { Icon(...) },
        label = { Text(LauncherScreenItem.Search.sectionName) },
    )
    // ...
}
```

`NavigationBarItem` already animates the active-indicator pill (width/fade) and applies
`NavigationBarItemDefaults.colors()` (indicator = `secondaryContainer`, selected icon/label =
`onSecondaryContainer`/`onSurface`, unselected = `onSurfaceVariant`) for free — this is most of
the visual work with no custom drawing needed. Confirm the defaults aren't overridden to
something that suppresses the indicator.

The filled/outline icon swap per selection state (search: filled magnifying glass when active,
outline when inactive — same for the other two items) is **not** free and needs real icons for
both states; that's covered by
[redesign-07-material-symbols-icons.md](redesign-07-material-symbols-icons.md), which this ticket
should either depend on or ship with a temporary same-icon-both-states fallback if sequenced
first.

`LauncherScreenItem` (`LauncherScreen.kt:18-22`) currently holds one `ImageVector` per item; it
will need two (`iconOutline`, `iconFilled`) once the fill-swap icons land.

## Acceptance criteria

- [ ] `LauncherScreen` uses `NavigationBar`/`NavigationBarItem` instead of `BottomNavigation`.
- [ ] Selected item shows the `secondaryContainer` pill indicator; matches mock's 80dp height and
      flat (non-elevated) `surfaceContainer` background.
- [ ] `LauncherSearchPreview`/`LauncherSettingsPreview` (`LauncherScreen.kt:70-95`) still render
      and visually match the corresponding mock state.
