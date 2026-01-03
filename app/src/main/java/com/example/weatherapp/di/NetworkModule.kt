package com.example.weatherapp.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.example.weatherapp.data.local.datastore.TokenManager
import com.example.weatherapp.data.remote.api.AuthApi
import com.example.weatherapp.data.remote.api.WeatherJournalApi
import com.example.weatherapp.data.remote.api.WeatherApi
import com.example.weatherapp.data.remote.interceptor.AuthInterceptor
import com.example.weatherapp.data.remote.interceptor.UnauthorizedInterceptor
import com.example.weatherapp.util.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.TlsVersion
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Collections
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder().setLenient().create()

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }

    @Provides
    @Singleton
    fun provideAuthInterceptor(tokenManager: TokenManager): AuthInterceptor =
        AuthInterceptor(tokenManager)

    @Provides
    @Singleton
    fun provideUnauthorizedInterceptor(tokenManager: TokenManager): UnauthorizedInterceptor =
        UnauthorizedInterceptor(tokenManager)

    // --- NEW: Authenticated Client for your Journal API ---
    @Provides
    @Singleton
    @Named("authenticatedClient")
    fun provideAuthenticatedOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        authInterceptor: AuthInterceptor,
        unauthorizedInterceptor: UnauthorizedInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(Constants.Api.CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(Constants.Api.READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(Constants.Api.WRITE_TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .addInterceptor(unauthorizedInterceptor)
            .build()
    }

    // --- NEW: Public Client for OpenWeatherMap (Images & Data) ---
    // Forces TLS 1.2 to fix your emulator issue and skips Auth headers
    @Provides
    @Singleton
    @Named("publicClient")
    fun providePublicOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        // Force TLS 1.2+ for compatibility with OpenWeatherMap
        val spec = ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
            .tlsVersions(TlsVersion.TLS_1_2, TlsVersion.TLS_1_3)
            .build()

        return OkHttpClient.Builder()
            .connectTimeout(Constants.Api.CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(Constants.Api.READ_TIMEOUT, TimeUnit.SECONDS)
            .connectionSpecs(Collections.singletonList(spec)) // Enforce Modern TLS
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    @Named("journalRetrofit")
    fun provideJournalRetrofit(
        @Named("authenticatedClient") okHttpClient: OkHttpClient,
        gson: Gson
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Constants.Api.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    @Named("weatherRetrofit")
    fun provideWeatherRetrofit(
        @Named("publicClient") okHttpClient: OkHttpClient, // Use public client
        gson: Gson
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Constants.Api.WEATHER_API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideWeatherJournalApi(@Named("journalRetrofit") retrofit: Retrofit): WeatherJournalApi =
        retrofit.create(WeatherJournalApi::class.java)

    @Provides
    @Singleton
    fun provideAuthApi(@Named("journalRetrofit") retrofit: Retrofit): AuthApi =
        retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideWeatherApi(@Named("weatherRetrofit") retrofit: Retrofit): WeatherApi =
        retrofit.create(WeatherApi::class.java)
}