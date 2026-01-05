package com.example.weatherapp.di

import android.content.Context
import android.hardware.SensorManager
import androidx.work.WorkManager
import com.example.weatherapp.data.local.datastore.TokenManager
import com.example.weatherapp.util.NetworkMonitor
import com.example.weatherapp.util.NotificationHelper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing general application dependencies.
 * 
 * Provides:
 * - TokenManager (DataStore)
 * - NetworkMonitor
 * - NotificationHelper
 * - WorkManager
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    /**
     * Provides TokenManager for secure token storage.
     */
    @Provides
    @Singleton
    fun provideTokenManager(
        @ApplicationContext context: Context
    ): TokenManager {
        return TokenManager(context)
    }
    
    /**
     * Provides NetworkMonitor for connectivity status.
     */
    @Provides
    @Singleton
    fun provideNetworkMonitor(
        @ApplicationContext context: Context
    ): NetworkMonitor {
        return NetworkMonitor(context)
    }
    
    /**
     * Provides NotificationHelper for system notifications.
     */
    @Provides
    @Singleton
    fun provideNotificationHelper(
        @ApplicationContext context: Context
    ): NotificationHelper {
        return NotificationHelper(context)
    }
    
    /**
     * Provides WorkManager instance.
     */
    @Provides
    @Singleton
    fun provideWorkManager(
        @ApplicationContext context: Context
    ): WorkManager {
        return WorkManager.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideFusedLocationProviderClient(
        @ApplicationContext context: Context
    ): FusedLocationProviderClient {
        return LocationServices.getFusedLocationProviderClient(context)
    }

    @Provides
    @Singleton
    fun provideSensorManager(
        @ApplicationContext context: Context
    ): SensorManager {
        return context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
}
