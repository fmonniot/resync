# Consolidate dependency versions into a version catalog

**Priority:** low
**Area:** build files

## Problem

Versions are spread across `build.gradle` (root) and `app/build.gradle`, with a mix of
inline literals and local `def`s. The Kotlin toolchain is currently declared in three places
with three different numbers:

- `org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.10` (root)
- `org.jetbrains.kotlin.plugin.compose` version `2.4.10` (app)
- `com.google.devtools.ksp` version `2.3.11` (root)

Dependabot is configured for gradle and opens one PR per artifact against this layout.

## Proposed fix

Move everything to `gradle/libs.versions.toml` and reference `libs.*` aliases. Align the
Kotlin plugin versions while doing so, and confirm the KSP version matches the Kotlin
version it is built against.

## Acceptance criteria

- [ ] No version literals left in `build.gradle` files.
- [ ] Kotlin / KSP / Compose-plugin versions are mutually consistent.
- [ ] `./gradlew test assembleDebug` still passes.
