package com.example.weatherapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.weatherapp.data.remote.RealtimeManager
import com.example.weatherapp.ui.WeatherJournalApp
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Main entry point for the Weather Journal application.
 * Uses Jetpack Compose for UI and Hilt for dependency injection.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var realtimeManager: RealtimeManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Connect automatically
        realtimeManager.connect()

        setContent {
            WeatherJournalApp()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        realtimeManager.disconnect()
    }
}