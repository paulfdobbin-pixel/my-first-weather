package com.example.myfirstweather.data

import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi {
    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("hourly") hourly: String = "temperature_2m,rain,wind_speed_10m,weather_code,is_day,relative_humidity_2m,uv_index",
        @Query("daily") daily: String = "temperature_2m_max,temperature_2m_min,sunrise,sunset,weather_code,uv_index_max",
        @Query("timezone") timezone: String = "auto",
        @Query("forecast_days") days: Int = 11
    ): WeatherResponse
}

interface MarineApi {
    @GET("v1/marine")
    suspend fun getTides(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("daily") daily: String = "tide_height_max,tide_height_min",
        @Query("hourly") hourly: String = "tide_height",
        @Query("timezone") timezone: String = "auto",
        @Query("forecast_days") days: Int = 11
    ): MarineResponse
}

interface AirQualityApi {
    @GET("v1/air-quality")
    suspend fun getAirQuality(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("hourly") hourly: String = "european_aqi,pm10,pm2_5,alder_pollen,birch_pollen,grass_pollen,mugwort_pollen,olive_pollen,ragweed_pollen",
        @Query("timezone") timezone: String = "auto",
        @Query("forecast_days") days: Int = 11
    ): AirQualityResponse
}

interface GeocodingApi {
    @GET("v1/search")
    suspend fun searchLocation(
        @Query("name") name: String,
        @Query("count") count: Int = 5,
        @Query("language") language: String = "en",
        @Query("format") format: String = "json"
    ): GeocodingResponse
}
