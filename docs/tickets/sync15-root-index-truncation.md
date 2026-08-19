# Sync 1.5 upload truncates the reMarkable root index

**Priority:** critical — potential data loss on the user's account
**Area:** `rmcloud/`

## Problem

`parseIndex` in `rmcloud/RmClient.kt:398` is a stub: the loop that would parse each
entry line is commented out (`//sequence.forEach { entries.add(parseEntry(it)) }`),
and the function always returns an empty list. `parseEntry` itself is commented out
entirely just below.

`getRootIndex` (`RmClient.kt:287`) feeds that result into `uploadEpub15`, which then does:

```kotlin
val newRootIndex = rootIndex + blobDoc.entry   // rootIndex is always empty
val newRootIndexContent = buildIndex(newRootIndex)
```

The root index written back to the cloud therefore contains only the document that was
just uploaded. Every other document on the account is dropped from the tree.

The `ExperimentalRmUpload` confirmation screen is currently the only thing standing between
a 1.5 account and this.

## Proposed fix

Implement `parseEntry` / `parseIndex` against the schema-3 line format already produced by
`Entry.line` (`hash:0:documentId:0:size`), so a round-trip `buildIndex(parseIndex(x)) == x`
holds. Reject unknown schema versions as it does today.

Until that is done, `uploadEpub15` should fail loudly rather than write a truncated root.

## Acceptance criteria

- [ ] `parseIndex` returns the real entries of a schema-3 index.
- [ ] Unit tests covering: schema-3 round trip, unknown schema version, empty index,
      malformed line.
- [ ] `uploadEpub15` preserves pre-existing entries in the new root index.

## Related

- `docs/tickets/blobdoc-entry-hash.md`
