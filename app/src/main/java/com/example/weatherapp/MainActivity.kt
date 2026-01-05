package com.example.weatherapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.weatherapp.data.remote.RealtimeManager
import com.example.weatherapp.ui.WeatherJournalApp
import com.example.weatherapp.ui.theme.WeatherJournalTheme
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