package eu.monniot.resync.downloader

import org.junit.Assert
import org.junit.Test

class ArchiveOfOurOwnDriverTest {

    private fun getResourceAsText(path: String): String {
        return javaClass.classLoader!!.getResource("ao3/works-$path.html")!!.readText()
    }

    private val driver = ArchiveOfOurOwnDriver()

    @Test
    fun parse_oneShotStory_firstChapter() {
        val html = getResourceAsText("35336083")
        val actual = driver.parseWebPage(html, StoryId(35336083), ChapterId(null))
        val expected = Chapter(
            StoryId(35336083),
            ChapterId(null),
            emptyMap(), // One-shots don't have an index on AO3
            1,
            null,
            "Help me I can't find the fic",
            "Damian_4rk",
            1,
            "<p>I just remember a chapter saying something in parseltongue</p>\n" +
                    "<p>Harry was also searching the horrocruxes I'm sorry I don't remember too much because I read a lot of fics but I miss this one</p>"
        )

        Assert.assertEquals(expected, actual)
    }

    @Test
    fun parse_multiChapterStory_firstChapter() {
        val html = getResourceAsText("34804678-chapters-86665087")
        val actual =
            driver.parseWebPage(html, StoryId(34804678), ChapterId(34804678)).copy(content = "")
        val expected = Chapter(
            StoryId(34804678),
            ChapterId(34804678),
            mapOf(
                1 to ChapterId(86665087),
                2 to ChapterId(86709106),
                3 to ChapterId(86822239),
                4 to ChapterId(86971135),
                5 to ChapterId(87090190),
                6 to ChapterId(87237301),
                7 to ChapterId(87389359),
                8 to ChapterId(87480331),
                9 to ChapterId(87550789),
                10 to ChapterId(87894796),
                11 to ChapterId(88047883),
            ),
            1,
            "Always",
            "More Than Enough",
            "Firemione",
            11,
            ""
        )

        Assert.assertEquals(expected, actual)
    }

}