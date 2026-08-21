package eu.monniot.resync.ui.launcher

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.automirrored.rounded.LibraryBooks
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Devices
import eu.monniot.resync.ui.ReSyncTheme


// Nav bar approximates a FILL 0 -> FILL 1 swap on the active item (Material Symbols variable
// font) using the Outlined/Rounded style families, since material-icons-extended has no
// variable-fill glyphs. Consolidate deliberately keeps the books metaphor here rather than a
// `sync` glyph: reusing `sync` for the nav bar would collide with its meaning as the Search
// screen's primary action.
enum class LauncherScreenItem(
    val sectionName: String,
    val topBarTitle: String,
    val iconOutline: ImageVector,
    val iconFilled: ImageVector,
) {
    Search("Search", "reSync", Icons.Outlined.Search, Icons.Rounded.Search),
    Consolidate(
        "Consolidate",
        "Documents",
        Icons.AutoMirrored.Outlined.LibraryBooks,
        Icons.AutoMirrored.Rounded.LibraryBooks,
    ),
    Settings("Settings", "Settings", Icons.Outlined.Settings, Icons.Rounded.Settings)
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
                    val itemSelected = selectedItem == item
                    NavigationBarItem(
                        selected = itemSelected,
                        onClick = { selectedItem = item },
                        icon = {
                            // The pill indicator behind this icon animates for free via
                            // NavigationBarItem (no colors override suppressing it). This
                            // AnimatedContent only handles the outline<->filled icon crossfade on
                            // top of it.
                            AnimatedContent(
                                targetState = itemSelected,
                                transitionSpec = {
                                    fadeIn(tween(150)) togetherWith fadeOut(tween(150))
                                },
                                label = "navIcon",
                            ) { selected ->
                                Icon(
                                    if (selected) item.iconFilled else item.iconOutline,
                                    contentDescription = null,
                                )
                            }
                        },
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
