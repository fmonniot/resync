# Rebuild the Settings screen scaffolding on M3 components

**Priority:** medium
**Area:** `app/src/main/java/eu/monniot/resync/ui/launcher/SettingsScreen.kt`
**Depends on:** [redesign-00-dependency-and-theme.md](redesign-00-dependency-and-theme.md)

## Design reference

Design project `274c396b-cc57-4eb1-8e13-4bea2287765d`, `reSync - Calm Reader v2.dc.html`, frame
"Settings".

## Problem / scope note

Per `CLAUDE.md`, `SettingsScreen.kt` is explicitly the placeholder anchor point for a future
reMarkable Cloud reimplementation — `SettingsGroup`/`SettingsMenuLine`/`SettingsTileTexts` etc.
(`SettingsScreen.kt:39-147`) are described there as "the reusable building blocks the old account
list used and are still there," kept intentionally for that future work.

The mock's Settings frame shows populated sections — "Account" (Sign in to reMarkable Cloud),
"Sync" (Sync frequency), "Storage" (Local storage: 128 MB used) — that **do not correspond to any
current functionality**; those rows have no backing `onClick` behavior to wire up today. This
ticket is scoped to **restyling the existing building blocks and the one real placeholder card**
("Direct integration is being rebuilt..."), not to adding the Account/Sync/Storage rows as
functional UI — that's the future cloud-reimplementation ticket's job, referenced but out of
scope here. Do add those rows to the screen (matching the mock, since it's directly relevant
reference for whoever builds that later), but leave them visually present and non-interactive
(no `onClick`, or a no-op), clearly distinguishable from adding real settings functionality.

## Proposed fix

1. **`SettingsGroup`** (`SettingsScreen.kt:40-67`): group title styling
   (`MaterialTheme.colors.primary` + `subtitle2`, lines 59-61) → M3
   (`MaterialTheme.colorScheme.primary` + `labelLarge`, matching the mock's 14/20/500 "Account"/
   "Sync"/"Storage"/"reMarkable Cloud" section headers). Replace the M2 `Divider`
   (`SettingsScreen.kt:48`) with M3 `HorizontalDivider`.

2. **`SettingsMenuLine`/`SettingsTileTexts`/`SettingsTileTitle`/`SettingsTileSubtitle`**
   (lines 69-137): these already roughly match the mock's two-line row shape (title + optional
   subtitle, 48-72dp height) — this is mostly a token swap
   (`MaterialTheme.typography.subtitle1` → `bodyLarge`, `.caption` → `bodyMedium`,
   `LocalContentAlpha`/`ContentAlpha.medium` → `MaterialTheme.colorScheme.onSurfaceVariant`, no
   alpha trick needed in M3's color-role system). Add a trailing `chevron_right` icon
   (`Icons.Rounded.KeyboardArrowRight` variant, `onSurfaceVariant`) to rows that navigate
   somewhere, matching the mock's Account/Sync/Storage rows — `SettingsMenuLine`'s `action`
   param (line 74) already supports this, just isn't used anywhere yet.

3. **Reference-only rows.** Add `SettingsGroup("Account")`, `SettingsGroup("Sync")`,
   `SettingsGroup("Storage")` blocks using `SettingsMenuLine` with the mock's copy
   ("Sign in to reMarkable Cloud" / "Not signed in", "Sync frequency" / "Manual", "Local storage" /
   "128 MB used") and the trailing chevron from step 2, `onClick = {}` (no-op) — comment each with
   something like `// Placeholder — see docs/tickets/redesign-06-settings-screen.md; wire up once
   the cloud integration lands (CLAUDE.md § reMarkable Cloud)`.

4. **"reMarkable Cloud" placeholder card** (`SettingsScreen.kt:24-35`): keep the existing copy,
   restyle as the mock's `surfaceContainerHighest` rounded-12dp card with a `bodyLarge` title line
   ("Direct integration is being rebuilt") and `bodyMedium` `onSurfaceVariant` body line — today
   it's a single unstyled `Text` block.

## Acceptance criteria

- [ ] `SettingsGroup`/`SettingsMenuLine` family uses M3 typography and color roles throughout.
- [ ] Account/Sync/Storage rows are present and visually match the mock, but are clearly
      non-functional (commented as placeholders referencing this ticket and `CLAUDE.md`).
- [ ] The reMarkable Cloud card uses the tonal `surfaceContainerHighest` treatment with
      title/body text split, matching the mock.
- [ ] `SettingsScreenPreview` (`SettingsScreen.kt:149-159`) — note it currently wraps in plain
      `MaterialTheme { ... }` (M2, no dark-theme param) rather than `ReSyncTheme` like every other
      screen's preview; switch it to `ReSyncTheme` while touching this file so light/dark both
      preview correctly, and add a dark variant alongside it.
