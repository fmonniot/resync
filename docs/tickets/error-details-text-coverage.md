# Unit test errorDetailsText

**Priority:** trivial
**Area:** `app/src/main/java/eu/monniot/resync/ui/downloader/DownloadScreen.kt`

## Problem

`errorDetailsText` (`DownloadScreen.kt:1076-1088`) builds the "Copy error details" clipboard
payload users are asked to paste into a bug report — a provider/story/chapter table followed by
the raw stack trace. It's pure (no Compose, no Context) but untested. It has a real, easy-to-break
detail worth locking down: it deliberately reads `driverType.websiteName()`, `storyId.id`, and
`chapterId.id` rather than the value classes' own `toString()`, specifically to avoid leaking as
`StoryId(id=…)` in what a user pastes into a GitHub issue (see the function's own doc comment).

## Proposed fix

Add `errorDetailsTextTest.kt` (or a section in a shared `DownloadScreen`-logic test file)
asserting:
- The provider line uses `driverType.websiteName()`, not `driverType.toString()`.
- `chapterId.id == null` (one-shot/no-chapter case) renders as `"—"`, not `"null"`.
- A non-null chapter id renders its raw `Int`.
- The stack trace (`error.stackTraceToString()`) is appended verbatim.

## Acceptance criteria

- [ ] All four cases above are covered.
- [ ] `./gradlew test` passes.
