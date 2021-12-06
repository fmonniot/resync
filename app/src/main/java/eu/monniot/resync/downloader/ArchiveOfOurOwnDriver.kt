package eu.monniot.resync.downloader

import org.jsoup.Jsoup

// AO3 has a rate limit of 40 chapters per ??? (more than a minute I think)
// Seems it's 40 per 5 minutes. At least trying again after 4m30 is still limited.
// There might be a bit of leeway though, as another time the RL kicked in at 40
// load but was working again after 270 seconds.
class ArchiveOfOurOwnDriver : Driver() {

    override fun makeUrl(storyId: StoryId, chapterId: ChapterId): String =
        if (chapterId.id == null) {
            "https://archiveofourown.org/works/${storyId.id}?view_adult=true"
        } else {
            "https://archiveofourown.org/works/${storyId.id}/chapters/${chapterId.id}?view_adult=true"
        }

    override fun parseWebPage(source: String, storyId: StoryId, chapterId: ChapterId): Chapter {
        val document = Jsoup.parse(source)

        // First, let's check if the page is a rate limit one
        if (document.select("body pre").text().superTrim() == "Retry later") {
            throw Companion.RateLimited
        }

        val index = document
            .select("#chapter_index option")
            .mapIndexed { i, el -> (i + 1) to ChapterId(el.attr("value").toInt()) }
            .toMap()

        // first number is published total, second number is planned total
        // We are only interested in the first one, as we want to know what
        // we can fetch. Non-published works isn't available :)
        val (totalPublishedChaptersStr, _) = document.select(".work.meta.group dd.chapters")
            .text()
            .superTrim()
            .split("/")

        val totalPublishedChapters = totalPublishedChaptersStr.toInt()

        val storyName = document.select("h2.title").text().superTrim()
        val authorName = document.select("h3.byline.heading").text().superTrim()

        val chapterNumber =
            document.select("#chapters .title a").text()
                .ifBlank { null }
                ?.replace("Chapter ", "")?.toInt()
                ?: 1

        val chapterName = document
            .select("#chapters .title")
            .textNodes().lastOrNull()
            ?.text()?.substring(1)?.superTrim()

        val content = document.select("#chapters .userstuff").html()

        return Chapter(
            storyId,
            chapterId,
            index,
            chapterNumber,
            chapterName,
            storyName,
            authorName,
            totalPublishedChapters,
            content
        )
    }

    private fun String.superTrim(): String =
        this.replace("\\n", "").trim()
}
