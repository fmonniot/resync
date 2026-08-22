# Tickets

One file per piece of work. Each ticket states the problem with file references, a proposed
fix, and acceptance criteria. Priorities are relative to each other, not absolute. Completed
tickets are deleted from this directory once merged — see git history for anything from the past.

## High

- [Add Robolectric Compose UI testing, proven on ConfirmChapters](compose-ui-test-infra-and-confirm-chapters.md)

## Medium

- [Nav bar stays visible over full-screen download states reached from Search](nav-bar-over-search-download.md)
- [Cover DownloadScreen's remaining state composables](download-screen-remaining-state-composables-coverage.md)
- [Cover StorySelectionView and ConsolidateView with Compose UI tests](search-and-consolidate-screen-compose-coverage.md)
- [Cover Driver.readChapter's network-fetch and rate-limit-retry path](driver-readchapter-network-path-coverage.md)

## Low / trivial

- [Comments contradict the code they describe](comment-drift.md)
- [Exclude @Preview-only composables from the coverage report](exclude-preview-composables-from-coverage.md)
- [Unit test errorDetailsText](error-details-text-coverage.md)
- [JaCoco reports 0% on Activity subclasses despite passing Robolectric tests](activity-subclass-jacoco-blindspot.md)
