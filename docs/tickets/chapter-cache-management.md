# Chapter HTML cache is shared between drivers and never cleaned

**Priority:** low
**Area:** `downloader/`

## Problem

Both drivers point their cache at the same directory:

- `ArchiveOfOurOwnDriver.tmpChaptersFolder` → `filesDir/ffnet`
- `FanFictionNetDriver.tmpChaptersFolder` → `filesDir/ffnet`

so cached pages are keyed only by `<storyId>/<chapterId>.html` across two sites whose id
spaces overlap. A collision would be rare (ff.net chapter ids are small integers, AO3's are
8-digit), and `readChapter` does recover from a parse failure by refetching — but the
sharing is unintentional and the AO3 one-shot case writes a literal `null.html`.

Nothing ever deletes these files. Every chapter of every story ever downloaded stays in
internal storage forever.

## Proposed fix

- Give each driver its own subdirectory (`filesDir/ffnet`, `filesDir/ao3`).
- Add an eviction policy — clear a story's directory after a successful epub build, or
  age/size-bound the cache.
- Decide deliberately whether the cache should survive a completed download at all. There is
  an existing TODO in `downloadLogic` arguing *for* persistence, on the grounds that a
  rate-limited AO3 download can take a long time and losing progress is expensive. That
  argues for keeping it during a run and clearing it after.

## Acceptance criteria

- [ ] The two drivers cannot collide.
- [ ] Cached HTML does not grow without bound.
