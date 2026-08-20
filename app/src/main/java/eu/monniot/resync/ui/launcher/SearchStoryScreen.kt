package eu.monniot.resync.ui.launcher

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import eu.monniot.resync.downloader.ChapterId
import eu.monniot.resync.downloader.DriverType
import eu.monniot.resync.downloader.StoryId
import eu.monniot.resync.ui.ReSyncTheme
import eu.monniot.resync.ui.downloader.DownloadScreen


@Composable
fun SearchStoryScreen() {

    val storyId = remember { mutableStateOf(TextFieldValue("")) }
    val chapterId = remember { mutableStateOf(TextFieldValue("")) }
    val driverType = remember { mutableStateOf(DriverType.ArchiveOfOurOwn) }
    val storySelected = remember { mutableStateOf(false) }

    if (storySelected.value) {
        // Safe: the Sync button in StorySelectionView is only enabled when
        // canSyncStory(storyId, chapterId) holds, which guarantees both fields parse as Int.
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

/**
 * True if [text] is a valid numeric id: non-empty and made up solely of digits.
 * Pure and Compose-free so it can be unit tested directly.
 */
fun isValidNumericId(text: String): Boolean = text.isNotEmpty() && text.all { it.isDigit() }

/**
 * True if [text] is a valid *optional* numeric id: either blank (not provided) or a valid
 * numeric id per [isValidNumericId]. Used for the chapter id field, which is optional.
 */
fun isValidOptionalNumericId(text: String): Boolean = text.isBlank() || isValidNumericId(text)

/**
 * Whether the Sync action can be triggered given the current story id and chapter id inputs.
 * The story id is required and must be numeric; the chapter id, if provided, must also be
 * numeric.
 */
fun canSyncStory(storyId: String, chapterId: String): Boolean =
    isValidNumericId(storyId) && isValidOptionalNumericId(chapterId)

@Composable
fun StorySelectionView(
    storyId: MutableState<TextFieldValue>,
    chapterId: MutableState<TextFieldValue>,
    driverType: MutableState<DriverType>,
    onClick: () -> Unit,
) {
    val storyIdText = storyId.value.text
    val chapterIdText = chapterId.value.text

    Column(Modifier.padding(16.dp)) {
        TextField(
            value = storyId.value,
            onValueChange = { storyId.value = it },
            label = { Text("Story Id") },
            isError = storyIdText.isNotEmpty() && !isValidNumericId(storyIdText),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        TextField(
            value = chapterId.value,
            onValueChange = { chapterId.value = it },
            label = { Text("Chapter Id (optional)") },
            isError = !isValidOptionalNumericId(chapterIdText),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text("Provider", style = MaterialTheme.typography.subtitle2)
        Column(Modifier.padding(bottom = 8.dp)) {
            DriverType.values().forEach { driver ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = driverType.value == driver,
                            onClick = { driverType.value = driver }
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = driverType.value == driver,
                        onClick = { driverType.value = driver }
                    )
                    Text(driver.websiteName())
                }
            }
        }

        Text(
            "Note that FF.Net has recently changed their Cloudflare protection plan" +
                    " and as such, using that provider might crash the app. If that is the" +
                    " case, then retrying won't help. Hopefully that is something we can fix.",
            style = MaterialTheme.typography.caption
        )

        Button(
            onClick = onClick,
            enabled = canSyncStory(storyIdText, chapterIdText),
            modifier = Modifier.padding(top = 8.dp)
        ) {
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
