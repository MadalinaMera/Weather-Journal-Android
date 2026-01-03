package com.example.weatherapp.data.local.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.weatherapp.data.local.database.entity.JournalEntity
import com.example.weatherapp.data.local.database.entity.SyncQueueEntity
import com.example.weatherapp.data.local.database.entity.SyncStatus
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Journal entries and Sync queue operations.
 * 
 * Provides all database operations needed for:
 * - CRUD operations on journal entries
 * - Offline sync queue management
 * - Pagination support
 * 
 * Uses Kotlin Coroutines and Flow for reactive data access.
 */
@Dao
interface JournalDao {
    
    // ==================== Journal Entry Operations ====================
    
    /**
     * Get all non-deleted journal entries as a Flow.
     * Automatically updates when data changes.
     * Ordered by date descending (newest first).
     */
    @Query("SELECT * FROM journal_entries WHERE is_deleted = 0 ORDER BY date DESC")
    fun getAllEntries(): Flow<List<JournalEntity>>
    
    /**
     * Get entries with pagination support.
     * Mirrors the original API: GET /entries?page=1&limit=10
     * 
     * @param limit Number of entries per page
     * @param offset Starting position (page - 1) * limit
     */
    @Query("""
        SELECT * FROM journal_entries 
        WHERE is_deleted = 0 
        ORDER BY date DESC 
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getEntriesPaginated(limit: Int, offset: Int): List<JournalEntity>
    
    /**
     * Get entries as a Flow with pagination.
     * Reactive version for Compose.
     */
    @Query("""
        SELECT * FROM journal_entries 
        WHERE is_deleted = 0 
        ORDER BY date DESC 
        LIMIT :limit OFFSET :offset
    """)
    fun getEntriesPaginatedFlow(limit: Int, offset: Int): Flow<List<JournalEntity>>
    
    /**
     * Get a single entry by ID.
     */
    @Query("SELECT * FROM journal_entries WHERE id = :id AND is_deleted = 0")
    suspend fun getEntryById(id: String): JournalEntity?
    
    /**
     * Get a single entry by ID as Flow.
     */
    @Query("SELECT * FROM journal_entries WHERE id = :id AND is_deleted = 0")
    fun getEntryByIdFlow(id: String): Flow<JournalEntity?>
    
    /**
     * Get total count of non-deleted entries.
     * Used for pagination calculations.
     */
    @Query("SELECT COUNT(*) FROM journal_entries WHERE is_deleted = 0")
    suspend fun getTotalCount(): Int
    
    /**
     * Get total count as Flow for reactive UI updates.
     */
    @Query("SELECT COUNT(*) FROM journal_entries WHERE is_deleted = 0")
    fun getTotalCountFlow(): Flow<Int>
    
    /**
     * Check if more entries exist after the given offset.
     */
    @Query("""
        SELECT COUNT(*) > :offset + :limit 
        FROM journal_entries 
        WHERE is_deleted = 0
    """)
    suspend fun hasMoreEntries(offset: Int, limit: Int): Boolean
    
    /**
     * Insert a new entry. Replaces if conflict (same ID).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: JournalEntity): Long
    
    /**
     * Insert multiple entries at once.
     * Used for bulk sync from server.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<JournalEntity>)
    
    /**
     * Update an existing entry.
     */
    @Update
    suspend fun updateEntry(entry: JournalEntity): Int
    
    /**
     * Soft delete an entry (mark as deleted).
     */
    @Query("UPDATE journal_entries SET is_deleted = 1, is_synced = 0 WHERE id = :id")
    suspend fun softDeleteEntry(id: String): Int
    
    /**
     * Hard delete an entry (remove from database).
     * Use after successful server deletion.
     */
    @Query("DELETE FROM journal_entries WHERE id = :id")
    suspend fun hardDeleteEntry(id: String): Int
    
    /**
     * Delete all entries (for logout/cache clear).
     */
    @Query("DELETE FROM journal_entries")
    suspend fun deleteAllEntries()
    
    // ==================== Sync Operations ====================
    
    /**
     * Get all entries that need to be synced (not synced and not deleted).
     */
    @Query("SELECT * FROM journal_entries WHERE is_synced = 0 AND is_deleted = 0")
    suspend fun getUnsyncedEntries(): List<JournalEntity>
    
    /**
     * Get count of unsynced entries.
     */
    @Query("SELECT COUNT(*) FROM journal_entries WHERE is_synced = 0 AND is_deleted = 0")
    suspend fun getUnsyncedCount(): Int
    
    /**
     * Get count of unsynced entries as Flow.
     */
    @Query("SELECT COUNT(*) FROM journal_entries WHERE is_synced = 0 AND is_deleted = 0")
    fun getUnsyncedCountFlow(): Flow<Int>
    
    /**
     * Get entries marked for deletion (need to sync deletion to server).
     */
    @Query("SELECT * FROM journal_entries WHERE is_deleted = 1")
    suspend fun getDeletedEntries(): List<JournalEntity>
    
    /**
     * Mark an entry as synced after successful server upload.
     */
    @Query("UPDATE journal_entries SET is_synced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String): Int
    
    /**
     * Mark multiple entries as synced.
     */
    @Query("UPDATE journal_entries SET is_synced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<String>): Int
    
    /**
     * Clean up entries that have been deleted and synced.
     */
    @Query("DELETE FROM journal_entries WHERE is_deleted = 1 AND is_synced = 1")
    suspend fun cleanupDeletedAndSynced(): Int
    
    // ==================== Sync Queue Operations ====================
    
    /**
     * Get all pending sync operations ordered by creation time.
     */
    @Query("""
        SELECT * FROM sync_queue 
        WHERE status IN ('PENDING', 'FAILED') 
        ORDER BY created_at ASC
    """)
    suspend fun getPendingSyncOperations(): List<SyncQueueEntity>
    
    /**
     * Get pending sync operations as Flow.
     */
    @Query("""
        SELECT * FROM sync_queue 
        WHERE status IN ('PENDING', 'FAILED') 
        ORDER BY created_at ASC
    """)
    fun getPendingSyncOperationsFlow(): Flow<List<SyncQueueEntity>>
    
    /**
     * Get count of pending sync operations.
     */
    @Query("SELECT COUNT(*) FROM sync_queue WHERE status IN ('PENDING', 'FAILED')")
    suspend fun getPendingSyncCount(): Int
    
    /**
     * Get count of pending sync operations as Flow.
     */
    @Query("SELECT COUNT(*) FROM sync_queue WHERE status IN ('PENDING', 'FAILED')")
    fun getPendingSyncCountFlow(): Flow<Int>
    
    /**
     * Get a specific sync operation by queue ID.
     */
    @Query("SELECT * FROM sync_queue WHERE queue_id = :queueId")
    suspend fun getSyncOperationById(queueId: Long): SyncQueueEntity?
    
    /**
     * Get sync operations for a specific entry.
     */
    @Query("SELECT * FROM sync_queue WHERE entry_id = :entryId ORDER BY created_at DESC")
    suspend fun getSyncOperationsForEntry(entryId: String): List<SyncQueueEntity>
    
    /**
     * Insert a new sync operation.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncOperation(operation: SyncQueueEntity): Long
    
    /**
     * Update a sync operation (e.g., status change).
     */
    @Update
    suspend fun updateSyncOperation(operation: SyncQueueEntity): Int
    
    /**
     * Delete a sync operation after successful processing.
     */
    @Query("DELETE FROM sync_queue WHERE queue_id = :queueId")
    suspend fun deleteSyncOperation(queueId: Long): Int
    
    /**
     * Delete all completed sync operations.
     */
    @Query("DELETE FROM sync_queue WHERE status = 'COMPLETED'")
    suspend fun deleteCompletedOperations(): Int
    
    /**
     * Delete all sync operations for an entry.
     * Used when entry is deleted or sync is cancelled.
     */
    @Query("DELETE FROM sync_queue WHERE entry_id = :entryId")
    suspend fun deleteSyncOperationsForEntry(entryId: String): Int
    
    /**
     * Clear entire sync queue.
     */
    @Query("DELETE FROM sync_queue")
    suspend fun clearSyncQueue()
    
    /**
     * Update sync operation status.
     */
    @Query("""
        UPDATE sync_queue 
        SET status = :status, last_attempt_at = :timestamp 
        WHERE queue_id = :queueId
    """)
    suspend fun updateSyncStatus(
        queueId: Long, 
        status: SyncStatus, 
        timestamp: Long = System.currentTimeMillis()
    ): Int
    
    /**
     * Mark sync operation as failed with error.
     */
    @Query("""
        UPDATE sync_queue 
        SET status = 'FAILED', 
            retry_count = retry_count + 1, 
            last_error = :error,
            last_attempt_at = :timestamp 
        WHERE queue_id = :queueId
    """)
    suspend fun markSyncFailed(
        queueId: Long, 
        error: String, 
        timestamp: Long = System.currentTimeMillis()
    ): Int
    
    /**
     * Delete operations that have exceeded max retries.
     */
    @Query("DELETE FROM sync_queue WHERE retry_count >= :maxRetries")
    suspend fun deleteExceededRetries(maxRetries: Int = SyncQueueEntity.MAX_RETRY_COUNT): Int
    
    // ==================== Transaction Operations ====================
    
    /**
     * Insert entry and add to sync queue atomically.
     */
    @Transaction
    suspend fun insertEntryWithSync(entry: JournalEntity, syncPayload: String) {
        insertEntry(entry)
        insertSyncOperation(
            SyncQueueEntity.createAddOperation(entry.id, syncPayload)
        )
    }
    
    /**
     * Update entry and add to sync queue atomically.
     */
    @Transaction
    suspend fun updateEntryWithSync(entry: JournalEntity, syncPayload: String) {
        updateEntry(entry)
        // Remove any existing pending operations for this entry
        deleteSyncOperationsForEntry(entry.id)
        insertSyncOperation(
            SyncQueueEntity.createUpdateOperation(entry.id, syncPayload)
        )
    }
    
    /**
     * Soft delete entry and add to sync queue atomically.
     */
    @Transaction
    suspend fun deleteEntryWithSync(entryId: String) {
        softDeleteEntry(entryId)
        deleteSyncOperationsForEntry(entryId)
        insertSyncOperation(
            SyncQueueEntity.createDeleteOperation(entryId)
        )
    }
    
    /**
     * Replace all entries (used during full sync from server).
     */
    @Transaction
    suspend fun replaceAllEntries(entries: List<JournalEntity>) {
        deleteAllEntries()
        insertEntries(entries)
    }
    
    /**
     * Merge server entries with local (preserving unsynced local changes).
     */
    @Transaction
    suspend fun mergeWithServerEntries(serverEntries: List<JournalEntity>) {
        // Get IDs of entries with pending local changes
        val unsyncedIds = getUnsyncedEntries().map { it.id }.toSet()
        
        // Only insert/update entries that don't have pending local changes
        val entriesToInsert = serverEntries.filter { it.id !in unsyncedIds }
        insertEntries(entriesToInsert)
    }
}
