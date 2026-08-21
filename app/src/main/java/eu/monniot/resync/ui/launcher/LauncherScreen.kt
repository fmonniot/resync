package eu.monniot.resync.ui.launcher

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Devices
import eu.monniot.resync.ui.ReSyncTheme
import eu.monniot.resync.ui.icons.LibraryBooks


enum class LauncherScreenItem(
    val sectionName: String,
    val topBarTitle: String,
    val icon: ImageVector,
) {
    Search("Search", "reSync", Icons.Filled.Search),
    Consolidate("Consolidate", "Documents", LibraryBooks),
    Settings("Settings", "Settings", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LauncherScreen(
    initialScreenItem: LauncherScreenItem = LauncherScreenItem.Search
) {
    var selectedItem by remember { mutableStateOf(initialScreenItem) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(selectedItem.topBarTitle) })
        },
        content = { padding ->
            Box(modifier = Modifier.padding(padding)) {
                when (selectedItem) {
                    LauncherScreenItem.Search -> SearchStoryScreen()
                    LauncherScreenItem.Consolidate -> ConsolidateScreen()
                    LauncherScreenItem.Settings -> SettingsScreen()
                }
            }
        },
        bottomBar = {
            NavigationBar {
                LauncherScreenItem.entries.forEach { item ->
                    NavigationBarItem(
                        selected = selectedItem == item,
                        onClick = { selectedItem = item },
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(item.sectionName) },
                    )
                }
            }
        }
    )
}


@Preview(
    showBackground = true,
    device = Devices.PIXEL_3,
    showSystemUi = true,
    name = "Launcher - Search - Pixel 3"
)
@Composable
fun LauncherSearchPreview() {
    ReSyncTheme {
        LauncherScreen(LauncherScreenItem.Search)
    }
}


@Preview(
    showBackground = true,
    device = Devices.PIXEL_3,
    showSystemUi = true,
    name = "Launcher - Consolidate - Pixel 3"
)
@Composable
fun LauncherConsolidatePreview() {
    ReSyncTheme {
        LauncherScreen(LauncherScreenItem.Consolidate)
    }
}


@Preview(
    showBackground = true,
    device = Devices.PIXEL_3,
    showSystemUi = true,
    name = "Launcher - Settings - Pixel 3"
)
@Composable
fun LauncherSettingsPreview() {
    ReSyncTheme {
        LauncherScreen(LauncherScreenItem.Settings)
    }
}
