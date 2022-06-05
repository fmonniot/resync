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

        // TODO That might not be such a good idea now that we have multiple account possible
        // It's probably a better idea to let the screens handle this responsibility
        val tokensPresent = mutableStateOf(readTokens(applicationContext).second != null)

        setContent(null) {
            ReSyncTheme {
                Surface(color = MaterialTheme.colors.background) {

                    if (!tokensPresent.value) {
                        SetupRemarkableScreen(onDone = { tokensPresent.value = true })
                    } else {
                        LauncherScreen()
                    }
                }
            }
        }
    }
}
