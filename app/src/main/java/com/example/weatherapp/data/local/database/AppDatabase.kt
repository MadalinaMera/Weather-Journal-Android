package com.example.weatherapp.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.weatherapp.data.local.database.entity.JournalEntity
import com.example.weatherapp.data.local.database.entity.SyncQueueEntity
import com.example.weatherapp.data.local.database.entity.SyncOperationType
import com.example.weatherapp.data.local.database.entity.SyncStatus

/**
 * Type converters for Room to handle enum types.
 */
class Converters {
    
    // SyncOperationType converters
    @TypeConverter
    fun fromSyncOperationType(value: SyncOperationType): String {
        return value.name
    }
    
    @TypeConverter
    fun toSyncOperationType(value: String): SyncOperationType {
        return SyncOperationType.valueOf(value)
    }
    
    // SyncStatus converters
    @TypeConverter
    fun fromSyncStatus(value: SyncStatus): String {
        return value.name
    }
    
    @TypeConverter
    fun toSyncStatus(value: String): SyncStatus {
        return SyncStatus.valueOf(value)
    }
}

/**
 * Room Database for the Weather Journal application.
 * 
 * Contains tables for:
 * - journal_entries: User's weather journal entries
 * - sync_queue: Pending sync operations for offline support
 * 
 * Features:
 * - Auto migration support
 * - Type converters for custom types
 * - Singleton pattern for database instance
 */
@Database(
    entities = [
        JournalEntity::class,
        SyncQueueEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    /**
     * Access to Journal DAO for all database operations.
     */
    abstract fun journalDao(): JournalDao
    
    companion object {
        /**
         * Database file name.
         */
        const val DATABASE_NAME = "weather_journal_db"
        
        /**
         * Volatile instance for thread-safe singleton.
         */
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        /**
         * Get singleton instance of the database.
         * 
         * Uses double-checked locking for thread safety.
         * In production, use Hilt for dependency injection instead.
         * 
         * @param context Application context
         * @return AppDatabase singleton instance
         */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }
        
        /**
         * Build the Room database instance.
         */
        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                // Enable destructive migration for development
                // In production, use proper migrations
                .fallbackToDestructiveMigration()
                // Add pre-populated callback if needed
                // .addCallback(DatabaseCallback())
                .build()
        }
        
        /**
         * Clear the singleton instance.
         * Useful for testing or when user logs out.
         */
        fun clearInstance() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}

/**
 * Optional callback for database creation/opening.
 * Can be used to pre-populate data.
 */
/*
class DatabaseCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        // Pre-populate if needed
    }
    
    override fun onOpen(db: SupportSQLiteDatabase) {
        super.onOpen(db)
        // Called when database is opened
    }
}
*/
