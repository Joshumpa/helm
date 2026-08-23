package dev.helm.sdk

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

class WmoConditionTest {

    @ParameterizedTest(name = "WMO {0} maps to {1}")
    @CsvSource(
        "0,  CLEAR",
        "1,  CLOUDY", "2, CLOUDY", "3, CLOUDY",
        "45, HAZE", "48, HAZE",
        "51, RAIN", "61, RAIN", "67, RAIN",
        "71, SNOW", "77, SNOW",
        "80, RAIN", "82, RAIN",
        "85, SNOW", "86, SNOW",
        "95, THUNDERSTORM", "99, THUNDERSTORM",
    )
    fun `documented WMO codes map to the right icon`(code: Int, expected: WeatherCondition) {
        assertEquals(expected, wmoToCondition(code))
    }

    @ParameterizedTest(name = "gap code {0} falls back to CLOUDY")
    @ValueSource(ints = [4, 44, 49, 50, 68, 70, 78, 79, 83, 84, 87, 94, 100])
    fun `codes in the gaps fall back to CLOUDY`(code: Int) {
        assertEquals(WeatherCondition.CLOUDY, wmoToCondition(code))
    }

    @Test
    fun `nonsense codes never crash the widget`() {
        // A malformed API response must degrade to an icon, not blank the home screen.
        assertEquals(WeatherCondition.CLOUDY, wmoToCondition(-1))
        assertEquals(WeatherCondition.CLOUDY, wmoToCondition(9999))
        assertEquals(WeatherCondition.CLOUDY, wmoToCondition(Int.MAX_VALUE))
        assertEquals(WeatherCondition.CLOUDY, wmoToCondition(Int.MIN_VALUE))
    }

    @Test
    fun `every condition has a non-empty label for the widget`() {
        WeatherCondition.entries.forEach { assertEquals(it.label, it.label.trim().ifEmpty { null }) }
    }
}
