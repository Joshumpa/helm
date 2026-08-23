package dev.helm.sdk

enum class WeatherCondition(val label: String) {
    CLEAR("Clear"),
    CLOUDY("Cloudy"),
    RAIN("Rain"),
    THUNDERSTORM("Storm"),
    SNOW("Snow"),
    HAZE("Haze"),
}

// WMO weather interpretation codes → Helm's 6 icon buckets.
// https://open-meteo.com/en/docs — unknown codes fall back to CLOUDY so the
// widget always renders something rather than blanking out.
internal fun wmoToCondition(code: Int): WeatherCondition = when (code) {
    0 -> WeatherCondition.CLEAR
    in 1..3 -> WeatherCondition.CLOUDY
    45, 48 -> WeatherCondition.HAZE
    in 51..67 -> WeatherCondition.RAIN
    in 71..77 -> WeatherCondition.SNOW
    in 80..82 -> WeatherCondition.RAIN
    85, 86 -> WeatherCondition.SNOW
    in 95..99 -> WeatherCondition.THUNDERSTORM
    else -> WeatherCondition.CLOUDY
}
