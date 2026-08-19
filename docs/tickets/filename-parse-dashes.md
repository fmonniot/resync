# FileName.parse fails on story titles containing a dash

**Priority:** high — silently drops documents from the Consolidate screen
**Area:** `FileName.kt`, `ui/launcher/ConsolidateScreen.kt`

## Problem

`FileName.parse` (`FileName.kt:25`) splits the whole file name on `-`:

```kotlin
val s = name.split("-")
```

The convention it is decoding is `"<Story> - Ch 3-7.epub"`, but any dash in the *title*
shifts every field:

| Input | Parsed as |
|---|---|
| `Some Story - Ch 3.epub` | `OneChapter(3)` — correct |
| `Some Story - The Sequel - Ch 3.epub` | range; `s[1] = " The Sequel "` → `toIntOrNull()` null → **null** |
| `A - B - C - Ch 1-2.epub` | 5 parts → **null** |

`ConsolidateViewModel.group` (`ui/launcher/ConsolidateScreen.kt:334`) maps `null` to a
dropped document, so those stories vanish from the Consolidate screen. Dashes are common in
fic titles.

## Proposed fix

Parse against the actual separator rather than a bare `-`. A regex anchored at the end of
the string, e.g. `^(?<name>.*?)(?: - Ch (?<from>\d+)(?:-(?<to>\d+))?)?\.epub$`, keeps the
title intact and makes the round trip with `FileName.make` explicit.

## Acceptance criteria

- [ ] `FileName.parse(FileName.make(...))` round-trips for titles containing `-`.
- [ ] Existing `FileNameTest` cases still pass, plus new cases for dashed titles.
- [ ] Consolidate groups a dashed-title story's chapters together.

## Notes

`FileName.make` also emits `"<Story> - Ch 1.epub"` for a one-shot, which `parse` then reads
as a partial download. Worth deciding whether one-shots should get the bare
`"<Story>.epub"` form while fixing this.
