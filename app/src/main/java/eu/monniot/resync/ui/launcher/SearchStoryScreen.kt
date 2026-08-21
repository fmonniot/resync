package eu.monniot.resync.ui.launcher

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorySelectionView(
    storyId: MutableState<TextFieldValue>,
    chapterId: MutableState<TextFieldValue>,
    driverType: MutableState<DriverType>,
    onClick: () -> Unit,
) {
    val storyIdText = storyId.value.text
    val chapterIdText = chapterId.value.text

    Column(
        Modifier
            .fillMaxSize()
            .padding(top = 8.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
    ) {
        TextField(
            value = storyId.value,
            onValueChange = { storyId.value = it },
            label = { Text("Story ID") },
            supportingText = { Text("e.g. 39200706") },
            isError = storyIdText.isNotEmpty() && !isValidNumericId(storyIdText),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        TextField(
            value = chapterId.value,
            onValueChange = { chapterId.value = it },
            label = { Text("Chapter (optional)") },
            supportingText = { Text("e.g. 4") },
            isError = !isValidOptionalNumericId(chapterIdText),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        Text(
            "Provider",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(8.dp))

        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            DriverType.entries.forEachIndexed { index, driver ->
                SegmentedButton(
                    selected = driverType.value == driver,
                    onClick = { driverType.value = driver },
                    shape = SegmentedButtonDefaults.itemShape(index, DriverType.entries.size),
                    label = { Text(driver.shortName(), maxLines = 1) },
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                "Note that FF.Net has recently changed their Cloudflare protection plan" +
                        " and as such, using that provider might crash the app. If that is the" +
                        " case, then retrying won't help. Hopefully that is something we can fix.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = onClick,
            enabled = canSyncStory(storyIdText, chapterIdText),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = Icons.Rounded.Sync,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
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

@Preview
@Composable
fun SearchStoryScreenDarkPreview() {
    ReSyncTheme(darkTheme = true) {
        SearchStoryScreen()
    }
}
