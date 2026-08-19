# Comments contradict the code they describe

**Priority:** trivial
**Area:** `ui/downloader/DownloadScreen.kt`

## Problem

- `DownloadScreen.kt:254` and `:287`: "Let's wait 5 seconds between each chapter" — the code
  is `delay(1000)`, one second. (Both copies of the comment, in the All and Range branches.)
- `DownloadScreen.kt:406`: "Wait 90 seconds before trying again" — the loop is
  `for (time in 60 downTo 1)`, 60 seconds.
- `DownloadingRemainingChaptersNoticePreview` renders "Waiting 90sec…", matching the comment
  rather than the behaviour.

## Proposed fix

Make the comments match the constants, or better, name the constants
(`AO3_INTER_CHAPTER_DELAY`, `RATE_LIMIT_BACKOFF`) and drop the numbers from prose.

Worth doing alongside the All/Range de-duplication TODO at `DownloadScreen.kt:227`, since
these two branches are near-identical copies.

## Acceptance criteria

- [ ] No comment states a duration the code does not use.
