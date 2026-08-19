# Download reports success when no upload happened

**Priority:** high — user-visible incorrect state
**Area:** `ui/downloader/DownloadScreen.kt`

## Problem

In `downloadLogic` (`DownloadScreen.kt:324`):

```kotlin
if (tokens == null) {
    // TODO save epub for later and display it in the LauncherActivity
} else {
    ... actually upload ...
}

setState(DownloadState.Done)
```

The null-token branch is empty, and `Done` is set unconditionally afterwards, so the user
sees "The story is now available on your tablet" while nothing was uploaded. The activity
then closes. Combined with `docs/tickets/first-run-account-index.md`, this is the default
experience for a freshly paired account.

The same shape appears in `ConsolidateViewModel.refreshDocuments`
(`ui/launcher/ConsolidateScreen.kt:316`), where the no-account branch is an empty block
with a comment.

## Proposed fix

Add a `DownloadState` for "no reMarkable account configured" and set it instead of `Done`.
The screen should tell the user what happened and point at Settings (pair an account, or
switch to the Share upload method). Only reach `Done` after a successful upload or share.

## Acceptance criteria

- [ ] With no tokens configured, the download screen shows an actionable message, not
      the success message.
- [ ] `Done` is only set on a genuinely completed upload.
- [ ] The Consolidate refresh path surfaces the same condition instead of silently no-oping.
