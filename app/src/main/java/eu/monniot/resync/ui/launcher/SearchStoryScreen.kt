package eu.monniot.resync.ui.launcher

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue


@Composable
fun SearchStoryScreen() {
    Column {

        val storyId = mutableStateOf(TextFieldValue())
        val chapter = mutableStateOf(TextFieldValue())

        Text("Story Id")
        TextField(
            value = storyId.value,
            onValueChange = { storyId.value = it },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Text("Specific chapter (optional)")
        TextField(
            value = chapter.value,
            onValueChange = { chapter.value = it },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Button(onClick = { /*TODO*/ }) {
            Text("Sync (TODO)")
        }
    }
}