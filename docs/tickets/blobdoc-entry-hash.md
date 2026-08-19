# BlobDoc.withEntry builds the wrong container entry

**Priority:** high
**Area:** `rmcloud/`

## Problem

`rmcloud/RmClient.kt:359`:

```kotlin
fun withEntry(entry: Entry): BlobDoc =
    BlobDoc(files + entry, entry.copy(hash = hashEntries(files)))
```

Two things are wrong:

1. `hashEntries(files)` hashes the file list *before* the new entry was appended, so the
   container hash always lags one entry behind.
2. `entry.copy(...)` replaces the container entry with the *file's* entry — its
   `documentId`, `type` and `size` overwrite the document's own. After the fold in
   `uploadEpub15` (`RmClient.kt:205`), the document entry carries the last uploaded file's
   identity instead of the document id generated at the top of the function.

## Proposed fix

Keep the document entry's identity and recompute only the hash (and, if the protocol wants
it, the size) from the updated file list:

```kotlin
fun withEntry(newEntry: Entry): BlobDoc {
    val updated = files + newEntry
    return BlobDoc(updated, entry.copy(hash = hashEntries(updated)))
}
```

## Acceptance criteria

- [ ] The container entry keeps the document id it was created with.
- [ ] The container hash matches `hashEntries` of the final file list.
- [ ] Unit test folding several entries and asserting both properties.

## Related

- `docs/tickets/sync15-root-index-truncation.md`
