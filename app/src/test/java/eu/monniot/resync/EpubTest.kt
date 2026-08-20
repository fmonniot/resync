package eu.monniot.resync

import org.junit.Assert.assertEquals
import org.junit.Test

class EpubTest {

    @Test
    fun sanitiseContent_hrWithAttributes_keepsSurroundingText() {
        val input = """<hr size="1" noshade> she turned away <em>slowly</em>"""
        val actual = sanitiseContent(input)

        assertEquals("<hr/> she turned away <em>slowly</em>", actual)
    }

    @Test
    fun sanitiseContent_bareHr_isNormalised() {
        assertEquals("<hr/>", sanitiseContent("<hr>"))
    }

    @Test
    fun sanitiseContent_hrWithAttributes_isNormalised() {
        assertEquals("<hr/>", sanitiseContent("""<hr size="1" noshade>"""))
    }

    @Test
    fun sanitiseContent_uppercaseHr_isNormalised() {
        assertEquals("<hr/>", sanitiseContent("<HR>"))
        assertEquals("<hr/>", sanitiseContent("""<HR SIZE="1" NOSHADE>"""))
    }

    @Test
    fun sanitiseContent_bareBr_isNormalised() {
        assertEquals("<br/>", sanitiseContent("<br>"))
    }

    @Test
    fun sanitiseContent_selfClosingBr_isNormalised() {
        assertEquals("<br/>", sanitiseContent("<br/>"))
        assertEquals("<br/>", sanitiseContent("<br />"))
    }

    @Test
    fun sanitiseContent_uppercaseBr_isNormalised() {
        assertEquals("<br/>", sanitiseContent("<BR>"))
    }

    @Test
    fun sanitiseContent_multipleTagsOnOneLine_areAllNormalised() {
        val input = "before<br>middle<hr size=\"1\">after<BR/>end<HR noshade>tail"
        val expected = "before<br/>middle<hr/>after<br/>end<hr/>tail"

        assertEquals(expected, sanitiseContent(input))
    }

    @Test
    fun sanitiseContent_stripsNbspAndXhtmlNamespace() {
        val input = "<p xmlns=\"http://www.w3.org/1999/xhtml\">a&nbsp;b</p>"
        val expected = "<p >ab</p>"

        assertEquals(expected, sanitiseContent(input))
    }
}
