package eu.monniot.resync.downloader

import eu.monniot.resync.ui.downloader.ChapterId
import eu.monniot.resync.ui.downloader.StoryId

class FanFictionNetDriver : Driver() {

    override fun makeUrl(storyId: StoryId, chapterId: ChapterId): String =
        "https://m.fanfiction.net/s/${storyId}/${chapterId}"

    private val scriptText =
        "javascript:window.grabber.%s(document.querySelector('%s').innerText);"

    private val scriptHtml =
        "javascript:window.grabber.%s(new XMLSerializer().serializeToString(document.querySelector('%s')));"
    private val scriptChapterName =
        "javascript:window.grabber.onChapterName((Array.apply([], document.querySelector('#content').childNodes || []).filter(n => n.nodeType === 3).pop() || {}).textContent);"

    /* Un-minimized javascript

        function re(acc, current) {
            const [done, previous] = acc;

            if (done) {
                return [true, previous]
            } else {
                if (current.text === "Next »" || current.text === " Review") {
                    return [true, previous]
                } else {
                    return [false, current]
                }
            }
        }

        window.grabber.onTotalChapters(
            Array.apply([], document.querySelectorAll('#top div[align] a'))
                .reduce(re, [false, null])[1]
                .textContent
        )
    */
    private val scriptTotalChapters =
        "javascript:function re(e,t){const[n,o]=e;return console.log(t.text),n?[!0,o]:\"Next »\"===t.text||\" Review\"===t.text?[!0,o]:[!1,t]}window.grabber.onTotalChapters(Array.apply([],document.querySelectorAll(\"#top div[align] a\")).reduce(re,[!1,null])[1].textContent);"

    private val scriptChapterNumber =
        "javascript:window.grabber.onChapterNumber(document.location.pathname.split('/')[3]);"

    override val chapterTextScript: String
        get() = scriptHtml.format("onChapterText", ".storycontent")
    override val storyNameScript: String
        get() = scriptText.format("onStoryName", "#content > div > b")
    override val authorNameScript: String
        get() = scriptText.format("onAuthorName", "#content > div > a")
    override val chapterNameScript: String
        get() = scriptChapterName
    override val totalChaptersScript: String
        get() = scriptTotalChapters
    override val chapterNumScript: String
        get() = scriptChapterNumber
}
