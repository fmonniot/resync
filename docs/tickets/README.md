# Tickets

One file per piece of work. Each ticket states the problem with file references, a proposed
fix, and acceptance criteria. Priorities are relative to each other, not absolute.

## High

- [Add Material 3 dependency and rebuild the theme](redesign-00-dependency-and-theme.md)
- [Migrate the bottom navigation bar to M3 NavigationBar](redesign-01-navigation-bar.md)
- [Rebuild the Search screen on M3 components](redesign-02-search-screen.md)
- [Rebuild the chapter-confirmation screen on M3 components](redesign-03-confirm-chapters-screen.md)
- [Rebuild Downloading/Success/Error as full-screen M3 task states](redesign-04-download-task-states.md)

## Medium

- [Rebuild the Consolidate screen on M3 components](redesign-05-consolidate-screen.md)
- [Rebuild the Settings screen scaffolding on M3 components](redesign-06-settings-screen.md)
- [Migrate to Material Symbols-equivalent icons](redesign-07-material-symbols-icons.md)

## Low / trivial

- [Comments contradict the code they describe](comment-drift.md)
- [Apply the M3 motion spec (Animation Handoff)](redesign-08-motion-and-animation.md)

## Material 3 redesign

The `redesign-*` tickets implement the Material 3 redesign explored in the Claude Design project
**"Phase 2 planning questions"** (id `274c396b-cc57-4eb1-8e13-4bea2287765d`, files
`reSync - Calm Reader v2.dc.html`, `M3NavBar.dc.html`, `Animation Handoff.md`). Pull it with the
`claude_design` MCP / `DesignSync` tool (`list_files`/`get_file` against that project id), or open
`https://claude.ai/design/p/274c396b-cc57-4eb1-8e13-4bea2287765d?file=reSync+-+Calm+Reader+v2.dc.html`.

The app currently runs on Compose **Material 2** (`androidx.compose.material`) with a
never-finished placeholder purple palette — there is no `material3` dependency in the project at
all yet. `redesign-00` is a hard prerequisite for every other ticket in the series; after that,
`01`–`06` are independent per-screen migrations and can be done in any order or parallelized,
`07` (icons) is a cross-cutting dependency several of them need for full visual parity but can
ship with fallback icons in the meantime, and `08` (motion) should be done last since it animates
components the earlier tickets create. `04` and `06` each contain an explicit open product
decision (see their "Problem / scope note" sections) that should be resolved before implementation
starts, not discovered mid-PR.

## Suggested order

