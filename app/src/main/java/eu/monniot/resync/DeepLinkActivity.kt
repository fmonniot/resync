package eu.monniot.resync

import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import eu.monniot.resync.ui.ChapterSelection
import eu.monniot.resync.ui.DownloadScreen
import eu.monniot.resync.ui.ReSyncTheme


class DeepLinkActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val ffNetUri: Uri =
            intent?.data ?: throw IllegalArgumentException("Intent URI mustn't be null")

        val segments = ffNetUri.pathSegments
        val (rawStoryId, rawChNum) = when (segments.size) {
            2 -> Pair(segments[1], null) // /s/<storyId>
            3, 4 -> Pair(segments[1], segments[2])// /s/<storyId>/<chapter>[/story-name]
            else -> throw IllegalArgumentException("Unrecognized ff.net uri: $ffNetUri")
        }

        val storyId = rawStoryId.toInt()
        val chapterNumber = rawChNum?.toInt()

        val chapterSelection =
            if (chapterNumber != null) ChapterSelection.One(chapterNumber) else ChapterSelection.All

        setContent(null) {
            ReSyncTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {

                    DownloadScreen(
                        storyId,
                        chapterSelection,
                        askConfirmation = true,
                        onDone = { finish() }
                    )

                }
            }
        }
    }
}

