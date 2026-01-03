package com.example.weatherapp.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.example.weatherapp.data.local.datastore.TokenManager
import com.example.weatherapp.data.remote.api.AuthApi
import com.example.weatherapp.data.remote.api.WeatherJournalApi
import com.example.weatherapp.data.remote.interceptor.AuthInterceptor
import com.example.weatherapp.data.remote.interceptor.UnauthorizedInterceptor
import com.example.weatherapp.util.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

/**
 * Hilt module providing networking dependencies.
 * 
 * Provides:
 * - Gson instance
 * - OkHttpClient with interceptors
 * - Retrofit instance
 * - API interfaces (WeatherJournalApi, AuthApi)
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    /**
     * Provides Gson instance for JSON serialization.
     */
    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .setLenient()
            .create()
    }
    
    /**
     * Provides HTTP logging interceptor for debugging.
     */
    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }
    
    /**
     * Provides Auth interceptor for adding JWT tokens.
     */
    @Provides
    @Singleton
    fun provideAuthInterceptor(
        tokenManager: TokenManager
    ): AuthInterceptor {
        return AuthInterceptor(tokenManager)
    }
    
    /**
     * Provides Unauthorized interceptor for handling 401s.
     */
    @Provides
    @Singleton
    fun provideUnauthorizedInterceptor(
        tokenManager: TokenManager
    ): UnauthorizedInterceptor {
        return UnauthorizedInterceptor(tokenManager)
    }
    
    /**
     * Provides OkHttpClient with all interceptors configured.
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        authInterceptor: AuthInterceptor,
        unauthorizedInterceptor: UnauthorizedInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(Constants.Api.CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(Constants.Api.READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(Constants.Api.WRITE_TIMEOUT, TimeUnit.SECONDS)
            // Order matters: auth first, then logging, then unauthorized handler
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .addInterceptor(unauthorizedInterceptor)
            .build()
    }
    
    /**
     * Provides Retrofit instance for Weather Journal API.
     */
    @Provides
    @Singleton
    @Named("journalRetrofit")
    fun provideJournalRetrofit(
        okHttpClient: OkHttpClient,
        gson: Gson
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Constants.Api.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
    
    /**
     * Provides Retrofit instance for Weather API (no auth needed).
     */
    @Provides
    @Singleton
    @Named("weatherRetrofit")
    fun provideWeatherRetrofit(
        loggingInterceptor: HttpLoggingInterceptor,
        gson: Gson
    ): Retrofit {
        val client = OkHttpClient.Builder()
            .connectTimeout(Constants.Api.CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(Constants.Api.READ_TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .build()
        
        return Retrofit.Builder()
            .baseUrl(Constants.Api.WEATHER_API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
    
    /**
     * Provides WeatherJournalApi interface.
     */
    @Provides
    @Singleton
    fun provideWeatherJournalApi(
        @Named("journalRetrofit") retrofit: Retrofit
    ): WeatherJournalApi {
        return retrofit.create(WeatherJournalApi::class.java)
    }
    
    /**
     * Provides AuthApi interface.
     */
    @Provides
    @Singleton
    fun provideAuthApi(
        @Named("journalRetrofit") retrofit: Retrofit
    ): AuthApi {
        return retrofit.create(AuthApi::class.java)
    }
}
