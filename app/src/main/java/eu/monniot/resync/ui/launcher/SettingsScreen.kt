package eu.monniot.resync.ui.launcher

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

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
        SettingsGroup(title = "reMarkable Cloud") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Text(
                    "Direct reMarkable Cloud integration is being rebuilt. For now, use the " +
                            "Share sheet after downloading a story to send it to the reMarkable app."
                )
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

        Divider(Modifier.padding(top=8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .padding(start = (16 + 40 + 8).dp, 8.dp, 16.dp, 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {


            val primary = MaterialTheme.colors.primary
            val titleStyle = MaterialTheme.typography.subtitle2.copy(color = primary)
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
            .height(48.dp)
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
                    .padding(start = (16 + 40 + 8).dp)
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
    ProvideTextStyle(value = MaterialTheme.typography.subtitle1) {
        title()
    }
}

@Composable
internal fun SettingsTileSubtitle(subtitle: @Composable () -> Unit) {
    ProvideTextStyle(value = MaterialTheme.typography.caption) {
        CompositionLocalProvider(
            LocalContentAlpha provides ContentAlpha.medium, content = subtitle
        )
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
        modifier = Modifier.size(64.dp),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Preview(
    showBackground = true,
    device = Devices.PIXEL_3,
    showSystemUi = true,
)
@Composable
internal fun SettingsScreenPreview() {
    MaterialTheme {
        SettingsView()
    }
}
