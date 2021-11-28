package eu.monniot.resync.downloader

import org.junit.Assert
import org.junit.Test

class FanFictionNetDriverTest {

    private fun getResourceAsText(path: String): String {
        return javaClass.classLoader!!.getResource("ffnet/s-$path.html")!!.readText()
    }

    private val driver = FanFictionNetDriver()

    private val index1Chapter = mapOf(1 to ChapterId(1))
    private val index2Chapters = (1..2).associateWith { ChapterId(it) }
    private val index16Chapters = (1..16).associateWith { ChapterId(it) }
    private val index24Chapters = (1..24).associateWith { ChapterId(it) }

    @Test
    fun parse_multiChapterStory_firstChapter() {
        val html = getResourceAsText("3384712")
        val actual = driver.parseWebPage(html, StoryId(3384712), ChapterId(1)).copy(content = "")
        val expected = Chapter(
            StoryId(3384712),
            ChapterId(1),
            index24Chapters,
            1,
            "Chapter 1: A House of Cards",
            "The Lie I've Lived",
            "jbern",
            24,
            ""
        )

        Assert.assertEquals(expected, actual)
    }

    @Test
    fun parse_multiChapterStory_secondToLastChapter() {
        val html = getResourceAsText("3384712-23")
        val actual = driver.parseWebPage(html, StoryId(3384712), ChapterId(23)).copy(content = "")
        val expected = Chapter(
            StoryId(3384712),
            ChapterId(23),
            index24Chapters,
            23,
            "Chapter 23: Humiliation and Other Diversionary",
            "The Lie I've Lived",
            "jbern",
            24,
            ""
        )

        Assert.assertEquals(expected, actual)
    }

    @Test
    fun parse_multiChapterStory_lastChapter() {
        val html = getResourceAsText("3384712-24")
        val actual = driver.parseWebPage(html, StoryId(3384712), ChapterId(24)).copy(content = "")
        val expected = Chapter(
            StoryId(3384712),
            ChapterId(24),
            index24Chapters,
            24,
            "Chapter 24: Cry Havoc",
            "The Lie I've Lived",
            "jbern",
            24,
            ""
        )

        Assert.assertEquals(expected, actual)
    }


    @Test
    fun parse_twoChaptersStory_firstChapter() {
        val html = getResourceAsText("13992705-1")
        val actual = driver.parseWebPage(html, StoryId(13992705), ChapterId(1)).copy(content = "")
        val expected = Chapter(
            StoryId(13992705),
            ChapterId(1),
            index2Chapters,
            1,
            "Chapter 1: Prologo",
            "Yo Soy La Otra",
            "CeliaBeth16",
            2,
            ""
        )

        Assert.assertEquals(expected, actual)
    }

    @Test
    fun parse_twoChaptersStory_secondChapter() {
        val html = getResourceAsText("13992705-2")
        val actual = driver.parseWebPage(html, StoryId(13992705), ChapterId(2)).copy(content = "")
        val expected = Chapter(
            StoryId(13992705),
            ChapterId(2),
            index2Chapters,
            2,
            "Chapter 1: Decisiones",
            "Yo Soy La Otra",
            "CeliaBeth16",
            2,
            ""
        )

        Assert.assertEquals(expected, actual)
    }


    @Test
    fun parse_oneShotStory_firstChapter() {
        val html = getResourceAsText("13995110-1")
        val actual = driver.parseWebPage(html, StoryId(13995110), ChapterId(1)).copy(content = "")
        val expected = Chapter(
            StoryId(13995110),
            ChapterId(1),
            index1Chapter,
            1,
            null,
            "More Than Satisfactory",
            "Deanna Halliwell",
            1,
            ""
        )

        Assert.assertEquals(expected, actual)
    }

    // see https://github.com/fmonniot/resync/issues/30
    @Test
    fun parse_malformedStory_aChapter() {
        val html = getResourceAsText("9157763-3")
        val actual = driver.parseWebPage(html, StoryId(9157763), ChapterId(3)).copy(content = "")
        val expected = Chapter(
            StoryId(9157763),
            ChapterId(3),
            index16Chapters,
            3,
            "Chapter 3 - In the Middle of the Night",
            "All Was Well",
            "SarRansom",
            16,
            ""
        )

        Assert.assertEquals(expected, actual)
    }

    @Test
    fun parse_extractContent_aChapter() {
        val html = getResourceAsText("13846282-1")
        val actual = driver.parseWebPage(html, StoryId(13846282), ChapterId(1))
        val expected = Chapter(
            StoryId(13846282),
            ChapterId(1),
            index1Chapter,
            1,
            null,
            "A Surprise Visitor",
            "HeatherLovesCB",
            1,
            "<p>\"Hello, Harry,\" a female voice said from behind Harry. She had a French accent.</p>\n" +
                    "<p>Harry turned around and to his surprise, he saw one of the other champions, Fleur Delacour.</p>\n" +
                    "<p>She was first a caring friend, then she became a loving romantic partner. With Fleur now by his side, he could get used to it.</p>"
        )

        Assert.assertEquals(expected, actual)
    }

}