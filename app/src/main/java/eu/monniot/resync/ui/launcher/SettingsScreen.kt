package eu.monniot.resync.ui.launcher

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import eu.monniot.resync.ui.ReSyncTheme

// Direct reMarkable Cloud pairing/account management was removed; downloaded stories
// currently go out through the Android Share sheet only. This screen is the anchor point
// for whatever account/pairing UI a future reimplementation of the cloud integration needs.
@Composable
fun SettingsScreen() {
    SettingsView()
}

@Composable
fun SettingsView() {
    Column(Modifier.padding(top = 8.dp)) {
        SettingsGroup(title = "Account") {
            SettingsMenuLine(
                title = { SettingsTileTitle { Text("Sign in to reMarkable Cloud") } },
                subtitle = { SettingsTileSubtitle { Text("Not signed in") } },
                action = {
                    Icon(
                        Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                // Placeholder — no backing functionality yet. Wire up when the cloud
                // integration lands (CLAUDE.md § reMarkable Cloud).
                onClick = {},
            )
        }

        SettingsGroup(title = "Sync") {
            SettingsMenuLine(
                title = { SettingsTileTitle { Text("Sync frequency") } },
                subtitle = { SettingsTileSubtitle { Text("Manual") } },
                action = {
                    Icon(
                        Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                // Placeholder — no backing functionality yet. Wire up when the cloud
                // integration lands (CLAUDE.md § reMarkable Cloud).
                onClick = {},
            )
        }

        SettingsGroup(title = "Storage") {
            SettingsMenuLine(
                title = { SettingsTileTitle { Text("Local storage") } },
                subtitle = { SettingsTileSubtitle { Text("128 MB used") } },
                action = {
                    Icon(
                        Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                // Placeholder — no backing functionality yet. Wire up when the cloud
                // integration lands (CLAUDE.md § reMarkable Cloud).
                onClick = {},
            )
        }

        SettingsGroup(title = "reMarkable Cloud") {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
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
        }
    }
}

@Composable
fun SettingsGroup(
    title: String, content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        // Group title

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            val primary = MaterialTheme.colorScheme.primary
            val titleStyle = MaterialTheme.typography.labelLarge.copy(color = primary)
            Text(style = titleStyle, text = title)
        }

        // Content
        content()
    }
}

@Composable
fun SettingsMenuLine(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    subtitle: (@Composable () -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onClick),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingsTileTexts(
                title = title,
                subtitle = subtitle,
                modifier = Modifier
                    .padding(start = 16.dp)
                    .fillMaxHeight()
            )
        }
        if (action != null) {

            SettingsTileAction {
                action.invoke()
            }
        }
    }
}

@Composable
internal fun SettingsTileTitle(title: @Composable () -> Unit) {
    ProvideTextStyle(value = MaterialTheme.typography.bodyLarge) {
        title()
    }
}

@Composable
internal fun SettingsTileSubtitle(subtitle: @Composable () -> Unit) {
    ProvideTextStyle(
        value = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    ) {
        subtitle()
    }
}

@Composable
internal fun RowScope.SettingsTileTexts(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    subtitle: @Composable (() -> Unit)?,
) {
    Column(
        modifier = modifier.weight(1f),
        verticalArrangement = Arrangement.Center,
    ) {
        SettingsTileTitle(title)
        if (subtitle != null) {
            Spacer(modifier = Modifier.size(2.dp))
            SettingsTileSubtitle(subtitle)
        }
    }
}

@Composable
internal fun SettingsTileAction(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.size(48.dp),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Preview(
    showBackground = true,
    device = Devices.PIXEL_3,
    showSystemUi = true,
    name = "Settings (Light)",
)
@Composable
internal fun SettingsScreenPreview() {
    ReSyncTheme {
        SettingsView()
    }
}

@Preview(
    showBackground = true,
    device = Devices.PIXEL_3,
    showSystemUi = true,
    name = "Settings (Dark)",
)
@Composable
internal fun SettingsScreenDarkPreview() {
    ReSyncTheme(darkTheme = true) {
        SettingsView()
    }
}
