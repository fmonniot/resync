# Tickets

One file per piece of work. Each ticket states the problem with file references, a proposed
fix, and acceptance criteria. Priorities are relative to each other, not absolute.

## Critical

- [Sync 1.5 upload truncates the reMarkable root index](sync15-root-index-truncation.md) —
  `parseIndex` is a stub, so a 1.5 upload rewrites the root with only the new document.

## High

- [BlobDoc.withEntry builds the wrong container entry](blobdoc-entry-hash.md)
- [Newly paired account is never selected as current](first-run-account-index.md)
- [Download reports success when no upload happened](silent-upload-success.md)
- [Greedy `<hr>` regex deletes chapter content](epub-hr-regex-greedy.md)
- [FileName.parse fails on titles containing a dash](filename-parse-dashes.md)

## Medium

- [Driver is recreated on every recomposition](remember-driver-instance.md)
- [WebView contents debugging enabled in release builds](webview-debugging-release-builds.md)
- [HTTP logging writes tokens to logcat](http-logging-leaks-tokens.md)
- [Moshi reflection + R8 will break JSON parsing in release](moshi-r8-keep-rules.md)
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

`epub-hr-regex-greedy`, `filename-parse-dashes`, `silent-upload-success` and
`first-run-account-index` are small, self-contained and testable — good first cut.
`sync15-root-index-truncation` and `blobdoc-entry-hash` should be done together, and
`pure-logic-test-coverage` is what makes both of them verifiable.
