# Greedy <hr> regex deletes chapter content

**Priority:** high — silent content loss in generated epubs
**Area:** `Epub.kt`

## Problem

`Epub.kt:79` normalises reMarkable-unfriendly rules with:

```kotlin
.replace(Regex("<hr.+>"), "<hr/>")
```

`.+` is greedy and `.` matches everything except newlines, so on a line like

```html
<hr size="1" noshade> she turned away <em>slowly</em>
```

the match runs from `<hr` to the last `>` on that line and the whole span collapses to
`<hr/>`. Everything in between is dropped from the epub. jsoup's `.html()` output puts a
fair amount of inline markup on a single line, so the blast radius is real.

The neighbouring `.replace("<br>", "<br/>")` on the same chain only handles the exact
lowercase, attribute-free form; `<br >`, `<br/>` variants and `<BR>` slip through.

## Proposed fix

- `Regex("<hr[^>]*>", RegexOption.IGNORE_CASE)`
- Same treatment for the `<br>` normalisation.

## Acceptance criteria

- [ ] A chapter with `<hr size="1" noshade>` followed by inline markup on the same line
      keeps all of its text.
- [ ] Unit tests over `Book.addChapter` (or an extracted pure `sanitiseContent` function)
      covering `<hr>`, `<hr …>`, `<br>`, `<br />`, uppercase variants, and multiple rules on
      one line.

## Notes

Fixing this cleanly probably means extracting the string cleanup out of `Book.addChapter`
into a pure function, which also removes the `android.text.TextUtils` dependency from the
tested path. See `docs/tickets/pure-logic-test-coverage.md`.
