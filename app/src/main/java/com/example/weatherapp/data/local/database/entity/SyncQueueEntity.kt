package com.example.weatherapp.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Enum representing the type of sync operation.
 * Mirrors the SyncItem interface from the original TypeScript:
 * 
 * ```typescript
 * interface SyncItem {
 *     type: 'ADD' | 'UPDATE';
 *     data: Omit<WeatherEntry, 'id'>;
 *     id?: string;
 * }
 * ```
 */
enum class SyncOperationType {
    ADD,
    UPDATE,
    DELETE
}

/**
 * Enum representing the status of a sync operation.
 */
enum class SyncStatus {
    PENDING,      // Waiting to be synced
    IN_PROGRESS,  // Currently being processed
    COMPLETED,    // Successfully synced
    FAILED        // Sync failed, will retry
}

/**
 * Room Entity representing a pending sync operation.
 * 
 * This entity maintains a queue of operations that need to be
 * synchronized with the server when connectivity is restored.
 * 
 * This replaces the localStorage-based QUEUE_KEY from the original:
 * ```typescript
 * const QUEUE_KEY = 'sync_queue';
 * queue.push({ type: 'ADD', data: entryData });
 * localStorage.setItem(QUEUE_KEY, JSON.stringify(queue));
 * ```
 */
@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    /**
     * Auto-generated primary key for queue ordering.
     */
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "queue_id")
    val queueId: Long = 0,
    
    /**
     * The ID of the journal entry this operation relates to.
     * For ADD operations, this is the locally-generated UUID.
     * For UPDATE/DELETE, this is the server-provided ID.
     */
    @ColumnInfo(name = "entry_id")
    val entryId: String,
    
    /**
     * Type of operation: ADD, UPDATE, or DELETE.
     */
    @ColumnInfo(name = "operation_type")
    val operationType: SyncOperationType,
    
    /**
     * Current status of this sync operation.
     */
    @ColumnInfo(name = "status")
    val status: SyncStatus = SyncStatus.PENDING,
    
    /**
     * JSON serialized data for the operation.
     * Contains the entry data that needs to be sent to the server.
     * 
     * For ADD/UPDATE: Full entry data (date, temperature, description, etc.)
     * For DELETE: May be empty or contain entry ID only
     */
    @ColumnInfo(name = "payload")
    val payload: String,
    
    /**
     * Timestamp when this operation was queued.
     * Used for ordering and cleanup.
     */
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    /**
     * Timestamp of the last sync attempt.
     * Updated each time we try to process this item.
     */
    @ColumnInfo(name = "last_attempt_at")
    val lastAttemptAt: Long? = null,
    
    /**
     * Number of failed sync attempts.
     * Used for exponential backoff and eventual removal.
     */
    @ColumnInfo(name = "retry_count")
    val retryCount: Int = 0,
    
    /**
     * Error message from the last failed attempt.
     * Useful for debugging and user feedback.
     */
    @ColumnInfo(name = "last_error")
    val lastError: String? = null
) {
    companion object {
        /**
         * Maximum number of retry attempts before giving up.
         */
        const val MAX_RETRY_COUNT = 5
        
        /**
         * Creates a new ADD operation for the sync queue.
         */
        fun createAddOperation(entryId: String, payload: String): SyncQueueEntity {
            return SyncQueueEntity(
                entryId = entryId,
                operationType = SyncOperationType.ADD,
                payload = payload
            )
        }
        
        /**
         * Creates a new UPDATE operation for the sync queue.
         */
        fun createUpdateOperation(entryId: String, payload: String): SyncQueueEntity {
            return SyncQueueEntity(
                entryId = entryId,
                operationType = SyncOperationType.UPDATE,
                payload = payload
            )
        }
        
        /**
         * Creates a new DELETE operation for the sync queue.
         */
        fun createDeleteOperation(entryId: String): SyncQueueEntity {
            return SyncQueueEntity(
                entryId = entryId,
                operationType = SyncOperationType.DELETE,
                payload = ""
            )
        }
    }
    
    /**
     * Check if this operation should be retried.
     */
    fun shouldRetry(): Boolean {
        return status == SyncStatus.FAILED && retryCount < MAX_RETRY_COUNT
    }
    
    /**
     * Calculate delay before next retry using exponential backoff.
     * Returns delay in milliseconds.
     */
    fun getRetryDelay(): Long {
        val baseDelay = 1000L // 1 second
        val maxDelay = 60000L // 1 minute max
        val delay = baseDelay * (1L shl minOf(retryCount, 6)) // 2^retryCount
        return minOf(delay, maxDelay)
    }
}

/**
 * Extension to mark operation as in progress.
 */
fun SyncQueueEntity.markInProgress(): SyncQueueEntity {
    return this.copy(
        status = SyncStatus.IN_PROGRESS,
        lastAttemptAt = System.currentTimeMillis()
    )
}

/**
 * Extension to mark operation as completed.
 */
fun SyncQueueEntity.markCompleted(): SyncQueueEntity {
    return this.copy(status = SyncStatus.COMPLETED)
}

/**
 * Extension to mark operation as failed.
 */
fun SyncQueueEntity.markFailed(error: String): SyncQueueEntity {
    return this.copy(
        status = SyncStatus.FAILED,
        retryCount = this.retryCount + 1,
        lastError = error,
        lastAttemptAt = System.currentTimeMillis()
    )
}

/**
 * Extension to reset operation to pending (for retry).
 */
fun SyncQueueEntity.resetToPending(): SyncQueueEntity {
    return this.copy(status = SyncStatus.PENDING)
}
