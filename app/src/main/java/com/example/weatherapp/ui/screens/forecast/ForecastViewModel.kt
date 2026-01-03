package com.example.weatherapp.ui.screens.forecast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val weatherApi: WeatherApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForecastUiState())
    val uiState: StateFlow<ForecastUiState> = _uiState.asStateFlow()

    // Store the last known coordinates
    private var lastLatitude: Double? = null
    private var lastLongitude: Double? = null

    init {
        loadWeatherByLocation(44.432,26.106) //bucharest
    }

    fun refresh() {
        if (lastLatitude != null && lastLongitude != null) {
            loadWeatherByLocation(lastLatitude!!, lastLongitude!!)
        } else {
            // Fallback to default logic if no location is stored yet
            loadWeather()
        }
    }

    fun loadWeatherByLocation(lat: Double, lon: Double) {
        // 1. Save the coordinates so refresh() can use them later
        lastLatitude = lat
        lastLongitude = lon

        val apiKey = Constants.Api.OPENWEATHER_API_KEY

        if (apiKey.isBlank()) {
            _uiState.update { it.copy(error = "API Key not configured in Constants.kt") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val response = weatherApi.getCurrentWeather(
                    lat = lat,
                    lon = lon,
                    apiKey = apiKey
                )

                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!
                    val weatherItem = data.weather.firstOrNull()

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            weather = WeatherData(
                                cityName = data.name,
                                country = data.sys.country,
                                temperature = data.main.temp,
                                feelsLike = data.main.feelsLike,
                                humidity = data.main.humidity,
                                description = weatherItem?.description ?: "Unknown",
                                conditionMain = weatherItem?.main ?: "Clear",
                                icon = weatherItem?.icon ?: "01d"
                            )
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Error: ${response.code()} ${response.message()}"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load weather"
                    )
                }
            }
        }
    }

    // Fallback method (e.g. for hardcoded city or initial load)
    private fun loadWeather() {
        // Currently empty or can be used to load a default city (e.g., London)
        // if the user hasn't clicked "Use Device Location" yet.
    }
}