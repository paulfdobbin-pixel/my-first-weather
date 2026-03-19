package com.example.myfirstweather.ui

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myfirstweather.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import androidx.core.content.edit

class WeatherViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = getApplication<Application>().getSharedPreferences("weather_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val uiState: StateFlow<WeatherUiState> = _uiState

    private val _searchSuggestions = MutableStateFlow<List<GeocodingResult>>(emptyList())
    val searchSuggestions: StateFlow<List<GeocodingResult>> = _searchSuggestions

    private val _currentLocationName = MutableStateFlow("Newcastle")
    val currentLocationName: StateFlow<String> = _currentLocationName

    private val json = Json { ignoreUnknownKeys = true }
    
    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(logging)
        .build()

    private fun createRetrofit(baseUrl: String) = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    private val weatherApi = createRetrofit("https://api.open-meteo.com/").create(WeatherApi::class.java)
    private val marineApi = createRetrofit("https://marine-api.open-meteo.com/").create(MarineApi::class.java)
    private val airQualityApi = createRetrofit("https://air-quality-api.open-meteo.com/").create(AirQualityApi::class.java)
    private val geocodingApi = createRetrofit("https://geocoding-api.open-meteo.com/").create(GeocodingApi::class.java)

    private var searchJob: Job? = null

    init {
        val lastLat = prefs.getFloat("last_lat", 54.9733f).toDouble()
        val lastLon = prefs.getFloat("last_lon", -1.6140f).toDouble()
        val lastName = prefs.getString("last_name", "Newcastle") ?: "Newcastle"
        fetchWeatherData(lastLat, lastLon, lastName)
    }

    fun onSearchQueryChanged(query: String) {
        searchJob?.cancel()
        if (query.length < 2) {
            _searchSuggestions.value = emptyList()
            return
        }

        searchJob = viewModelScope.launch {
            delay(300) // Debounce
            try {
                val response = withContext(Dispatchers.IO) {
                    geocodingApi.searchLocation(query)
                }
                _searchSuggestions.value = response.results ?: emptyList()
            } catch (e: Exception) {
                Log.e("WeatherViewModel", "Geocoding Error: ${e.message}")
            }
        }
    }

    fun searchLocation(query: String) {
        searchJob?.cancel()
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    geocodingApi.searchLocation(query)
                }
                _searchSuggestions.value = response.results ?: emptyList()
            } catch (e: Exception) {
                Log.e("WeatherViewModel", "Search Error: ${e.message}")
            }
        }
    }

    fun selectLocation(location: GeocodingResult) {
        _searchSuggestions.value = emptyList()
        
        prefs.edit {
            putFloat("last_lat", location.latitude.toFloat())
                .putFloat("last_lon", location.longitude.toFloat())
                .putString("last_name", location.name)
        }

        fetchWeatherData(location.latitude, location.longitude, location.name)
    }

    fun fetchWeatherData(lat: Double, lon: Double, locationName: String) {
        viewModelScope.launch {
            _uiState.value = WeatherUiState.Loading
            _currentLocationName.value = locationName
            try {
                val uiModels = withContext(Dispatchers.IO) {
                    val forecast = weatherApi.getForecast(lat, lon)
                    
                    val marine = try { 
                        marineApi.getTides(lat, lon)
                    } catch (e: Exception) { 
                        Log.e("WeatherViewModel", "Marine API Error: ${e.message}")
                        null 
                    }
                    
                    val airQuality = try { airQualityApi.getAirQuality(lat, lon) } catch (e: Exception) { null }
                    
                    val zoneId = try { ZoneId.of(forecast.timezone) } catch (e: Exception) { ZoneId.systemDefault() }
                    val localNow = ZonedDateTime.now(zoneId)
                    
                    forecast.daily.time.mapIndexed { index, time ->
                        val date = LocalDate.parse(time)
                        
                        // Adjust start index for "Today" to start from current hour
                        val startIdx = if (index == 0) index * 24 + localNow.hour else index * 24
                        val endIdx = (index + 1) * 24
                        
                        val hourlyTemp = forecast.hourly?.temperature2m?.safeSubList(startIdx, endIdx) ?: emptyList()
                        val hourlyIsDay = forecast.hourly?.isDay?.safeSubList(startIdx, endIdx)?.map { it == 1 } ?: emptyList()
                        val hourlyTide = marine?.hourly?.tideHeight?.safeSubList(startIdx, endIdx) ?: emptyList()

                        // Pollen mapping - average of available types
                        val hourlyPollen = if (airQuality?.hourly != null) {
                            val types = listOfNotNull(
                                airQuality.hourly.alderPollen?.safeSubList(startIdx, endIdx),
                                airQuality.hourly.birchPollen?.safeSubList(startIdx, endIdx),
                                airQuality.hourly.grassPollen?.safeSubList(startIdx, endIdx),
                                airQuality.hourly.mugwortPollen?.safeSubList(startIdx, endIdx),
                                airQuality.hourly.olivePollen?.safeSubList(startIdx, endIdx),
                                airQuality.hourly.ragweedPollen?.safeSubList(startIdx, endIdx)
                            )
                            if (types.isNotEmpty()) {
                                List(hourlyTemp.size) { hIdx ->
                                    types.map { it[hIdx] }.average()
                                }
                            } else emptyList()
                        } else emptyList()

                        val moonPhase = MoonPhaseUtils.getMoonPhase(date)

                        val weatherCode = if (index == 0) {
                            forecast.hourly?.weatherCode?.getOrNull(localNow.hour) ?: forecast.daily.weatherCode[index]
                        } else {
                            forecast.daily.weatherCode[index]
                        }

                        WeatherUIModel(
                            date = date.format(DateTimeFormatter.ofPattern("EEE, MMM d")),
                            tempMax = forecast.daily.tempMax[index],
                            tempMin = forecast.daily.tempMin[index],
                            sunrise = forecast.daily.sunrise[index].substringAfter("T"),
                            sunset = forecast.daily.sunset[index].substringAfter("T"),
                            weatherCode = weatherCode,
                            moonPhase = moonPhase,
                            hourlyTemp = hourlyTemp,
                            hourlyRain = forecast.hourly?.rain?.safeSubList(startIdx, endIdx) ?: emptyList(),
                            hourlyWind = forecast.hourly?.windSpeed10m?.safeSubList(startIdx, endIdx) ?: emptyList(),
                            hourlyTide = hourlyTide,
                            hourlyWeatherCode = forecast.hourly?.weatherCode?.safeSubList(startIdx, endIdx) ?: emptyList(),
                            hourlyIsDay = hourlyIsDay,
                            hourlyHumidity = forecast.hourly?.humidity?.safeSubList(startIdx, endIdx) ?: emptyList(),
                            hourlyUV = forecast.hourly?.uvIndex?.safeSubList(startIdx, endIdx) ?: emptyList(),
                            dailyUVMax = forecast.daily.uvIndexMax?.getOrNull(index) ?: 0.0,
                            aqi = airQuality?.hourly?.aqi?.getOrNull(index * 24 + (if (index == 0) localNow.hour else 0)) ?: 0.0,
                            hourlyHours = List(hourlyTemp.size) { (startIdx + it) % 24 },
                            dailyHighTide = marine?.daily?.highTides?.getOrNull(index),
                            dailyLowTide = marine?.daily?.lowTides?.getOrNull(index),
                            hourlyPollen = hourlyPollen
                        )
                    }
                }
                withContext(Dispatchers.Main) {
                    _uiState.value = if (uiModels.isEmpty()) WeatherUiState.Error("No data") else WeatherUiState.Success(uiModels)
                }
            } catch (e: Exception) {
                Log.e("WeatherViewModel", "Fetch Error", e)
                withContext(Dispatchers.Main) { _uiState.value = WeatherUiState.Error(e.message ?: "Error") }
            }
        }
    }

    private fun <T> List<T>.safeSubList(from: Int, to: Int): List<T> {
        if (from >= size) return emptyList()
        return subList(maxOf(0, from), minOf(to, size))
    }
}

sealed class WeatherUiState {
    object Loading : WeatherUiState()
    data class Success(val forecast: List<WeatherUIModel>) : WeatherUiState()
    data class Error(val message: String) : WeatherUiState()
}
