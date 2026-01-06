package com.example.weatherapp.util

import com.example.weatherapp.R

object WeatherIconMapper {
    fun getIconResource(iconCode: String): Int {
        return when (iconCode) {
            // Clear Sky
            "01d" -> R.drawable.ic_clear_day
            "01n" -> R.drawable.ic_clear_day // Or add a moon icon for night

            // Clouds (Few, Scattered, Broken)
            "02d", "02n" -> R.drawable.ic_cloudy
            "03d", "03n", "04d", "04n" -> R.drawable.ic_cloudy

            // Rain & Drizzle
            "09d", "09n", "10d", "10n" -> R.drawable.ic_rain

            // Thunderstorm
            "11d", "11n" -> R.drawable.ic_thunderstorm

            // Snow
            "13d", "13n" -> R.drawable.ic_snow

            // Mist/Fog
            "50d", "50n" -> R.drawable.ic_cloudy // Fallback to cloudy or add specific mist icon

            // Default fallback
            else -> R.drawable.ic_clear_day
        }
    }
}