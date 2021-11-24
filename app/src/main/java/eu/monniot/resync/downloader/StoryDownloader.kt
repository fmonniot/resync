package eu.monniot.resync.downloader

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import eu.monniot.resync.ui.ChapterSelection

class StoryDownloader(driver: Driver, initialChapterSelection: ChapterSelection) : ViewModel() {
    private val mutableScreenState = mutableStateOf(FetchFirstChapter)

    val screenState: State<DownloadState>
        get() = mutableScreenState

    init {
        // fetch the first chapter
    }
}

sealed interface DownloadState
object FetchFirstChapter : DownloadState
