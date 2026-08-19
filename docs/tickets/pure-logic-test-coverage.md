# Pure logic outside the scrapers has no test coverage

**Priority:** medium
**Area:** `Epub.kt`, `FileName.kt`, `ui/downloader/DownloadScreen.kt`

## Problem

The parse-vs-fetch split in `downloader/` is the well-tested part of this codebase: HTML
fixtures under `app/src/test/resources/{ao3,ffnet}/` drive `parseWebPage` directly. Nothing
comparable exists for what happens *after* parsing:

- `makeEpub` / `Book.addChapter` / `Book.addCover` — no tests.
- `FileName.make` — no tests (`FileNameTest` only covers `parse`, and only for
  dash-free titles).
- `downloadLogic` — no tests.

Two of the bugs filed alongside this ticket
(`epub-hr-regex-greedy`, `filename-parse-dashes`) live in exactly that untested pure code.

## Proposed fix

1. Extract the HTML sanitising in `Book.addChapter` into a pure function, free of
   `android.text.TextUtils`, and test it directly.
2. Add `FileName.make` ↔ `parse` round-trip tests.
3. Act on the existing TODO at `DownloadScreen.kt:312` — inject `PreferencesManager` (and
   the upload target) into `downloadLogic` instead of constructing them from `Context`. That
   makes the whole state-machine flow — chapter selection, rate-limit retry, the
   no-account path — unit-testable with `kotlinx-coroutines-test`, which is already a
   dependency.

## Acceptance criteria

- [ ] `makeEpub`'s content transformation is tested without an Android runtime.
- [ ] `downloadLogic` can be driven in a unit test with a fake driver and fake preferences.
- [ ] The bugs listed above each land with a regression test.
