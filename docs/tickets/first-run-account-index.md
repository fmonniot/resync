# Newly paired account is never selected as current

**Priority:** high — breaks direct upload on first run
**Area:** `rmcloud/PreferencesManager.kt`

## Problem

`addAccount` (`PreferencesManager.kt:105`) increments `ACCOUNT_NUMBERS` and writes the
account fields, but never writes `ACCOUNT_INDEX`. Nothing else in the pairing flow does
either — `SetupRemarkableStateMachine.kt:104` only calls `addAccount`; `changeCurrentAccount`
is wired solely to the settings screen's account picker.

The two readers also disagree on the default:

- `listAccounts` (`:63`) defaults `ACCOUNT_INDEX` to `-1`
- `readCurrentAccount` (`:95`) defaults it to `0`, while account indices are 1-based

So after pairing, `readCurrentAccount()` looks up `accountName-0` / `deviceToken-0`, which
were never written, and returns an account with `tokens == null`. Direct upload then does
nothing (see `docs/tickets/silent-upload-success.md`). The user has to go into Settings and
re-select the account they just added.

## Proposed fix

- Set `ACCOUNT_INDEX` in `addAccount` when it is the first account (or unconditionally,
  matching "the account you just paired becomes active").
- Agree on one sentinel for "no account selected" across `listAccounts` and
  `readCurrentAccount`, and have `readCurrentAccount` return null / a typed absence rather
  than a fabricated `Account` with a bogus index.

## Acceptance criteria

- [ ] Pairing an account from a clean install leaves it selected.
- [ ] `readCurrentAccount` and `listAccounts` use the same "not selected" value.
- [ ] Callers handle the no-account case explicitly.
