# HTTP logging writes reMarkable tokens to logcat in release builds

**Priority:** medium — security
**Area:** `rmcloud/RmClient.kt`, `rmcloud/utilities.kt`

## Problem

Three related issues:

1. `exchangeCodeForDeviceToken` (`RmClient.kt:17`) installs a plain
   `HttpLoggingInterceptor` at `Level.BODY`, unconditionally. The response body of
   `/token/json/2/device/new` *is* the device token, so pairing writes the long-lived
   device token to logcat.
2. `RmClient`'s own interceptor (`RmClient.kt:40`) is also `Level.BODY` in all build types.
   Even the requests tagged `"no body logging"` still log the
   `Authorization: Bearer <user token>` header at `HEADERS` level.
3. `FilteredLoggingInterceptor` (`rmcloud/utilities.kt`) *mutates* the shared logger's level
   when it sees the tag and never restores it, so the filter is order-dependent and sticky
   rather than per-request.

`CLAUDE.md` currently claims tokens never leave `PreferencesManager` in plaintext logs. That
is not true today; the doc should be corrected along with the code.

## Proposed fix

- Gate the log level on `BuildConfig.DEBUG` (`NONE` in release, `BODY`/`HEADERS` in debug).
- Use `HttpLoggingInterceptor.redactHeader("Authorization")`.
- Make the filtering per-request: build two interceptor instances, or snapshot/restore the
  level around the `chain.proceed` call, instead of assigning to a shared field.
- Factor the OkHttp client construction into one place (there is already a
  `// TODO Creating OkHttp client should be a common function` at `RmClient.kt:14`).

## Acceptance criteria

- [ ] No token material appears in logcat for a release build during pairing or upload.
- [ ] `Authorization` headers are redacted even in debug logs.
- [ ] The logging level is not mutated globally at request time.
- [ ] `CLAUDE.md` matches the resulting behaviour.
