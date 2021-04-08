package eu.monniot.resync

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import eu.monniot.resync.rmcloud.readTokens
import eu.monniot.resync.ui.ReSyncTheme
import eu.monniot.resync.ui.SetupRemarkableScreen


class LauncherActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val tokens = mutableStateOf(readTokens(applicationContext))

        setContent(null) {
            ReSyncTheme {
                Surface(color = MaterialTheme.colors.background) {

                    if (tokens.value == null) {
                        SetupRemarkableScreen {
                            tokens.value = it
                        }
                    } else {
                        Greeting()
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting() {
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


@Preview(
    showBackground = true,
    device = Devices.PIXEL_3,
    showSystemUi = true,
    name = "Launcher - Pixel 3"
)
@Composable
fun DefaultPreview() {
    ReSyncTheme {
        Greeting()
    }
}