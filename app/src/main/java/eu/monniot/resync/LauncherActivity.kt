package eu.monniot.resync

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.*
import androidx.compose.runtime.*
import eu.monniot.resync.rmcloud.readTokens
import eu.monniot.resync.ui.ReSyncTheme
import eu.monniot.resync.ui.SetupRemarkableScreen
import eu.monniot.resync.ui.launcher.LauncherScreen


class LauncherActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val tokens = mutableStateOf(readTokens(applicationContext) != null)

        setContent(null) {
            ReSyncTheme {
                Surface(color = MaterialTheme.colors.background) {

                    if (tokens.value) {
                        SetupRemarkableScreen(onDone = { tokens.value = true })
                    } else {
                        LauncherScreen()
                    }
                }
            }
        }
    }
}
