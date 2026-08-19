# Moshi reflection + R8 will break JSON parsing in release

**Priority:** medium — release-only failure mode
**Area:** `app/build.gradle`, `app/proguard-rules.pro`, `rmcloud/`

## Problem

The reMarkable API models (`RegistrationPayload`, `Document`, `UploadRequestPayload`,
`BlobStorageRequest/Response`, `JwtPayload`, …) are plain Kotlin data classes parsed by
`Moshi.Builder().build()` — the reflective `ClassJsonAdapter` path. Neither `moshi-kotlin`
nor `moshi-kotlin-codegen` is a dependency, so field names come straight from the JVM
fields.

`minifyEnabled true` is on for release, and `proguard-rules.pro` has no keep rules for these
classes. Once R8 renames their fields, JSON keys stop matching. The nastiest case is
`Tokens.scopes` / `is15Account()` (`PreferencesManager.kt`): a failure there does not throw
anything obvious, it just reports the account as sync 1.0 and takes the wrong upload path.

## Proposed fix

Pick one:

- add `moshi-kotlin-codegen` via KSP (already applied in the project) and annotate the
  models `@JsonClass(generateAdapter = true)`; or
- add explicit `-keep class eu.monniot.resync.rmcloud.** { <fields>; }` rules and `@Keep`.

Codegen is preferable: it also fixes reflective Moshi's known problems with Kotlin default
values and non-null constructor parameters.

## Acceptance criteria

- [ ] A minified release build parses the pairing response, the document list, and the JWT
      payload correctly.
- [ ] `is15Account()` returns the same answer in debug and release for the same token.

## Notes

Worth checking at the same time whether the default AGP rules keep the
`@JavascriptInterface` method in `Driver.Companion.JsInterface` — the keep block in
`proguard-rules.pro` for that is present but commented out. If scraping works in a release
build today, it is relying on the default rules; make that explicit.
