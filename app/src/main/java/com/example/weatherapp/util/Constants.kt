package com.example.weatherapp.util

/**
 * Application-wide constants.
 */
object Constants {
    
    /**
     * API Configuration
     */
    object Api {
        /**
         * Base URL for the Weather Journal backend API.
         * Change this to your server URL in production.
         * 
         * For emulator testing:
         * - Use 10.0.2.2 instead of localhost
         * - Ensure your server is running on port 3001
         */
        const val BASE_URL = "http://192.168.0.103:3001/"
        
        /**
         * OpenWeatherMap API
         */
        const val WEATHER_API_BASE_URL = "https://api.openweathermap.org/"
        
        /**
         * Your OpenWeatherMap API key.
         * Get one free at: https://openweathermap.org/api
         * 
         * TODO: Move to BuildConfig or secrets management in production
         */
        const val OPENWEATHER_API_KEY = "6b7cc26643fcd8bab44289dbd26c777e" // Add your API key here
        
        /**
         * Network timeouts in seconds.
         */
        const val CONNECT_TIMEOUT = 30L
        const val READ_TIMEOUT = 30L
        const val WRITE_TIMEOUT = 30L
    }
    
    /**
     * Database Configuration
     */
    object Database {
        const val NAME = "weather_journal_db"
        const val VERSION = 1
    }
    
    /**
     * DataStore Configuration
     */
    object DataStore {
        const val PREFERENCES_NAME = "weather_journal_prefs"
    }
    
    /**
     * Sync Configuration
     */
    object Sync {
        /**
         * Periodic sync interval in minutes.
         * Minimum is 15 minutes for WorkManager.
         */
        const val PERIODIC_SYNC_INTERVAL_MINUTES = 15L
        
        /**
         * Maximum retry attempts for failed sync operations.
         */
        const val MAX_SYNC_RETRIES = 5
        
        /**
         * Default page size for pagination.
         */
        const val DEFAULT_PAGE_SIZE = 10
    }
    
    /**
     * Pagination
     */
    object Pagination {
        const val INITIAL_PAGE = 1
        const val PAGE_SIZE = 10
    }
    
    /**
     * JWT Configuration
     */
    object Jwt {
        /**
         * Default token expiry time (24 hours in milliseconds).
         */
        const val DEFAULT_EXPIRY_MS = 24 * 60 * 60 * 1000L
    }
    
    /**
     * Navigation Routes
     */
    object Routes {
        const val LOGIN = "login"
        const val REGISTER = "register"
        const val JOURNAL = "journal"
        const val FORECAST = "forecast"
        const val ENTRY_DETAIL = "entry/{entryId}"
        const val ENTRY_EDIT = "entry/edit/{entryId}"
        const val ENTRY_CREATE = "entry/create"
        const val SETTINGS = "settings"
        
        fun entryDetail(entryId: String) = "entry/$entryId"
        fun entryEdit(entryId: String) = "entry/edit/$entryId"
    }
    
    /**
     * Intent Actions
     */
    object Actions {
        const val ACTION_SYNC = "com.weatherjournal.ACTION_SYNC"
        const val ACTION_LOGOUT = "com.weatherjournal.ACTION_LOGOUT"
    }
    
    /**
     * Request Codes
     */
    object RequestCodes {
        const val LOCATION_PERMISSION = 1001
        const val CAMERA_PERMISSION = 1002
        const val NOTIFICATION_PERMISSION = 1003
    }
}
