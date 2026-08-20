# Comments contradict the code they describe

**Priority:** trivial
**Area:** `app/src/main/java/eu/monniot/resync/ui/downloader/DownloadScreen.kt`

## Problem

- `DownloadScreen.kt:317` and `:351`: "Let's wait 5 seconds between each chapter" — the code is
  `delay(1000)` (lines 320 and 354), one second. Both copies of the comment, in the `All` and
  `Range` branches.
- `DownloadScreen.kt:384`: "Wait 90 seconds before trying again" — the loop on the next line is
  `for (time in 60 downTo 1)`, 60 seconds.
- `DownloadScreen.kt:754`: `DownloadingRemainingChaptersNoticePreview` hardcodes
  `"AO3 rate limit hit (1 time)\nWaiting 90sec before resuming download."`, which matches the
  comment rather than the behaviour. The real string comes from `ao3RLNotice` (lines 397-405) and
  reads `"AO3 rate limit hit (once)\nWaiting 60 seconds before resuming download."` — so the
  preview also misrepresents the pluralisation ("1 time" vs "once") and the format
  ("90sec" vs "60 seconds").

## Proposed fix

Name the constants and drop the numbers from prose:

```kotlin
private const val AO3_INTER_CHAPTER_DELAY_MS = 1_000L
private const val RATE_LIMIT_BACKOFF_SECONDS = 60
```

Use them at lines 320, 354 and 385, and reduce the three comments to *why* the delay exists (the
"looks like a human reading" rationale is the useful part) without restating the duration.

Change the preview at line 754 to call `ao3RLNotice(1, 47)` rather than hardcoding a string, so it
can't drift again. (47 seconds matches the value drawn in the design's "Downloading" frame.)

Worth doing alongside the `// TODO Factor together All and Range as they are very similar` at
`DownloadScreen.kt:289`, since those two branches are near-identical copies.

Note the line numbers above are current as of the pre-redesign code; the `redesign-*` tickets move
this file around. Do this before them, or re-locate by content.

## Acceptance criteria

- [ ] No comment in `DownloadScreen.kt` states a duration the code does not use.
- [ ] The inter-chapter delay and the rate-limit backoff are each named constants, referenced at
      every use site rather than repeated as literals.
- [ ] `DownloadingRemainingChaptersNoticePreview` renders a string produced by `ao3RLNotice`, not a
      hardcoded one.
- [ ] `./gradlew test` passes.
