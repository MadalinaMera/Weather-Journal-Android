package com.example.weatherapp.ui.screens.forecast

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weatherapp.data.local.database.JournalDao
import com.example.weatherapp.data.local.database.entity.WeatherEntity
import com.example.weatherapp.data.remote.api.WeatherApi
import com.example.weatherapp.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WeatherData(
    val cityName: String,
    val country: String,
    val temperature: Double,
    val feelsLike: Double,
    val humidity: Int,
    val description: String,
    val conditionMain: String,
    val icon: String
)

data class ForecastUiState(
    val isLoading: Boolean = false,
    val weather: WeatherData? = null,
    val error: String? = null
)

@HiltViewModel
class ForecastViewModel @Inject constructor(
    private val weatherApi: WeatherApi,
    private val journalDao: JournalDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForecastUiState())
    val uiState: StateFlow<ForecastUiState> = _uiState.asStateFlow()

    private var lastLatitude: Double? = null
    private var lastLongitude: Double? = null

    init {
        // 2. Load cached weather immediately on startup
        loadCachedWeather()

        // Then try to fetch fresh data
        loadWeatherByLocation(Constants.Location.DEFAULT_LAT, Constants.Location.DEFAULT_LONG)
    }

    // New helper to load from DB
    private fun loadCachedWeather() {
        viewModelScope.launch {
            val cached = journalDao.getLastWeather()
            if (cached != null) {
                Log.d("WeatherCache", "✅ Found cached weather for: ${cached.cityName}")
                _uiState.update {
                    it.copy(
                        weather = WeatherData(
                            cityName = cached.cityName,
                            country = cached.country,
                            temperature = cached.temperature,
                            feelsLike = cached.feelsLike,
                            humidity = cached.humidity,
                            description = cached.description,
                            conditionMain = cached.conditionMain,
                            icon = cached.icon
                        )
                    )
                }
            }else android.util.Log.e("WeatherCache", "❌ Cache is EMPTY")
        }
    }

    fun refresh() {
        if (lastLatitude != null && lastLongitude != null) {
            loadWeatherByLocation(lastLatitude!!, lastLongitude!!)
        }
    }

    fun loadWeatherByLocation(lat: Double, lon: Double) {
        lastLatitude = lat
        lastLongitude = lon
        val apiKey = Constants.Api.OPENWEATHER_API_KEY

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val response = weatherApi.getCurrentWeather(lat, lon, apiKey)

                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!
                    val weatherItem = data.weather.firstOrNull()

                    // Create UI Object
                    val weatherData = WeatherData(
                        cityName = data.name,
                        country = data.sys.country,
                        temperature = data.main.temp,
                        feelsLike = data.main.feelsLike,
                        humidity = data.main.humidity,
                        description = weatherItem?.description ?: "Unknown",
                        conditionMain = weatherItem?.main ?: "Clear",
                        icon = weatherItem?.icon ?: "01d"
                    )

                    // 3. Update UI
                    _uiState.update {
                        it.copy(isLoading = false, weather = weatherData)
                    }

                    android.util.Log.d("WeatherCache", "💾 Saving new weather to DB...")
                    // 4. Save to Database (Cache it)
                    journalDao.saveWeather(
                        WeatherEntity(
                            cityName = weatherData.cityName,
                            country = weatherData.country,
                            temperature = weatherData.temperature,
                            feelsLike = weatherData.feelsLike,
                            humidity = weatherData.humidity,
                            description = weatherData.description,
                            conditionMain = weatherData.conditionMain,
                            icon = weatherData.icon
                        )
                    )

                } else {
                    // On API error, we just stop loading (we already have cached data visible)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Error: ${response.code()} (Using cached data)"
                        )
                    }
                }
            } catch (e: Exception) {
                // On Network error, stop loading and show error message
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Offline: Showing last known weather"
                    )
                }
            }
        }
    }
}