# Release build config is machine-specific and unversioned

**Priority:** medium
**Area:** `app/build.gradle`

## Problem

```groovy
signingConfigs {
    release {
        storeFile file('/Users/<owner>/AndroidSigningKeystoreDebug')
        storePassword 'android-signing-keystore-debug'
        ...
    }
}
```

- The keystore path is absolute and machine-specific, so `assembleRelease` fails on any
  other machine (including CI).
- The store/key passwords are committed to the repository.
- `versionCode 1` / `versionName "1.0"` have never been bumped, so no two builds are
  distinguishable.

## Proposed fix

- Read keystore path and credentials from `~/.gradle/gradle.properties` or environment
  variables, and skip/replace the signing config when they are absent so the release
  variant at least *builds* elsewhere.
- Rotate the committed credentials if that keystore is used for anything real.
- Introduce a versioning scheme (manual bump, or derive from git tag / commit count).

## Acceptance criteria

- [ ] `./gradlew assembleRelease` succeeds on a machine without the owner's keystore
      (unsigned or debug-signed).
- [ ] No credentials in version control.
- [ ] `versionCode` changes between releases.
