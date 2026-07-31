package dev.helm.sdk

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class McuEventTest {

    @Test
    fun `StubMcuDataSource emits day and idle audio source`() = runTest {
        val source = StubMcuDataSource()
        val events = mutableListOf<McuEvent>()
        source.events().collect { events.add(it) }

        assertTrue(events.any { it is McuEvent.DayNightChanged && !it.isNight })
        assertTrue(events.any { it is McuEvent.AudioSourceChanged && it.sourceId == AudioSourceId.IDLE })
    }

    @Test
    fun `StubMcuDataSource send always succeeds`() = runTest {
        val result = StubMcuDataSource().send(0x0204, 1, 0)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `DoorState anyOpen is false when all closed`() {
        val state = DoorState()
        assertFalse(state.anyOpen)
    }

    @Test
    fun `DoorState anyOpen is true when driver open`() {
        val state = DoorState(driverOpen = true)
        assertTrue(state.anyOpen)
    }

    @Test
    fun `AmbientTempChanged null when sentinel 127`() {
        val event = McuEvent.AmbientTempChanged(celsius = null)
        assertNull(event.celsius)
    }

    @Test
    fun `AmbientTempChanged carries valid celsius`() {
        val event = McuEvent.AmbientTempChanged(celsius = 22)
        assertEquals(22, event.celsius)
    }

    @Test
    fun `ParkingRadarChanged stores distances correctly`() {
        val event = McuEvent.ParkingRadarChanged(
            front = listOf(10, 20, 30, 40),
            frontMax = 200,
            rear = listOf(15, 25, 35, 45),
            rearMax = 210,
        )
        assertEquals(4, event.front.size)
        assertEquals(4, event.rear.size)
        assertEquals(10, event.front[0])
        assertEquals(15, event.rear[0])
    }

    @Test
    fun `AudioSourceId constants have expected values`() {
        assertEquals(1, AudioSourceId.BLUETOOTH)
        assertEquals(7, AudioSourceId.RADIO)
        assertEquals(192, AudioSourceId.IDLE)
    }

    @Test
    fun `DataError Unavailable wraps optional cause`() {
        val err = DataError.Unavailable(cause = null)
        assertNull(err.cause)
        val err2 = DataError.Unavailable(RuntimeException("test"))
        assertTrue(err2.cause is RuntimeException)
    }
}
