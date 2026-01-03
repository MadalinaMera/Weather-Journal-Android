package com.example.weatherapp.data.remote.interceptor

import com.example.weatherapp.data.local.datastore.TokenManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttp Interceptor that adds JWT token to request headers.
 * 
 * Mirrors the original authorization header logic:
 * ```typescript
 * const response = await fetch(`${API_URL}/entries`, {
 *     method: 'GET',
 *     headers: {
 *         'Content-Type': 'application/json',
 *         'Authorization': `Bearer ${token}`
 *     }
 * });
 * ```
 * 
 * This interceptor:
 * - Adds Authorization header with Bearer token
 * - Skips auth endpoints (login, register)
 * - Handles token refresh (optional)
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {
    
    companion object {
        private const val HEADER_AUTHORIZATION = "Authorization"
        private const val BEARER_PREFIX = "Bearer "
        
        /**
         * Endpoints that don't require authentication.
         */
        private val NO_AUTH_ENDPOINTS = listOf(
            "/login",
            "/register"
        )
    }
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // Check if this endpoint requires authentication
        val path = originalRequest.url.encodedPath
        if (NO_AUTH_ENDPOINTS.any { path.endsWith(it) }) {
            return chain.proceed(originalRequest)
        }
        
        // Get token from DataStore (blocking call - necessary for interceptor)
        val token = runBlocking {
            tokenManager.tokenFlow.first()
        }
        
        // If no token, proceed without auth header
        if (token.isNullOrBlank()) {
            return chain.proceed(originalRequest)
        }
        
        // Add authorization header
        val authenticatedRequest = originalRequest.newBuilder()
            .header(HEADER_AUTHORIZATION, "$BEARER_PREFIX$token")
            .build()
        
        return chain.proceed(authenticatedRequest)
    }
}

/**
 * Interceptor for handling 401 Unauthorized responses.
 * Can be used to trigger automatic logout or token refresh.
 */
@Singleton
class UnauthorizedInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        
        // If we get a 401, clear the session
        if (response.code == 401) {
            runBlocking {
                tokenManager.clearSession()
            }
        }
        
        return response
    }
}
