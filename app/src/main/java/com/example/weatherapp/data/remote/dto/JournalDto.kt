package com.example.weatherapp.data.remote.dto

import com.google.gson.annotations.SerializedName

// ==================== Authentication DTOs ====================

/**
 * Login request body.
 */
data class LoginRequest(
    @SerializedName("username")
    val username: String,
    
    @SerializedName("password")
    val password: String
)

/**
 * Login response from server.
 * 
 * Original:
 * ```typescript
 * { token: string; username: string }
 * ```
 */
data class LoginResponse(
    @SerializedName("token")
    val token: String,
    
    @SerializedName("username")
    val username: String
)

/**
 * Registration request body.
 */
data class RegisterRequest(
    @SerializedName("username")
    val username: String,
    
    @SerializedName("password")
    val password: String
)

/**
 * Registration response from server.
 */
data class RegisterResponse(
    @SerializedName("message")
    val message: String,
    
    @SerializedName("user")
    val user: UserDto?
)

/**
 * User DTO.
 */
data class UserDto(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("username")
    val username: String
)

// ==================== Journal Entry DTOs ====================

/**
 * Coordinates DTO for API communication.
 */
data class CoordsDto(
    @SerializedName("latitude")
    val latitude: Double,
    
    @SerializedName("longitude")
    val longitude: Double
)

/**
 * Request body for creating/updating a journal entry.
 * 
 * Mirrors the original TypeScript:
 * ```typescript
 * const entryData = {
 *     date: entry ? entry.date : new Date().toISOString(),
 *     temperature: tempNum,
 *     description,
 *     photoUrl: photoUrl || undefined,
 *     coords
 * };
 * ```
 */
data class CreateEntryRequest(
    @SerializedName("id")
    val id: String? = null,
    
    @SerializedName("date")
    val date: String,
    
    @SerializedName("temperature")
    val temperature: Double,
    
    @SerializedName("description")
    val description: String,
    
    @SerializedName("photoUrl")
    val photoUrl: String? = null,
    
    @SerializedName("coords")
    val coords: CoordsDto
) {
    /**
     * Alternative constructor with separate lat/lon.
     */
    constructor(
        id: String?,
        date: String,
        temperature: Double,
        description: String,
        photoUrl: String?,
        latitude: Double,
        longitude: Double
    ) : this(id, date, temperature, description, photoUrl, CoordsDto(latitude, longitude))
    
    /**
     * Nested CoordsDto for use in companion object.
     */
    data class CoordsDto(
        @SerializedName("latitude")
        val latitude: Double,
        
        @SerializedName("longitude")
        val longitude: Double
    )
}

/**
 * Response DTO for a single journal entry.
 * 
 * Mirrors the server response format:
 * ```javascript
 * const newEntry = {
 *     ...rawEntry,
 *     photoUrl: rawEntry.photo_url,
 *     coords: typeof rawEntry.coords === 'string' ? JSON.parse(rawEntry.coords) : rawEntry.coords
 * };
 * ```
 */
data class EntryResponse(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("user_id")
    val userId: Int?,
    
    @SerializedName("date")
    val date: String,
    
    @SerializedName("temperature")
    val temperature: Double,
    
    @SerializedName("description")
    val description: String,
    
    @SerializedName("photoUrl")
    val photoUrl: String?,
    
    @SerializedName("photo_url")
    val photoUrlSnakeCase: String?,
    
    @SerializedName("coords")
    val coords: CoordsDto?
) {
    /**
     * Get photo URL (handles both camelCase and snake_case).
     */
    fun getPhotoUrlResolved(): String? = photoUrl ?: photoUrlSnakeCase
}

/**
 * Paginated response for entries list.
 * 
 * Original:
 * ```typescript
 * export interface PaginatedResponse {
 *   entries: WeatherEntry[];
 *   total: number;
 *   page: number;
 *   limit: number;
 *   hasMore: boolean;
 * }
 * ```
 */
data class PaginatedEntriesResponse(
    @SerializedName("entries")
    val entries: List<EntryResponse>,
    
    @SerializedName("total")
    val total: Int,
    
    @SerializedName("page")
    val page: Int,
    
    @SerializedName("limit")
    val limit: Int? = null,
    
    @SerializedName("hasMore")
    val hasMore: Boolean
)

// ==================== Weather DTOs ====================

/**
 * OpenWeatherMap API response.
 * 
 * Original:
 * ```typescript
 * export interface OpenWeatherResponse {
 *   main: { temp: number; feels_like: number; humidity: number; };
 *   weather: Array<{ main: string; description: string; icon: string; }>;
 *   name: string;
 *   sys: { country: string; };
 * }
 * ```
 */
data class WeatherResponse(
    @SerializedName("main")
    val main: MainWeatherData,
    
    @SerializedName("weather")
    val weather: List<WeatherCondition>,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("sys")
    val sys: SysData
)

data class MainWeatherData(
    @SerializedName("temp")
    val temp: Double,
    
    @SerializedName("feels_like")
    val feelsLike: Double,
    
    @SerializedName("humidity")
    val humidity: Int,
    
    @SerializedName("temp_min")
    val tempMin: Double? = null,
    
    @SerializedName("temp_max")
    val tempMax: Double? = null,
    
    @SerializedName("pressure")
    val pressure: Int? = null
)

data class WeatherCondition(
    @SerializedName("id")
    val id: Int? = null,
    
    @SerializedName("main")
    val main: String,
    
    @SerializedName("description")
    val description: String,
    
    @SerializedName("icon")
    val icon: String
)

data class SysData(
    @SerializedName("country")
    val country: String,
    
    @SerializedName("sunrise")
    val sunrise: Long? = null,
    
    @SerializedName("sunset")
    val sunset: Long? = null
)

/**
 * Geocoding API response for city search.
 */
data class GeocodingResponse(
    @SerializedName("name")
    val name: String,
    
    @SerializedName("lat")
    val lat: Double,
    
    @SerializedName("lon")
    val lon: Double,
    
    @SerializedName("country")
    val country: String,
    
    @SerializedName("state")
    val state: String? = null
)

// ==================== Error DTOs ====================

/**
 * API error response.
 */
data class ApiErrorResponse(
    @SerializedName("error")
    val error: String,
    
    @SerializedName("message")
    val message: String? = null
)
