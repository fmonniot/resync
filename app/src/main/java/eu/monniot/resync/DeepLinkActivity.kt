package eu.monniot.resync

import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import eu.monniot.resync.downloader.ChapterId
import eu.monniot.resync.downloader.DriverType
import eu.monniot.resync.downloader.StoryId
import eu.monniot.resync.ui.ReSyncTheme
import eu.monniot.resync.ui.downloader.DownloadScreen


class DeepLinkActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uri: Uri = intent?.data ?: throw IllegalArgumentException("Intent URI mustn't be null")

        val (storyId, chapterId, driverType) = parsePath(uri)

        setContent(null) {
            ReSyncTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
                    DownloadScreen(
                        driverType,
                        storyId,
                        chapterId,
                        onDone = {}
                    )
                }
            }
        }
    }

    companion object {
        fun parsePath(uri: Uri): Triple<StoryId, ChapterId, DriverType> {
            val segments = uri.pathSegments

            val (driverType, rawStoryId, rawChId) = when (uri.host) {
                "m.fanfiction.net", "www.fanfiction.net" -> {
                    val (rawStoryId, rawChNum) = when (segments.size) {
                        2 -> segments[1] to null // /s/<storyId>
                        3, 4 -> segments[1] to segments[2] // /s/<storyId>/<chapter>[/story-name]
                        else -> throw IllegalArgumentException("Unrecognized ff.net uri: $uri")
                    }

                    Triple(DriverType.FanFictionNet, rawStoryId, rawChNum)
                }
                "archiveofourown.org" -> {
                    val (rawStoryId, rawChNum) = when (segments.size) {
                        2 -> segments[1] to null // /works/<storyId>
                        4 -> segments[1] to segments[3] // /works/<storyId>/chapters/<chapterId>
                        else -> throw IllegalArgumentException("Unrecognized ao3 uri: $uri")
                    }

                    Triple(DriverType.ArchiveOfOurOwn, rawStoryId, rawChNum)
                }
                else -> throw IllegalArgumentException("Unrecognized uri's host: $uri")
            }

            val storyId = rawStoryId.toInt()
            val chapterId = rawChId?.toInt()

            return Triple(StoryId(storyId), ChapterId(chapterId), driverType)
        }
    }
}

