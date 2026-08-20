# Tickets

One file per piece of work. Each ticket states the problem with file references, a proposed
fix, and acceptance criteria. Priorities are relative to each other, not absolute.

## Material 3 redesign

The `redesign-*` tickets implement the Material 3 redesign explored in the Claude Design project
**"Phase 2 planning questions"** (id `274c396b-cc57-4eb1-8e13-4bea2287765d`, files
`reSync - Calm Reader v2.dc.html`, `M3NavBar.dc.html`, `Animation Handoff.md`). Pull it with the
`claude_design` MCP / `DesignSync` tool (`list_files`/`get_file` against that project id), or open
`https://claude.ai/design/p/274c396b-cc57-4eb1-8e13-4bea2287765d?file=reSync+-+Calm+Reader+v2.dc.html`.

The app currently runs on Compose **Material 2** (`androidx.compose.material`) with a
never-finished placeholder purple palette — there is no `material3` dependency in the project yet.

Each ticket is scoped to one sitting. Where a ticket names a value, a string or an API, it has been
checked against the design file and against the code at the referenced line — including the two
places where the design and the code disagree in ways that are easy to miss (see
[redesign-03](redesign-03-confirm-chapters-screen.md) step 5 and
[redesign-05](redesign-05-downloading-screen.md) step 3).

### Order

`redesign-00` is a hard prerequisite for everything else.

After it, `01`–`03` and `05`–`10` are independent per-screen migrations and can be parallelized,
with two sequencing constraints:

- `04` (cancellation) supplies the callback that `03`'s back arrow and `05`'s close icon bind to.
  Land it before either, or ship those screens without the affordance — both tickets say so
  explicitly. `04` is behavior-only and doesn't even need `00`.
- `08` and `09` touch the same block of `ConsolidateScreen.kt`; do `08` first.

`11` (icons) is cross-cutting and should land **after** `01`–`10`, not in parallel with them —
every screen ticket says to ship without its icons and leave a `TODO`, so `11` is one clean pass
instead of six conflicting ones.

`12` (motion) animates components `01`–`11` create, so it goes second to last. `13` removes the M2
dependency once nothing imports it, and closes the series.

### High

- [Add Material 3 dependency and rebuild the theme](redesign-00-dependency-and-theme.md)
- [Migrate the launcher scaffold, top app bar and bottom navigation to M3](redesign-01-scaffold-and-navigation.md)
- [Rebuild the Search screen on M3 components](redesign-02-search-screen.md)
- [Rebuild the chapter-confirmation screen on M3 components](redesign-03-confirm-chapters-screen.md)
- [Add a cancellation path to the download flow](redesign-04-cancellation-plumbing.md)
- [Rebuild the Downloading screen as a full-screen M3 task state](redesign-05-downloading-screen.md)
- [Add the Success screen and move sharing behind an explicit button](redesign-06-success-screen.md)
- [Rebuild the Error screen with a generic message, retry and copyable details](redesign-07-error-screen.md)

### Medium

- [Rebuild the Consolidate list rows and empty states on M3](redesign-08-consolidate-list.md)
- [Migrate Consolidate's pull-to-refresh and bottom sheet to M3](redesign-09-consolidate-refresh-and-sheet.md)
- [Rebuild the Settings screen on M3 components](redesign-10-settings-screen.md)
- [Add the extended icon pack and map the design's Material Symbols glyphs](redesign-11-material-symbols-icons.md)

### Low / trivial

- [Apply the M3 motion spec](redesign-12-motion-and-animation.md)
- [Remove the Compose Material 2 dependency](redesign-13-remove-material2.md)
- [Comments contradict the code they describe](comment-drift.md)

## Resolved product decisions

These were open questions in the first draft of the redesign tickets. They are settled; the
owning ticket records each one under a "Decision (resolved)" heading. Don't re-litigate them
mid-PR.

| Question | Decision | Ticket |
|---|---|---|
| Auto-share on completion, or an explicit Success screen? | Explicit Success screen; `downloadLogic` stops firing the intent | [06](redesign-06-success-screen.md) |
| What do the back arrow and close icon do? | Real cancellation; partial work and cached chapters are discarded | [04](redesign-04-cancellation-plumbing.md) |
| Add the design's Account/Sync/Storage rows with no functionality behind them? | Yes, as commented inert placeholders | [10](redesign-10-settings-screen.md) |
| Consolidate nav icon — the design's `sync`, or the existing books icon? | Keep the books metaphor; recorded deviation from the design | [11](redesign-11-material-symbols-icons.md) |
| Navigation-Compose, or `AnimatedContent`, for screen transitions? | `AnimatedContent` — no back stack needed | [12](redesign-12-motion-and-animation.md) |

## Deviations from the design

Places where the implementation intentionally differs, so they don't get "fixed" later:

- **Consolidate nav icon** — books, not `sync` (see above).
- **Segmented button labels** ([02](redesign-02-search-screen.md)) — the design lets the
  "Archive of Our Own" segment hug its content; `SingleChoiceSegmentedButtonRow` weights segments
  equally, so the labels may need shortening.
- **Nav bar icon fill swap** ([11](redesign-11-material-symbols-icons.md)) — approximated with
  `Icons.Outlined.*` / `Icons.Rounded.*`, since `material-icons-extended` has no variable-fill
  glyphs.
- **Consolidate bottom sheet** ([09](redesign-09-consolidate-refresh-and-sheet.md)) — the design
  never draws it; the migration maps M3 tokens best-effort rather than inventing detail.
- **`FetchingFirstChapterView` header** ([05](redesign-05-downloading-screen.md)) — no story title,
  because the name isn't known until the first chapter parses.
