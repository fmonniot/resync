package eu.monniot.resync.downloader

import org.jsoup.Jsoup

class ArchiveOfOurOwnDriver : Driver() {

    override fun makeUrl(storyId: StoryId, chapterId: ChapterId): String =
        if (chapterId.id == null) {
            "https://archiveofourown.org/works/${storyId.id}?view_adult=true"
        } else {
            "https://archiveofourown.org/works/${storyId.id}/chapters/${chapterId.id}?view_adult=true"
        }

    override fun parseWebPage(source: String, storyId: StoryId, chapterId: ChapterId): Chapter {
        @Suppress("UNUSED_VARIABLE") val document = Jsoup.parse(source)

        val index = document
            .select("#chapter_index option")
            .mapIndexed { i, el -> (i + 1) to ChapterId(el.attr("value").toInt()) }
            .toMap()

        // first number is published total, second number is planned total
        // We are only interested in the first one, as we want to know what
        // we can fetch. Non-published works isn't available :)
        val (totalPublishedChapters, _) = document.select(".work.meta.group dd.chapters")
            .text()
            .superTrim()
            .split("/")
            .map { it.toInt() }

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
