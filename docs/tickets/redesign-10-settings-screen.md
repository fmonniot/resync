# Rebuild the Settings screen on M3 components

**Priority:** medium
**Area:** `app/src/main/java/eu/monniot/resync/ui/launcher/SettingsScreen.kt`
**Depends on:** [redesign-00-dependency-and-theme.md](redesign-00-dependency-and-theme.md)

## Design reference

Design project `274c396b-cc57-4eb1-8e13-4bea2287765d`, `reSync - Calm Reader v2.dc.html`, frame
**"Settings"**.

Four sections, each a `labelLarge`/`primary` header with `16dp` horizontal / `16dp` top / `8dp`
bottom padding, followed by its rows:

| Section | Row title | Row subtitle | Trailing |
|---|---|---|---|
| Account | Sign in to reMarkable Cloud | Not signed in | `chevron_right` |
| Sync | Sync frequency | Manual | `chevron_right` |
| Storage | Local storage | 128 MB used | `chevron_right` |
| reMarkable Cloud | *(a card, not a row — see below)* | | |

Rows are `min-height: 72dp`, `8dp` vertical / `16dp` horizontal padding, title
`bodyLarge`/`onSurface`, subtitle `bodyMedium`/`onSurfaceVariant`, trailing `chevron_right` at 24dp
in `onSurfaceVariant`. There are **no dividers** between sections.

The "Settings" top app bar belongs to
[redesign-01-scaffold-and-navigation.md](redesign-01-scaffold-and-navigation.md).

## Decision (resolved — do not re-open)

**Add the Account / Sync / Storage rows as inert placeholders**, matching the design exactly,
including the literal `"128 MB used"` string. They have no backing functionality until the cloud
integration is rebuilt. Each gets `onClick = {}` and a comment naming this ticket and
`CLAUDE.md` § reMarkable Cloud, so the next person can tell a placeholder from an unfinished
feature.

## Problem

Per `CLAUDE.md`, `SettingsScreen.kt` is the anchor point for a future reMarkable Cloud
reimplementation; `SettingsGroup`/`SettingsMenuLine`/`SettingsTileTexts` etc. (lines 39-147) are
kept deliberately as the building blocks the old account list used. They need M3 tokens, and three
structural fixes beyond a token swap:

1. **The 64dp left inset.** `SettingsGroup`'s header (line 54) and `SettingsMenuLine`'s texts
   (line 92) both pad start by `(16 + 40 + 8).dp` — an inset for a leading icon that no longer
   exists. The design puts headers and row text at a plain 16dp. Remove the inset from both.
2. **Row height.** `SettingsMenuLine` is a fixed `height(48.dp)` (line 80). The design's rows are
   `min-height: 72dp` with 8dp vertical padding — swap to `heightIn(min = 72.dp)` so two-line rows
   aren't clipped.
3. **The divider.** `SettingsGroup` draws a `Divider` above every group title (line 48). The design
   has none. Delete it rather than porting it to `HorizontalDivider`.

## Proposed fix

### 1. Token swaps

| Location | From | To |
|---|---|---|
| `SettingsGroup` title, lines 59-61 | `MaterialTheme.colors.primary`, `typography.subtitle2` | `MaterialTheme.colorScheme.primary`, `typography.labelLarge` |
| `SettingsTileTitle`, line 107 | `typography.subtitle1` | `typography.bodyLarge` |
| `SettingsTileSubtitle`, lines 113-118 | `typography.caption` + `LocalContentAlpha`/`ContentAlpha.medium` | `typography.bodyMedium` + `color = MaterialTheme.colorScheme.onSurfaceVariant` |

`LocalContentAlpha` and `ContentAlpha` are M2-only and have no M3 equivalent — M3 expresses the
same thing through the `onSurfaceVariant` color role, so the `CompositionLocalProvider` wrapper
disappears entirely rather than being ported. `ProvideTextStyle` exists in M3 and can stay.

Replace `SettingsGroup`'s fixed `height(32.dp)` Box (line 53) with the design's
`padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp)`.

### 2. Trailing chevron

`SettingsMenuLine` already has an `action` slot (line 74) that nothing uses. Pass
`Icons.AutoMirrored.Rounded.KeyboardArrowRight` tinted `onSurfaceVariant` into it for the three
placeholder rows. `SettingsTileAction`'s 64dp box (lines 140-147) is wider than the design's
24dp icon at 16dp end padding — narrow it to `size(48.dp)`.

The icon comes from
[redesign-11-material-symbols-icons.md](redesign-11-material-symbols-icons.md), but
`KeyboardArrowRight` is already in `material-icons-core`, so this does not need to wait.

### 3. Placeholder rows

Add three `SettingsGroup` blocks above the existing one, each with a single `SettingsMenuLine`
carrying the copy from the table at the top of this ticket, the trailing chevron, and:

```kotlin
// Placeholder — no backing functionality yet. See
// docs/tickets/redesign-10-settings-screen.md; wire up when the cloud
// integration lands (CLAUDE.md § reMarkable Cloud).
onClick = {},
```

### 4. reMarkable Cloud card

Keep the existing `SettingsGroup("reMarkable Cloud")` wrapper and replace its `Box` + single `Text`
(lines 25-34) with a card at 16dp horizontal / 16dp bottom margin:

```kotlin
Card(
    colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    ),
    shape = RoundedCornerShape(12.dp),
    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
) {
    Column(Modifier.padding(16.dp)) {
        Text("Direct integration is being rebuilt", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(4.dp))
        Text(
            "Use the Share sheet after downloading a story to send it to reMarkable for now.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
```

**Use the design's copy above, not today's.** Today the screen has one run-on sentence
("Direct reMarkable Cloud integration is being rebuilt. For now, use the Share sheet after
downloading a story to send it to the reMarkable app."); the design splits it into a title line and
a body line, which is the point of the restyle. The same body sentence is used by
[redesign-08-consolidate-list.md](redesign-08-consolidate-list.md)'s no-account state — keep the two
byte-identical.

### 5. Preview

`SettingsScreenPreview` (lines 149-159) wraps in bare `MaterialTheme { }` — M2, no dark-theme
parameter — unlike every other screen's preview. Switch it to `ReSyncTheme` and add a
`ReSyncTheme(darkTheme = true)` variant.

## Acceptance criteria

- [ ] `SettingsScreen.kt` imports `androidx.compose.material3.*` and contains no
      `androidx.compose.material.*` import.
- [ ] `LocalContentAlpha`, `ContentAlpha` and `Divider` no longer appear in the file.
- [ ] `(16 + 40 + 8).dp` no longer appears in the file; headers and row text start at 16dp.
- [ ] `SettingsMenuLine` uses `heightIn(min = 72.dp)`, not a fixed 48dp.
- [ ] Account, Sync and Storage sections render with the exact copy in the table above, each with a
      trailing chevron and an `onClick = {}` carrying the placeholder comment.
- [ ] The reMarkable Cloud card is a `surfaceContainerHighest` 12dp card with a `bodyLarge` title
      line and a `bodyMedium`/`onSurfaceVariant` body line.
- [ ] The card's body sentence is byte-identical to the no-account subtitle in
      [redesign-08](redesign-08-consolidate-list.md) (grep for it; two hits, or one if ticket 08
      hasn't landed).
- [ ] `SettingsScreenPreview` uses `ReSyncTheme`, and a dark variant exists; both render without
      error and show all four sections.
- [ ] `./gradlew assembleDebug` and `./gradlew lint` pass.
