package com.example.weatherapp.data.remote.api

import com.example.weatherapp.data.remote.dto.LoginRequest
import com.example.weatherapp.data.remote.dto.LoginResponse
import com.example.weatherapp.data.remote.dto.RegisterRequest
import com.example.weatherapp.data.remote.dto.RegisterResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit API interface for authentication endpoints.
 * 
 * These endpoints do NOT require authorization header.
 * 
 * Mirrors the original api.ts:
 * ```typescript
 * async login(username: string, password: string): Promise<{ token: string; username: string }>
 * async register(username: string, password: string): Promise<void>
 * ```
 */
interface AuthApi {
    
    /**
     * User login.
     * 
     * @param request Login credentials
     * @return JWT token and username on success
     */
    @POST("login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>
    
    /**
     * User registration.
     * 
     * @param request Registration details
     * @return Registration confirmation
     */
    @POST("register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<RegisterResponse>
}
