package dev.helm.launcher.media

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LrcParserTest {

    @Test
    fun `valid line parses timestamp to milliseconds`() {
        val result = LrcParser.parse("[01:23.45]Hello world")
        assertEquals(1, result.size)
        // 1*60_000 + 23*1_000 + 450 = 83_450
        assertEquals(83_450L, result[0].timeMs)
        assertEquals("Hello world", result[0].text)
    }

    @Test
    fun `2-digit centiseconds are padded to milliseconds`() {
        val result = LrcParser.parse("[00:00.45]text")
        // "45".padEnd(3, '0') = "450"
        assertEquals(450L, result[0].timeMs)
    }

    @Test
    fun `3-digit milliseconds are parsed correctly`() {
        val result = LrcParser.parse("[00:00.450]text")
        assertEquals(450L, result[0].timeMs)
    }

    @Test
    fun `line with only whitespace text is ignored`() {
        val result = LrcParser.parse("[00:01.00]   ")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `line with empty text is ignored`() {
        val result = LrcParser.parse("[00:01.00]")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `malformed line without timestamp is ignored`() {
        val result = LrcParser.parse("no timestamp here")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `lines are sorted by timestamp regardless of input order`() {
        val input = """
            [00:30.00]Second
            [00:10.00]First
            [00:50.00]Third
        """.trimIndent()
        val result = LrcParser.parse(input)
        assertEquals(listOf("First", "Second", "Third"), result.map { it.text })
    }

    @Test
    fun `mixed valid and invalid lines returns only valid ones`() {
        val input = """
            [00:01.00]Valid line
            not a valid line
            [00:02.00]Another valid line
        """.trimIndent()
        val result = LrcParser.parse(input)
        assertEquals(2, result.size)
    }

    @Test
    fun `empty input returns empty list`() {
        assertTrue(LrcParser.parse("").isEmpty())
    }
}
