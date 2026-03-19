package com.example.myfirstweather.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter

object WeatherCodeUtils {
    fun getWeatherDescription(code: Int): String {
        return when (code) {
            0 -> "Clear Sky"
            1, 2 -> "Partly Cloudy"
            3 -> "Overcast"
            45, 48 -> "Misty Fog"
            51, 53, 55 -> "Light Drizzle"
            56, 57 -> "Freezing Drizzle"
            61, 63, 65 -> "Rainy"
            66, 67 -> "Freezing Rain"
            71, 73, 75 -> "Snowfall"
            77 -> "Snow Grains"
            80, 81, 82 -> "Rain Showers"
            85, 86 -> "Snow Showers"
            95 -> "Thunderstorm"
            96, 99 -> "Severe Storm"
            else -> "Unknown"
        }
    }

    @Composable
    fun getWeatherIcon(code: Int, isDay: Boolean): Painter {
        val imageVector = when (code) {
            0 -> if (isDay) Icons.Outlined.WbSunny else Icons.Outlined.NightsStay
            1, 2 -> if (isDay) Icons.Outlined.CloudQueue else Icons.Outlined.NightsStay
            3 -> Icons.Outlined.Cloud
            45, 48 -> Icons.Outlined.Air 
            51, 53, 55 -> Icons.Outlined.WaterDrop
            56, 57 -> Icons.Outlined.AcUnit
            61, 63, 65, 66, 67 -> Icons.Outlined.Umbrella
            71, 73, 75, 77 -> Icons.Outlined.AcUnit
            80, 81, 82 -> Icons.Outlined.Thunderstorm
            85, 86 -> Icons.Outlined.AcUnit
            95, 96, 99 -> Icons.Outlined.Thunderstorm
            else -> Icons.AutoMirrored.Outlined.HelpOutline
        }
        return rememberVectorPainter(imageVector)
    }

    fun getWeatherColor(code: Int, isDay: Boolean): Color {
        return when (code) {
            0 -> if (isDay) Color(0xFFFFE082) else Color(0xFFC5CAE9) // Pastel Yellow / Lavender
            1, 2, 3 -> if (isDay) Color(0xFFB3E5FC) else Color(0xFFCFD8DC) // Pastel Blue / Blue Grey
            45, 48 -> Color(0xFFF5F5F5) // Soft White
            51, 53, 55, 56, 57 -> Color(0xFF81D4FA) // Pastel Sky Blue
            61, 63, 65, 66, 67 -> Color(0xFF90CAF9) // Pastel Blue
            71, 73, 75, 77 -> Color(0xFFE1F5FE) // Very Light Blue
            80, 81, 82 -> Color(0xFF4FC3F7) // Bright Pastel Blue
            85, 86 -> Color(0xFFB3E5FC)
            95, 96, 99 -> Color(0xFFFFE082) // Pastel Gold
            else -> Color(0xFFECEFF1)
        }
    }
}
