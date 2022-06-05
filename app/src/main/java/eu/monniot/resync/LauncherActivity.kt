package eu.monniot.resync

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.*
import eu.monniot.resync.ui.ReSyncTheme
import eu.monniot.resync.ui.launcher.LauncherScreen


class LauncherActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent(null) {
            ReSyncTheme {
                Surface(color = MaterialTheme.colors.background) {

                    LauncherScreen()
                }
            }
        }
    }
}
