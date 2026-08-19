# Search screen ships hardcoded ids and an unreachable provider picker

**Priority:** low
**Area:** `ui/launcher/SearchStoryScreen.kt`

## Problem

The Search tab is one of the three launcher destinations, but:

- The story and chapter fields are pre-filled with the developer's test ids
  (`39200706` / `98724747`), with a previous pair left commented out just above.
- The provider `DropdownMenu` has no trigger: `expanded` is initialised to `false` and never
  set to `true` anywhere, so the menu cannot open and `driverType` is stuck on its initial
  value (`ArchiveOfOurOwn`). Entering an ff.net story id is impossible.
- The field labels are bare `Text` above the fields rather than `TextField` labels, and
  there is no validation despite `toInt()` being called on the raw input (the comment says
  "Hopefully").

## Proposed fix

- Empty defaults, `label = { Text(...) }` on the fields.
- Give the dropdown a trigger (an `OutlinedTextField`/`Button` showing the current provider
  that sets `expanded = true`), or replace it with a two-option segmented control / radio row.
- Guard the `toInt()` calls and disable the Sync button on invalid input.

## Acceptance criteria

- [ ] Both providers are selectable.
- [ ] Empty or non-numeric input cannot crash the screen.
- [ ] No developer-specific defaults.
