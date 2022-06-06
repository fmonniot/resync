package eu.monniot.resync.ui.launcher

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
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
import eu.monniot.resync.ui.icons.Science


enum class LauncherScreenItem(val sectionName: String, val icon: ImageVector) {
    Search("Search", Icons.Filled.Search),
    Consolidate("Consolidate", LibraryBooks),
    Experimental("Experiments", Science),
    Settings("Settings", Icons.Default.Settings)
}

@Composable
fun LauncherScreen(
    initialScreenItem: LauncherScreenItem = LauncherScreenItem.Search
) {
    var selectedItem by remember { mutableStateOf(initialScreenItem) }

    Scaffold(
        topBar = {},
        content = { padding ->
            Box(modifier = Modifier.padding(padding)) {
                when (selectedItem) {
                    LauncherScreenItem.Search -> SearchStoryScreen()
                    LauncherScreenItem.Consolidate -> ConsolidateScreen()
                    LauncherScreenItem.Experimental -> TestingSavingAnimation()
                    LauncherScreenItem.Settings -> SettingsScreen()
                }
            }
        },
        bottomBar = {
            BottomNavigation {

                BottomNavigationItem(
                    icon = { Icon(LauncherScreenItem.Search.icon, contentDescription = null) },
                    label = { Text(LauncherScreenItem.Search.sectionName) },
                    selected = selectedItem == LauncherScreenItem.Search,
                    onClick = { selectedItem = LauncherScreenItem.Search }
                )

                BottomNavigationItem(
                    icon = { Icon(LauncherScreenItem.Consolidate.icon, contentDescription = null) },
                    label = { Text(LauncherScreenItem.Consolidate.sectionName) },
                    selected = selectedItem == LauncherScreenItem.Consolidate,
                    onClick = { selectedItem = LauncherScreenItem.Consolidate }
                )

                // TODO Only in debug mode
                BottomNavigationItem(
                    icon = {
                        Icon(
                            LauncherScreenItem.Experimental.icon,
                            contentDescription = null
                        )
                    },
                    label = { Text(LauncherScreenItem.Experimental.sectionName) },
                    selected = selectedItem == LauncherScreenItem.Experimental,
                    onClick = { selectedItem = LauncherScreenItem.Experimental }
                )

                BottomNavigationItem(
                    icon = { Icon(LauncherScreenItem.Settings.icon, contentDescription = null) },
                    label = { Text(LauncherScreenItem.Settings.sectionName) },
                    selected = selectedItem == LauncherScreenItem.Settings,
                    onClick = { selectedItem = LauncherScreenItem.Settings }
                )
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
    name = "Launcher - Experimental - Pixel 3"
)
@Composable
fun LauncherExperimentalPreview() {
    ReSyncTheme {
        LauncherScreen(LauncherScreenItem.Experimental)
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
