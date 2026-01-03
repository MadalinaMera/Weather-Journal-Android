package com.example.weatherapp.data.remote.api

import com.example.weatherapp.data.remote.dto.CreateEntryRequest
import com.example.weatherapp.data.remote.dto.EntryResponse
import com.example.weatherapp.data.remote.dto.PaginatedEntriesResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit API interface for Weather Journal backend.
 * 
 * Mirrors the API endpoints from the original api.ts:
 * ```typescript
 * const API_URL = 'http://localhost:3001';
 * 
 * // Endpoints:
 * // GET  /entries?page=1&limit=10
 * // POST /entries
 * // PUT  /entries/:id
 * ```
 * 
 * All endpoints (except auth) require JWT token in Authorization header.
 * Token is automatically added by AuthInterceptor.
 */
interface WeatherJournalApi {
    
    /**
     * Get paginated journal entries.
     * 
     * Original:
     * ```typescript
     * async getEntries(page: number = 1, limit: number = 10): Promise<PaginatedResponse>
     * ```
     * 
     * @param page Page number (1-indexed)
     * @param limit Number of entries per page
     * @return Paginated response with entries
     */
    @GET("entries")
    suspend fun getEntries(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10
    ): Response<PaginatedEntriesResponse>
    
    /**
     * Create a new journal entry.
     * 
     * Original:
     * ```typescript
     * async addEntry(entry: Omit<WeatherEntry, 'id'>): Promise<WeatherEntry>
     * ```
     * 
     * @param entry Entry data to create
     * @return Created entry with server-generated fields
     */
    @POST("entries")
    suspend fun createEntry(
        @Body entry: CreateEntryRequest
    ): Response<EntryResponse>
    
    /**
     * Update an existing journal entry.
     * 
     * Original:
     * ```typescript
     * async updateEntry(id: string, entry: Omit<WeatherEntry, 'id'>): Promise<WeatherEntry>
     * ```
     * 
     * @param id Entry ID to update
     * @param entry Updated entry data
     * @return Updated entry
     */
    @PUT("entries/{id}")
    suspend fun updateEntry(
        @Path("id") id: String,
        @Body entry: CreateEntryRequest
    ): Response<EntryResponse>
}
