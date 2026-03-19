package com.example.myfirstweather.data

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.cos
import kotlin.math.PI

object MoonPhaseUtils {
    /**
     * Calculates the moon phase for a given date.
     * Returns a value between 0 and 1, where:
     * 0.0: New Moon
     * 0.25: First Quarter
     * 0.5: Full Moon
     * 0.75: Last Quarter
     */
    fun getMoonPhase(date: LocalDate): Double {
        val knownNewMoon = LocalDate.of(1970, 1, 7)
        val daysSinceNewMoon = ChronoUnit.DAYS.between(knownNewMoon, date)
        val lunarCycle = 29.530588853
        val phase = (daysSinceNewMoon % lunarCycle) / lunarCycle
        return if (phase < 0) phase + 1 else phase
    }

    fun getMoonPhaseName(phase: Double): String {
        return when {
            phase < 0.0625 || phase >= 0.9375 -> "New Moon"
            phase < 0.1875 -> "Waxing Crescent"
            phase < 0.3125 -> "First Quarter"
            phase < 0.4375 -> "Waxing Gibbous"
            phase < 0.5625 -> "Full Moon"
            phase < 0.6875 -> "Waning Gibbous"
            phase < 0.8125 -> "Last Quarter"
            else -> "Waning Crescent"
        }
    }

    // Alias for WeatherScreen
    fun getPhaseName(phase: Double): String = getMoonPhaseName(phase)

    fun getIllumination(phase: Double): Double {
        // Simple approximation: 0 at New Moon (0/1), 1 at Full Moon (0.5)
        return (1 - cos(phase * 2 * PI)) / 2
    }

    fun getMoonPhaseEmoji(phase: Double): String {
        return when {
            phase < 0.0625 || phase >= 0.9375 -> "🌑"
            phase < 0.1875 -> "🌒"
            phase < 0.3125 -> "🌓"
            phase < 0.4375 -> "🌔"
            phase < 0.5625 -> "🌕"
            phase < 0.6875 -> "🌖"
            phase < 0.8125 -> "🌗"
            else -> "🌘"
        }
    }
}
