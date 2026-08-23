package dev.helm.sdk

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

class ReconnectBackoffTest {

    @ParameterizedTest(name = "attempt {0} waits {1} ms")
    @CsvSource(
        "0, 500",
        "1, 1000",
        "2, 2000",
        "3, 4000",
        "4, 8000",
        "5, 16000",
        "6, 30000",   // 32000 clamped to the 30 s ceiling
    )
    fun `back-off follows the documented schedule`(attempt: Long, expectedMs: Long) {
        assertEquals(expectedMs, reconnectDelayMs(attempt))
    }

    @ParameterizedTest(name = "attempt {0} stays at the 30 s ceiling")
    @ValueSource(longs = [7, 8, 20, 63, 64, 1_000, Int.MAX_VALUE.toLong(), Long.MAX_VALUE])
    fun `long outages stay at the ceiling`(attempt: Long) {
        assertEquals(30_000L, reconnectDelayMs(attempt))
    }

    @Test
    fun `delay is never zero or negative`() {
        // A non-positive delay would turn reconnection into a busy loop on the UART.
        listOf(0L, 6L, 7L, 64L, Long.MAX_VALUE).forEach {
            assertTrue(reconnectDelayMs(it) > 0, "attempt $it produced a non-positive delay")
        }
    }

    @Test
    fun `back-off is monotonic up to the ceiling`() {
        val delays = (0L..10L).map { reconnectDelayMs(it) }
        delays.zipWithNext { a, b -> assertTrue(b >= a, "delay decreased: $a then $b") }
        assertEquals(30_000L, delays.last())
    }
}
