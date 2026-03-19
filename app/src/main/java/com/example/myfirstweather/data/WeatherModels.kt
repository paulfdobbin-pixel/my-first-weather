package com.example.myfirstweather.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WeatherResponse(
    val daily: DailyData,
    val hourly: HourlyData? = null,
    @SerialName("daily_units") val dailyUnits: DailyUnits,
    @SerialName("utc_offset_seconds") val utcOffsetSeconds: Int = 0,
    val timezone: String = "UTC"
)

@Serializable
data class DailyData(
    val time: List<String>,
    @SerialName("temperature_2m_max") val tempMax: List<Double>,
    @SerialName("temperature_2m_min") val tempMin: List<Double>,
    val sunrise: List<String>,
    val sunset: List<String>,
    @SerialName("weather_code") val weatherCode: List<Int>,
    @SerialName("uv_index_max") val uvIndexMax: List<Double>? = null
)

@Serializable
data class HourlyData(
    val time: List<String>,
    @SerialName("temperature_2m") val temperature2m: List<Double>,
    val rain: List<Double>? = null,
    @SerialName("wind_speed_10m") val windSpeed10m: List<Double>? = null,
    @SerialName("weather_code") val weatherCode: List<Int>? = null,
    @SerialName("is_day") val isDay: List<Int>? = null,
    @SerialName("relative_humidity_2m") val humidity: List<Double>? = null,
    @SerialName("uv_index") val uvIndex: List<Double>? = null
)

@Serializable
data class DailyUnits(
    @SerialName("temperature_2m_max") val tempMaxUnit: String,
    @SerialName("temperature_2m_min") val tempMinUnit: String
)

@Serializable
data class MarineResponse(
    val daily: DailyTideData? = null,
    val hourly: MarineHourlyData? = null
)

@Serializable
data class DailyTideData(
    val time: List<String>,
    @SerialName("tide_height_max") val highTides: List<Double>? = null,
    @SerialName("tide_height_min") val lowTides: List<Double>? = null
)

@Serializable
data class MarineHourlyData(
    val time: List<String>,
    @SerialName("tide_height") val tideHeight: List<Double>? = null,
    @SerialName("wave_height") val waveHeight: List<Double>? = null
)

@Serializable
data class AirQualityResponse(
    val hourly: AirQualityHourlyData
)

@Serializable
data class AirQualityHourlyData(
    val time: List<String>,
    @SerialName("european_aqi") val aqi: List<Double>? = null,
    @SerialName("pm10") val pm10: List<Double>? = null,
    @SerialName("pm2_5") val pm2_5: List<Double>? = null,
    @SerialName("alder_pollen") val alderPollen: List<Double>? = null,
    @SerialName("birch_pollen") val birchPollen: List<Double>? = null,
    @SerialName("grass_pollen") val grassPollen: List<Double>? = null,
    @SerialName("mugwort_pollen") val mugwortPollen: List<Double>? = null,
    @SerialName("olive_pollen") val olivePollen: List<Double>? = null,
    @SerialName("ragweed_pollen") val ragweedPollen: List<Double>? = null
)

@Serializable
data class GeocodingResponse(
    val results: List<GeocodingResult>? = null
)

@Serializable
data class GeocodingResult(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String? = null,
    val admin1: String? = null
)

data class WeatherUIModel(
    val date: String,
    val tempMax: Double,
    val tempMin: Double,
    val sunrise: String,
    val sunset: String,
    val weatherCode: Int,
    val moonPhase: Double,
    val hourlyTemp: List<Double> = emptyList(),
    val hourlyRain: List<Double> = emptyList(),
    val hourlyWind: List<Double> = emptyList(),
    val hourlyTide: List<Double> = emptyList(),
    val hourlyWave: List<Double> = emptyList(),
    val hourlyWeatherCode: List<Int> = emptyList(),
    val hourlyIsDay: List<Boolean> = emptyList(),
    val hourlyHumidity: List<Double> = emptyList(),
    val hourlyUV: List<Double> = emptyList(),
    val dailyUVMax: Double = 0.0,
    val aqi: Double = 0.0,
    val hourlyHours: List<Int> = (0..23).toList(),
    val dailyHighTide: Double? = null,
    val dailyLowTide: Double? = null,
    val hourlyPollen: List<Double> = emptyList() // Average of different pollen types
)
