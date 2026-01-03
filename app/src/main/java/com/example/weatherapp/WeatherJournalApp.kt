package com.example.weatherapp

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.example.weatherapp.worker.SyncWorker // Fixed import
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import javax.inject.Named
import okhttp3.OkHttpClient

@HiltAndroidApp
class WeatherJournalApp : Application(), Configuration.Provider, ImageLoaderFactory {

    companion object {
        private const val TAG = "WeatherJournalApp"
    }

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    // Inject the client we configured with TLS 1.2+
    @Inject
    @Named("publicClient")
    lateinit var publicOkHttpClient: OkHttpClient

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Application onCreate")
        schedulePeriodicSync()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(Log.DEBUG)
            .build()

    // Configure Coil to use our fixed OkHttpClient
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .okHttpClient(publicOkHttpClient)
            .crossfade(true)
            .build()
    }

    private fun schedulePeriodicSync() {
        SyncWorker.schedulePeriodicSync(this)
        Log.d(TAG, "Scheduled periodic sync")
    }
}