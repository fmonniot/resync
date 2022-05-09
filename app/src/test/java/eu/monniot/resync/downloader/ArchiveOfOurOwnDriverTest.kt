package eu.monniot.resync.downloader

import org.junit.Assert
import org.junit.Test

class ArchiveOfOurOwnDriverTest {

    private fun getResourceAsText(path: String): String {
        return javaClass.classLoader!!.getResource("ao3/works-$path.html")!!.readText()
    }

    private val driver = ArchiveOfOurOwnDriver(null!!)

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

    @Test
    fun parse_unknownPlannedChapters() {
        val html = getResourceAsText("15343806-chapters-37208093")
        val actual =
            driver.parseWebPage(html, StoryId(15343806), ChapterId(37208093)).copy(content = "")
        val expected = Chapter(
            StoryId(15343806),
            ChapterId(37208093),
            mapOf(
                1 to ChapterId(35603070),
                2 to ChapterId(35603169),
                3 to ChapterId(35603160),
                4 to ChapterId(35603412),
                5 to ChapterId(35603439),
                6 to ChapterId(35603490),
                7 to ChapterId(35603517),
                8 to ChapterId(35603541),
                9 to ChapterId(35603592),
                10 to ChapterId(35603607),
                11 to ChapterId(35603628),
                12 to ChapterId(35686365),
                13 to ChapterId(37208093),
                14 to ChapterId(40007271),
                15 to ChapterId(43238078),
                16 to ChapterId(50362376),
                17 to ChapterId(55007023),
                18 to ChapterId(59087716),
                19 to ChapterId(60479464),
                20 to ChapterId(63157024),
                21 to ChapterId(69058209),
                22 to ChapterId(69962349),
                23 to ChapterId(73217637),
            ),
            13,
            "Evil Fantasies",
            "A Godfather's Promise",
            "TheMetalSage",
            23,
            ""
        )

        Assert.assertEquals(expected, actual)
    }

    @Test
    fun parse_retryLater_throwsException() {
        val html = getResourceAsText("retry-later")

        Assert.assertThrows(Driver.Companion.RateLimited::class.java) {
            driver.parseWebPage(
                html,
                StoryId(35336083),
                ChapterId(null)
            )
        }
    }

}