package eu.monniot.resync.ui.launcher

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import eu.monniot.resync.downloader.ChapterId
import eu.monniot.resync.downloader.DriverType
import eu.monniot.resync.downloader.StoryId
import eu.monniot.resync.ui.ReSyncTheme
import eu.monniot.resync.ui.downloader.DownloadScreen


@Composable
fun SearchStoryScreen() {

    val storyId = remember { mutableStateOf(TextFieldValue("34804678")) }
    val chapterId = remember { mutableStateOf(TextFieldValue("86665087")) }
    val driverType = remember { mutableStateOf(DriverType.ArchiveOfOurOwn) }
    val storySelected = remember { mutableStateOf(false) }

    if (storySelected.value) {
        // The `toInt` calls are safe because the input have a number keyboard on them
        // Hopefully.
        val sid = StoryId(storyId.value.text.toInt())
        val cid = ChapterId(chapterId.value.text.ifBlank { null }?.toInt())
        DownloadScreen(
            driverType = driverType.value,
            storyId = sid,
            chapterId = cid,
            onDone = { storySelected.value = false }
        )
    } else {
        StorySelectionView(storyId, chapterId, driverType) {
            storySelected.value = true
        }
    }
}

@Composable
fun StorySelectionView(
    storyId: MutableState<TextFieldValue>,
    chapterId: MutableState<TextFieldValue>,
    driverType: MutableState<DriverType>,
    onClick: () -> Unit,
) {
    val expanded = remember { mutableStateOf(false) }

    Column {
        Text("Story Id")
        TextField(
            value = storyId.value,
            onValueChange = { storyId.value = it },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Text("Chapter id (optional)")
        TextField(
            value = chapterId.value,
            onValueChange = { chapterId.value = it },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Text("Provider")
        DropdownMenu(
            expanded = expanded.value,
            onDismissRequest = { expanded.value = false },
        ) {
            DropdownMenuItem(onClick = {
                driverType.value = DriverType.ArchiveOfOurOwn
                expanded.value = false
            }) {
                Text("Archive of our Own")
            }

            DropdownMenuItem(onClick = {
                driverType.value = DriverType.FanFictionNet
                expanded.value = false
            }) {
                Text("FanFiction.Net")
            }
        }

        Text("Note that FF.Net has recently changed their Cloudflare protection plan" +
                " and as such, using that provider might crash the app. If that is the" +
                " case, then retrying won't help. Hopefully that is something we can fix.")

        Button(onClick = onClick) {
            Text("Sync")
        }
    }
}

@Preview
@Composable
fun SearchStoryScreenPreview() {
    ReSyncTheme {
        SearchStoryScreen()
    }
}
