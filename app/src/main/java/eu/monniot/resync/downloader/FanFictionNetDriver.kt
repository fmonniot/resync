package eu.monniot.resync.downloader

import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.jsoup.Jsoup
import java.io.File

class FanFictionNetDriver(private val ctx: Context) : Driver() {

    override val ioDispatcher: CoroutineDispatcher
        get() = Dispatchers.IO
    override val tmpChaptersFolder: File
        get() = ctx.filesDir.resolve("ffnet")

    override fun makeUrl(storyId: StoryId, chapterId: ChapterId): String =
        "https://m.fanfiction.net/s/${storyId.id}/${chapterId.id ?: 1}"

    override fun parseWebPage(source: String, storyId: StoryId, chapterId: ChapterId): Chapter {

        if (source.contains("DDoS protection by")) {
            throw Companion.WaitAndTryAgain
        }

        val document = Jsoup.parse(source)

        val extractText = { selector: String ->
            document.select(selector).first()!!.text().replace("\\n", "").trim()
        }

        val content = document.select(".storycontent").first()!!.html()
        val storyName = extractText("#content > div > b")
        val authorName = extractText("#content > div > a")

        val chapterName =
            document.select("#content").textNodes().last().text()
                .replace("\\n", "").trim()
                .ifBlank { null }

        // FanFiction.Net uses chapter numbers as id, so we can use this to our advantage here
        val chapterNumber = chapterId.id ?: 1

        // Total chapters
        // Unfortunately ff.net doesn't have great structure, so we have to look at a lot of links.
        // We then filter them their reference value: if they starts with the page url then its a
        // navigation link (and if they don't we don't really care about them).
        // Navigation links also includes some helpers (first, prev, next, review) that we remove
        // by checking if the link text is a number or not.
        // Note that it is highly likely that the total chapter is the only one containing a number
        // only, but we put the href check just to be one the safe side.
        val totalChapters =
            document.select("#top div[align] a")
                .firstOrNull {
                    it.attr("href").startsWith("/s/${storyId.id}") &&
                            it.text().toIntOrNull() != null
                }
                ?.text()?.toInt()
                ?: 1

        // FF.Net uses numbers as indices, so we can cheat a bit and build the index
        // based on that rule.
        val index = (1..totalChapters).associateWith { ChapterId(it) }

        return Chapter(
            storyId,
            chapterId,
            index,
            chapterNumber,
            chapterName,
            storyName,
            authorName,
            totalChapters,
            content
        )
    }
}
