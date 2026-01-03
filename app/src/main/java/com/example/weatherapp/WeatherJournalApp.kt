package com.example.weatherapp

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.weatherapp.worker.SyncWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application class for Weather Journal.
 * 
 * Initializes:
 * - Hilt dependency injection
 * - WorkManager with Hilt support
 * - Periodic background sync
 */
@HiltAndroidApp
class WeatherJournalApp : Application(), Configuration.Provider {
    
    companion object {
        private const val TAG = "WeatherJournalApp"
    }
    
    /**
     * Hilt-managed WorkerFactory for dependency injection in Workers.
     */
    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    override fun onCreate() {
        super.onCreate()
        
        Log.d(TAG, "Application onCreate")
        
        // Schedule periodic background sync
        schedulePeriodicSync()
    }
    
    /**
     * Provide WorkManager configuration with Hilt support.
     * This enables @HiltWorker annotation in SyncWorker.
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(Log.DEBUG)
            .build()
    
    /**
     * Schedule periodic background sync using WorkManager.
     */
    private fun schedulePeriodicSync() {
        SyncWorker.schedulePeriodicSync(this)
        Log.d(TAG, "Scheduled periodic sync")
    }
}
