# CI only runs unit tests

**Priority:** medium
**Area:** `.github/workflows/ci.yml`

## Problem

The workflow runs `./gradlew test` and nothing else. Nothing catches:

- a broken assembly (`assembleDebug` is never run),
- Android lint findings (`lint` is never run — and there is no other linter in the project),
- a broken release variant, which is doubly likely given the machine-specific signing config
  (`docs/tickets/release-build-configuration.md`).

Separately, `CLAUDE.md` documents a JDK 21 daemon toolchain via
`gradle/gradle-daemon-jvm.properties`; that file does not exist, and CI pins JDK 17. The doc
and the repo should agree.

## Proposed fix

- Add `./gradlew assembleDebug` and `./gradlew lint` steps (or fold into a single
  `./gradlew build`).
- Upload the lint report as an artifact.
- Either add `gradle/gradle-daemon-jvm.properties` or correct `CLAUDE.md`.
- Consider `actions/setup-java` + `gradle/actions/setup-gradle` for caching instead of the
  hand-rolled `actions/cache` block.

## Acceptance criteria

- [ ] CI fails on a compile error in any variant.
- [ ] CI reports lint findings.
- [ ] The documented JDK setup matches reality.
