# Cover DownloadScreen's remaining state composables

**Priority:** medium
**Area:** `app/src/main/java/eu/monniot/resync/ui/downloader/DownloadScreen.kt`
**Depends on:** [compose-ui-test-infra-and-confirm-chapters](compose-ui-test-infra-and-confirm-chapters.md)

## Problem

Once Compose UI testing is wired up (see the dependency ticket), `ConfirmChapters` gets covered but
the other four `DownloadState` views in the same file are still untested:

- `FetchingFirstChapterView` (`DownloadScreen.kt:598-636`) — the close button (`onCancel`).
- `DownloadingRemainingChapters` (`:959-1037`) — the conditional notice `Card` (line 1014-1034)
  only renders when `notice != null`; untested, that branch is easy to silently break.
- `DisplayDownloadError` (`:1090-1248`) — the most stateful of the four: retry (`onRetry`),
  close (`onDone`), "Copy error details" (writes `errorDetailsText`'s output to the clipboard via
  `LocalClipboard`, lines 1187-1198), and the same collapse/expand pattern as `ConfirmChapters`
  (`expanded`, lines 1105, 1217-1246) gating the technical-details `Card`.
- `DownloadSuccess` (`:1362-1444`) — "Share to reMarkable" (`onShare`) and "Done" (`onDone`).

`TaskStateHeader` (`:568-596`, shared by the first two) and `OrDivider` (`:657-674`, used inside
`ConfirmChapters`) get incidental coverage from whichever of these renders them, no separate test
needed.

## Proposed fix

Add one test file per composable (or one file covering all four, contributor's call — they're
small enough to fit together): render each with `createComposeRule()`, and assert:

- `FetchingFirstChapterView`: tapping the close icon calls `onCancel`.
- `DownloadingRemainingChapters`: the notice card is absent when `notice = null` and shows the
  exact text when set — this is the one branch most likely to regress silently.
- `DisplayDownloadError`: retry/close/copy each fire their callback; "Technical details" toggles
  the details card open/closed and its rendered text matches `errorDetailsText`'s output (that
  function itself is unit-tested separately per
  [error-details-text-coverage](error-details-text-coverage.md) — this test is about the UI wiring
  round-tripping to it, not re-deriving the format).
- `DownloadSuccess`: share/done each fire their callback.

Don't bother testing the `@Preview` functions themselves (see
[exclude-preview-composables-from-coverage](exclude-preview-composables-from-coverage.md)) — they
carry no logic, just hardcoded call sites.

## Acceptance criteria

- [ ] All four composables have at least one interaction test exercising their callbacks.
- [ ] `DownloadingRemainingChapters`' notice-present and notice-absent cases are both covered.
- [ ] `DisplayDownloadError`'s clipboard content on "Copy error details" is asserted against
      `errorDetailsText(...)`'s actual output, not a hand-duplicated string.
- [ ] `./gradlew test` passes.
- [ ] `DownloadScreen.kt`'s line coverage in `jacocoTestReport` visibly improves over the baseline
      left by the `ConfirmChapters` ticket.
