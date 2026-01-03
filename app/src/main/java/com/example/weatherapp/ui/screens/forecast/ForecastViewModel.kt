package com.example.weatherapp.ui.screens.forecast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
class ForecastViewModel @Inject constructor() : ViewModel() {
    
    private val _uiState = MutableStateFlow(ForecastUiState())
    val uiState: StateFlow<ForecastUiState> = _uiState.asStateFlow()
    
    init {
        // Show mock data if no API key is set
        if (Constants.Api.OPENWEATHER_API_KEY.isBlank()) {
            // Keep state empty to show mock UI
        } else {
            loadWeather()
        }
    }
    
    fun refresh() {
        loadWeather()
    }
    
    private fun loadWeather() {
        if (Constants.Api.OPENWEATHER_API_KEY.isBlank()) {
            _uiState.update { it.copy(error = "No API key configured") }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                // TODO: Implement actual API call with location
                // For now, show mock data
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        weather = WeatherData(
                            cityName = "Sample City",
                            country = "US",
                            temperature = 22.0,
                            feelsLike = 20.0,
                            humidity = 65,
                            description = "partly cloudy",
                            conditionMain = "Clouds",
                            icon = "02d"
                        )
                    )
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
}
