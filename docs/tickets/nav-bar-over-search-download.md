# Nav bar stays visible over full-screen download states reached from Search

**Priority:** medium
**Area:** `app/src/main/java/eu/monniot/resync/ui/launcher/LauncherScreen.kt`,
`app/src/main/java/eu/monniot/resync/ui/launcher/SearchStoryScreen.kt`

## Problem

Found and flagged (but left unfixed as out of scope) during the Material 3 redesign, in
[PR #690](https://github.com/fmonniot/reSync/pull/690). Still present on `main`.

There are two entry points into the download flow:

- `DeepLinkActivity` (browser share sheet) renders `DownloadScreen` directly under
  `ReSyncTheme { Surface { ... } }`, no `Scaffold` involved — correctly full-screen.
- `LauncherScreen` (`LauncherScreen.kt:55-98`) wraps a `Scaffold` whose `topBar` and `bottomBar`
  (the M3 `NavigationBar`, lines 68-97) render unconditionally, regardless of what's in `content`.
  Its `content` slot (lines 59-67) renders `SearchStoryScreen()` for the Search tab.
  `SearchStoryScreen` (`SearchStoryScreen.kt:64-90`) uses an `AnimatedContent` that swaps in
  `DownloadScreen(...)` (line 79) once a story is submitted, but this happens *inside* the same
  `Scaffold` content slot — it never leaves `LauncherScreen`.

So a story downloaded by typing an id into the Search tab shows the "reSync" `TopAppBar` and the
bottom `NavigationBar` around the full-screen Downloading/Success/Error states, contradicting the
design ("no navigation bar on this frame"). A story opened via a fanfiction.net/AO3 URL (the
primary flow) is unaffected.

## Proposed fix

Hoist the "a download is in progress" state up from `SearchStoryScreen` to `LauncherScreen`, so
`LauncherScreen` can bypass its own `Scaffold` entirely while a download is active — mirroring how
`DeepLinkActivity` renders `DownloadScreen` standalone. Concretely: `SearchStoryScreen` would take
the `storySelected`/submitted-story state as hoisted state (or an `onStorySelected` callback) rather
than owning it internally, and `LauncherScreen` would render either the `Scaffold` (nav idle) or
`DownloadScreen` directly (download in progress), keyed on that state.

## Acceptance criteria

- [ ] Downloading a story via the Search tab (id + optional chapter, not a deep link) shows no
      `TopAppBar` or `NavigationBar` during the Downloading/Success/Error states.
- [ ] The deep-link flow (`DeepLinkActivity`) is unaffected.
- [ ] Returning from the download flow (`onDone`/cancel) lands back on the Search tab with the
      Scaffold and nav bar restored.
- [ ] `./gradlew test` passes.
