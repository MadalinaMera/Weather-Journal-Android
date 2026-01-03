package com.example.weatherapp.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Coordinates data class for storing latitude/longitude.
 * Used as an embedded object in JournalEntity.
 */
data class Coordinates(
    @ColumnInfo(name = "latitude")
    val latitude: Double,
    
    @ColumnInfo(name = "longitude")
    val longitude: Double
)

/**
 * Room Entity representing a Journal Entry.
 * 
 * Maps to the 'journal_entries' table in the local SQLite database.
 * This entity mirrors the WeatherEntry interface from the original TypeScript:
 * 
 * Original TypeScript interface:
 * ```
 * export interface WeatherEntry {
 *   id: string;
 *   date: string;
 *   temperature: number;
 *   description: string;
 *   photoUrl?: string;
 *   coords: { latitude: number; longitude: number; };
 * }
 * ```
 */
@Entity(tableName = "journal_entries")
data class JournalEntity(
    /**
     * Unique identifier for the entry.
     * Generated as UUID on creation, or received from server.
     */
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    
    /**
     * The user ID who owns this entry.
     * Used for multi-user support and data isolation.
     */
    @ColumnInfo(name = "user_id")
    val userId: Int? = null,
    
    /**
     * Date and time when the entry was created.
     * Stored as ISO 8601 format string (e.g., "2024-01-15T10:30:00Z").
     */
    @ColumnInfo(name = "date")
    val date: String,
    
    /**
     * Temperature recorded at the time, in Celsius.
     */
    @ColumnInfo(name = "temperature")
    val temperature: Double,
    
    /**
     * User's description of the weather conditions.
     */
    @ColumnInfo(name = "description")
    val description: String,
    
    /**
     * Optional photo URL or Base64 data URI.
     * Can be:
     * - Remote URL (https://...)
     * - Base64 data URI (data:image/jpeg;base64,...)
     * - null if no photo
     */
    @ColumnInfo(name = "photo_url")
    val photoUrl: String? = null,
    
    /**
     * Geographic coordinates where the entry was recorded.
     * Embedded as separate columns: latitude and longitude.
     */
    @Embedded
    val coords: Coordinates,
    
    /**
     * Flag indicating if this entry has been synced with the server.
     * Used for offline-first synchronization.
     * - true: Entry exists on server
     * - false: Entry pending upload
     */
    @ColumnInfo(name = "is_synced", defaultValue = "0")
    val isSynced: Boolean = false,
    
    /**
     * Timestamp of the last modification.
     * Used for conflict resolution during sync.
     */
    @ColumnInfo(name = "last_modified")
    val lastModified: Long = System.currentTimeMillis(),
    
    /**
     * Flag for soft delete.
     * Entries are marked as deleted locally, then removed after sync.
     */
    @ColumnInfo(name = "is_deleted", defaultValue = "0")
    val isDeleted: Boolean = false
) {
    companion object {
        /**
         * Creates a new JournalEntity for a new entry (not yet synced).
         */
        fun createNew(
            id: String,
            date: String,
            temperature: Double,
            description: String,
            photoUrl: String? = null,
            latitude: Double,
            longitude: Double
        ): JournalEntity {
            return JournalEntity(
                id = id,
                date = date,
                temperature = temperature,
                description = description,
                photoUrl = photoUrl,
                coords = Coordinates(latitude, longitude),
                isSynced = false,
                lastModified = System.currentTimeMillis()
            )
        }
        
        /**
         * Creates a JournalEntity from server response (already synced).
         */
        fun fromServer(
            id: String,
            userId: Int?,
            date: String,
            temperature: Double,
            description: String,
            photoUrl: String?,
            latitude: Double,
            longitude: Double
        ): JournalEntity {
            return JournalEntity(
                id = id,
                userId = userId,
                date = date,
                temperature = temperature,
                description = description,
                photoUrl = photoUrl,
                coords = Coordinates(latitude, longitude),
                isSynced = true,
                lastModified = System.currentTimeMillis()
            )
        }
    }
}

/**
 * Extension function to mark entity as synced.
 */
fun JournalEntity.markAsSynced(): JournalEntity {
    return this.copy(isSynced = true, lastModified = System.currentTimeMillis())
}

/**
 * Extension function to mark entity as modified (needs sync).
 */
fun JournalEntity.markAsModified(): JournalEntity {
    return this.copy(isSynced = false, lastModified = System.currentTimeMillis())
}
