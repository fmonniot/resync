# Tickets

One file per piece of work. Each ticket states the problem with file references, a proposed
fix, and acceptance criteria. Priorities are relative to each other, not absolute.

## High

- [FileName.parse fails on titles containing a dash](filename-parse-dashes.md)

## Medium

- [WebView contents debugging enabled in release builds](webview-debugging-release-builds.md)
- [Release build config is machine-specific and unversioned](release-build-configuration.md)
- [CI only runs unit tests](ci-build-and-lint.md)
- [Pure logic outside the scrapers has no test coverage](pure-logic-test-coverage.md)

## Low / trivial

- [compose ui-tooling ships in release APKs](compose-ui-tooling-debug-only.md)
- [Consolidate dependency versions into a version catalog](gradle-version-catalog.md)
- [Remove dead code and template leftovers](remove-dead-code.md)
- [Search screen ships hardcoded ids and an unreachable provider picker](search-screen-unusable.md)
- [Chapter HTML cache is shared between drivers and never cleaned](chapter-cache-management.md)
- [Comments contradict the code they describe](comment-drift.md)

## Suggested order

`filename-parse-dashes` is small, self-contained and testable — good first cut.
