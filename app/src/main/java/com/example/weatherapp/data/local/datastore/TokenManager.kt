package com.example.weatherapp.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.text.get

/**
 * Extension property to create DataStore instance.
 * Creates a single DataStore instance for the application.
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "weather_journal_prefs"
)

/**
 * Data class representing user session information.
 * Contains all authentication-related data.
 */
data class UserSession(
    val token: String?,
    val username: String?,
    val userId: Int?,
    val isLoggedIn: Boolean,
    val tokenExpiry: Long?
) {
    companion object {
        /**
         * Create an empty/logged out session.
         */
        val EMPTY = UserSession(
            token = null,
            username = null,
            userId = null,
            isLoggedIn = false,
            tokenExpiry = null
        )
    }
    
    /**
     * Check if the token has expired.
     */
    fun isTokenExpired(): Boolean {
        return tokenExpiry?.let { System.currentTimeMillis() > it } ?: false
    }
    
    /**
     * Check if session is valid (logged in and not expired).
     */
    fun isValid(): Boolean {
        return isLoggedIn && !isTokenExpired() && !token.isNullOrBlank()
    }
}

/**
 * TokenManager manages authentication tokens and user session data
 * using Preferences DataStore.
 * 
 * This replaces the localStorage-based token storage from the original:
 * ```typescript
 * localStorage.setItem('jwt_token', data.token);
 * localStorage.setItem('username', data.username);
 * ```
 * 
 * Features:
 * - Secure token storage using DataStore
 * - Reactive session state via Flow
 * - Token expiry tracking
 * - Clean logout functionality
 * 
 * Usage:
 * ```kotlin
 * // Save token after login
 * tokenManager.saveToken(token, username)
 * 
 * // Get current token
 * val token = tokenManager.getToken()
 * 
 * // Observe session state
 * tokenManager.userSession.collect { session ->
 *     if (session.isLoggedIn) { /* ... */ }
 * }
 * 
 * // Logout
 * tokenManager.clearSession()
 * ```
 */
@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    /**
     * Preference keys for DataStore.
     */
    private object PreferenceKeys {
        val JWT_TOKEN = stringPreferencesKey("jwt_token")
        val USERNAME = stringPreferencesKey("username")
        val USER_ID = stringPreferencesKey("user_id")
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val TOKEN_EXPIRY = longPreferencesKey("token_expiry")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val LAST_SYNC_TIME = longPreferencesKey("last_sync_time")
    }
    
    /**
     * DataStore instance.
     */
    private val dataStore = context.dataStore
    
    /**
     * Flow of current user session.
     * Emits whenever session data changes.
     * 
     * Use this for reactive UI updates:
     * ```kotlin
     * viewModel.userSession.collect { session ->
     *     if (!session.isLoggedIn) navigateToLogin()
     * }
     * ```
     */
    val userSession: Flow<UserSession> = dataStore.data
        .catch { exception ->
            // Handle errors - emit empty preferences on IOException
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            UserSession(
                token = preferences[PreferenceKeys.JWT_TOKEN],
                username = preferences[PreferenceKeys.USERNAME],
                userId = preferences[PreferenceKeys.USER_ID]?.toIntOrNull(),
                isLoggedIn = preferences[PreferenceKeys.IS_LOGGED_IN] ?: false,
                tokenExpiry = preferences[PreferenceKeys.TOKEN_EXPIRY]
            )
        }
    
    /**
     * Flow of just the authentication token.
     */
    val tokenFlow: Flow<String?> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferenceKeys.JWT_TOKEN]
        }
    
    /**
     * Flow indicating if user is logged in.
     */
    val isLoggedIn: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferenceKeys.IS_LOGGED_IN] ?: false
        }
    
    /**
     * Save authentication data after successful login.
     * 
     * Mirrors the original:
     * ```typescript
     * localStorage.setItem('jwt_token', data.token);
     * localStorage.setItem('username', data.username);
     * setIsAuthenticated(true);
     * ```
     * 
     * @param token JWT token from server
     * @param username User's username
     * @param userId User's ID (optional)
     * @param expiresInMillis Token validity duration in milliseconds (default 24 hours)
     */
    suspend fun saveToken(
        token: String,
        username: String,
        userId: Int? = null,
        expiresInMillis: Long = 24 * 60 * 60 * 1000 // 24 hours default
    ) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.JWT_TOKEN] = token
            preferences[PreferenceKeys.USERNAME] = username
            preferences[PreferenceKeys.IS_LOGGED_IN] = true
            preferences[PreferenceKeys.TOKEN_EXPIRY] = System.currentTimeMillis() + expiresInMillis
            
            userId?.let {
                preferences[PreferenceKeys.USER_ID] = it.toString()
            }
        }
    }
    
    /**
     * Save refresh token (if using refresh token flow).
     */
    suspend fun saveRefreshToken(refreshToken: String) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.REFRESH_TOKEN] = refreshToken
        }
    }
    
    /**
     * Get current token synchronously (blocking).
     * Use tokenFlow for reactive access.
     * 
     * @return Current JWT token or null if not logged in
     */
    suspend fun getToken(): String? {
        return dataStore.data.first()[PreferenceKeys.JWT_TOKEN]
    }
    
    /**
     * Get current username synchronously.
     */
    suspend fun getUsername(): String? {
        return dataStore.data.first()[PreferenceKeys.USERNAME]
    }
    
    /**
     * Get current user ID synchronously.
     */
    suspend fun getUserId(): Int? {
        return dataStore.data.first()[PreferenceKeys.USER_ID]?.toIntOrNull()
    }
    
    /**
     * Get refresh token (if available).
     */
    suspend fun getRefreshToken(): String? {
        return dataStore.data.first()[PreferenceKeys.REFRESH_TOKEN]
    }
    
    /**
     * Get current session synchronously.
     */
    suspend fun getSession(): UserSession {
        return userSession.first()
    }
    
    /**
     * Check if currently logged in (synchronous).
     */
    suspend fun isAuthenticated(): Boolean {
        val session = getSession()
        return session.isValid()
    }
    
    /**
     * Clear all session data (logout).
     * 
     * Mirrors the original:
     * ```typescript
     * const logout = () => {
     *     localStorage.removeItem('jwt_token');
     *     localStorage.removeItem('username');
     *     setIsAuthenticated(false);
     * };
     * ```
     */
    suspend fun clearSession() {
        dataStore.edit { preferences ->
            preferences.remove(PreferenceKeys.JWT_TOKEN)
            preferences.remove(PreferenceKeys.USERNAME)
            preferences.remove(PreferenceKeys.USER_ID)
            preferences.remove(PreferenceKeys.REFRESH_TOKEN)
            preferences.remove(PreferenceKeys.TOKEN_EXPIRY)
            preferences[PreferenceKeys.IS_LOGGED_IN] = false
        }
    }
    
    /**
     * Update token (e.g., after refresh).
     */
    suspend fun updateToken(
        newToken: String,
        expiresInMillis: Long = 24 * 60 * 60 * 1000
    ) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.JWT_TOKEN] = newToken
            preferences[PreferenceKeys.TOKEN_EXPIRY] = System.currentTimeMillis() + expiresInMillis
        }
    }
    
    // ==================== Sync Preferences ====================
    
    /**
     * Save last sync timestamp.
     */
    suspend fun saveLastSyncTime(timestamp: Long = System.currentTimeMillis()) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.LAST_SYNC_TIME] = timestamp
        }
    }
    
    /**
     * Get last sync timestamp.
     */
    suspend fun getLastSyncTime(): Long? {
        return dataStore.data.first()[PreferenceKeys.LAST_SYNC_TIME]
    }
    
    /**
     * Flow of last sync time.
     */
    val lastSyncTime: Flow<Long?> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferenceKeys.LAST_SYNC_TIME]
        }
}

/**
 * Extension to format token expiry as readable string.
 */
fun UserSession.getExpiryTimeFormatted(): String? {
    return tokenExpiry?.let {
        val remaining = it - System.currentTimeMillis()
        if (remaining <= 0) {
            "Expired"
        } else {
            val hours = remaining / (1000 * 60 * 60)
            val minutes = (remaining % (1000 * 60 * 60)) / (1000 * 60)
            "${hours}h ${minutes}m"
        }
    }
}
