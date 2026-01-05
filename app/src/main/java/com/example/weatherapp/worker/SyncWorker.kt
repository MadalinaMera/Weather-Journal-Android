package com.example.weatherapp.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.gson.Gson
import com.example.weatherapp.data.local.database.JournalDao
import com.example.weatherapp.data.local.database.entity.JournalEntity
import com.example.weatherapp.data.local.database.entity.SyncOperationType
import com.example.weatherapp.data.local.database.entity.SyncQueueEntity
import com.example.weatherapp.data.local.database.entity.SyncStatus
import com.example.weatherapp.data.local.datastore.TokenManager
import com.example.weatherapp.data.remote.api.WeatherJournalApi
import com.example.weatherapp.data.remote.dto.CreateEntryRequest
import com.example.weatherapp.util.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * SyncWorker handles background synchronization of journal entries.
 * 
 * This is a direct migration of the sync logic from useJournalSync.tsx:
 * ```typescript
 * const flushQueue = async () => {
 *     const queue: SyncItem[] = JSON.parse(queueString);
 *     for (const item of queue) {
 *         if (item.type === 'ADD') {
 *             await apiService.addEntry(item.data);
 *         } else if (item.type === 'UPDATE' && item.id) {
 *             await apiService.updateEntry(item.id, item.data);
 *         }
 *     }
 *     localStorage.removeItem(QUEUE_KEY);
 *     refresh();
 * };
 * ```
 * 
 * Features:
 * - Processes offline sync queue
 * - Handles ADD, UPDATE, DELETE operations
 * - Exponential backoff on failure
 * - Sends notifications on completion
 * - Runs periodically and on network restoration
 * 
 * Usage:
 * ```kotlin
 * // Schedule periodic sync
 * SyncWorker.schedulePeriodicSync(context)
 * 
 * // Trigger immediate sync
 * SyncWorker.triggerImmediateSync(context)
 * ```
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val journalDao: JournalDao,
    private val api: WeatherJournalApi,
    private val tokenManager: TokenManager,
    private val notificationHelper: NotificationHelper,
    private val gson: Gson
) : CoroutineWorker(context, workerParams) {
    
    companion object {
        private const val TAG = "SyncWorker"
        
        /**
         * Unique work names for WorkManager.
         */
        const val PERIODIC_SYNC_WORK_NAME = "periodic_sync_work"
        const val ONE_TIME_SYNC_WORK_NAME = "one_time_sync_work"
        
        /**
         * Output data keys.
         */
        const val KEY_SYNCED_COUNT = "synced_count"
        const val KEY_FAILED_COUNT = "failed_count"
        const val KEY_ERROR_MESSAGE = "error_message"
        
        /**
         * Input data keys.
         */
        const val KEY_FORCE_FULL_SYNC = "force_full_sync"
        
        /**
         * Schedule periodic background sync.
         * Runs every 15 minutes when network is available.
         * 
         * @param context Application context
         * @param intervalMinutes Sync interval (minimum 15 minutes)
         */
        fun schedulePeriodicSync(
            context: Context,
            intervalMinutes: Long = 15
        ) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()
            
            val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
                repeatInterval = intervalMinutes,
                repeatIntervalTimeUnit = TimeUnit.MINUTES,
                flexTimeInterval = 5,
                flexTimeIntervalUnit = TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    1,
                    TimeUnit.MINUTES
                )
                .addTag("sync")
                .build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_SYNC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
            
            Log.d(TAG, "Scheduled periodic sync every $intervalMinutes minutes")
        }
        
        /**
         * Trigger immediate one-time sync.
         * Use when connectivity is restored or user manually triggers sync.
         * 
         * @param context Application context
         * @param forceFullSync If true, fetches all entries from server
         */
        fun triggerImmediateSync(
            context: Context,
            forceFullSync: Boolean = false
        ) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            
            val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .setInputData(workDataOf(KEY_FORCE_FULL_SYNC to forceFullSync))
                .addTag("sync")
                .addTag("immediate")
                .build()
            
            WorkManager.getInstance(context).enqueueUniqueWork(
                ONE_TIME_SYNC_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                syncRequest
            )
            
            Log.d(TAG, "Triggered immediate sync (forceFullSync=$forceFullSync)")
        }
        
        /**
         * Cancel all sync work.
         */
        fun cancelAllSync(context: Context) {
            WorkManager.getInstance(context).cancelAllWorkByTag("sync")
            Log.d(TAG, "Cancelled all sync work")
        }
        
        /**
         * Get work info for sync status observation.
         */
        fun getSyncWorkInfo(context: Context) =
            WorkManager.getInstance(context)
                .getWorkInfosForUniqueWorkLiveData(ONE_TIME_SYNC_WORK_NAME)
    }
    
    /**
     * Main work execution.
     */
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting sync work...")
        
        // Check if user is authenticated
        if (!tokenManager.isAuthenticated()) {
            Log.w(TAG, "User not authenticated, skipping sync")
            return@withContext Result.success(
                workDataOf(KEY_ERROR_MESSAGE to "Not authenticated")
            )
        }
        
        var syncedCount = 0
        var failedCount = 0
        var lastError: String? = null
        
        try {
            // 1. Process pending sync queue (offline changes)
            val queueResult = processSyncQueue()
            syncedCount += queueResult.first
            failedCount += queueResult.second
            
            fetchServerEntries()

            
            // 3. Update last sync time
            tokenManager.saveLastSyncTime()
            
            // 4. Show notification
            if (syncedCount > 0) {
                notificationHelper.showSyncSuccessNotification(syncedCount)
            }
            
            Log.d(TAG, "Sync completed: synced=$syncedCount, failed=$failedCount")
            
            return@withContext if (failedCount > 0) {
                Result.retry()
            } else {
                Result.success(
                    workDataOf(
                        KEY_SYNCED_COUNT to syncedCount,
                        KEY_FAILED_COUNT to failedCount
                    )
                )
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed with exception", e)
            lastError = e.message
            
            // Show failure notification
            notificationHelper.showSyncFailedNotification(e.message ?: "Unknown error")
            
            return@withContext if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure(
                    workDataOf(
                        KEY_ERROR_MESSAGE to lastError,
                        KEY_FAILED_COUNT to failedCount
                    )
                )
            }
        }
    }
    
    /**
     * Process all pending operations in the sync queue.
     * 
     * Mirrors the original flushQueue logic:
     * ```typescript
     * for (const item of queue) {
     *     if (item.type === 'ADD') {
     *         await apiService.addEntry(item.data);
     *     } else if (item.type === 'UPDATE' && item.id) {
     *         await apiService.updateEntry(item.id, item.data);
     *     }
     * }
     * ```
     * 
     * @return Pair of (synced count, failed count)
     */
    private suspend fun processSyncQueue(): Pair<Int, Int> {
        var synced = 0
        var failed = 0
        
        val pendingOperations = journalDao.getPendingSyncOperations()
        Log.d(TAG, "Processing ${pendingOperations.size} pending operations")
        
        for (operation in pendingOperations) {
            // Skip operations that have exceeded retry limit
            if (!operation.shouldRetry() && operation.status == SyncStatus.FAILED) {
                Log.w(TAG, "Skipping operation ${operation.queueId} - exceeded retry limit")
                continue
            }
            
            try {
                // Mark as in progress
                journalDao.updateSyncStatus(operation.queueId, SyncStatus.IN_PROGRESS)
                
                when (operation.operationType) {
                    SyncOperationType.ADD -> {
                        processAddOperation(operation)
                    }
                    SyncOperationType.UPDATE -> {
                        processUpdateOperation(operation)
                    }
                    SyncOperationType.DELETE -> {
                        processDeleteOperation(operation)
                    }
                }
                
                // Mark as completed and remove from queue
                journalDao.deleteSyncOperation(operation.queueId)
                journalDao.markAsSynced(operation.entryId)
                synced++
                
                Log.d(TAG, "Successfully synced operation ${operation.queueId}")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync operation ${operation.queueId}", e)
                
                // Mark as failed
                journalDao.markSyncFailed(operation.queueId, e.message ?: "Unknown error")
                failed++
            }
        }
        
        // Clean up completed operations
        journalDao.deleteCompletedOperations()
        
        // Clean up old failed operations
        journalDao.deleteExceededRetries()
        
        return Pair(synced, failed)
    }
    
    /**
     * Process ADD operation - create new entry on server.
     */
    private suspend fun processAddOperation(operation: SyncQueueEntity) {
        val entry = journalDao.getEntryById(operation.entryId)
            ?: throw IllegalStateException("Entry not found: ${operation.entryId}")
        
        // Parse the stored payload or use entry data
        val request = CreateEntryRequest(
            id = entry.id,
            date = entry.date,
            temperature = entry.temperature,
            description = entry.description,
            photoUrl = entry.photoUrl,
            coords = CreateEntryRequest.CoordsDto(
                latitude = entry.coords.latitude,
                longitude = entry.coords.longitude
            )
        )
        
        // Call API
        val response = api.createEntry(request)
        
        if (!response.isSuccessful) {
            throw Exception("API error: ${response.code()} - ${response.message()}")
        }
        
        // Update local entry with server response if needed
        response.body()?.let { serverEntry ->
            // Server might have modified something, update local
            val updated = entry.copy(
                userId = serverEntry.userId,
                isSynced = true
            )
            journalDao.updateEntry(updated)
        }
    }
    
    /**
     * Process UPDATE operation - update existing entry on server.
     */
    private suspend fun processUpdateOperation(operation: SyncQueueEntity) {
        val entry = journalDao.getEntryById(operation.entryId)
            ?: throw IllegalStateException("Entry not found: ${operation.entryId}")
        
        val request = CreateEntryRequest(
            id = entry.id,
            date = entry.date,
            temperature = entry.temperature,
            description = entry.description,
            photoUrl = entry.photoUrl,
            coords = CreateEntryRequest.CoordsDto(
                latitude = entry.coords.latitude,
                longitude = entry.coords.longitude
            )
        )
        
        val response = api.updateEntry(operation.entryId, request)
        
        if (!response.isSuccessful) {
            throw Exception("API error: ${response.code()} - ${response.message()}")
        }
    }
    
    /**
     * Process DELETE operation - delete entry on server.
     */
    private suspend fun processDeleteOperation(operation: SyncQueueEntity) {
        // Note: The original API doesn't have a delete endpoint
        // This would need to be added to the server
        // For now, just mark as synced and hard delete locally
        
        journalDao.hardDeleteEntry(operation.entryId)
    }
    
    /**
     * Fetch all entries from server and merge with local.
     * Used for full sync or initial data load.
     */
    private suspend fun fetchServerEntries() {
        Log.d(TAG, "Fetching entries from server...")
        
        var page = 1
        val limit = 50
        var hasMore = true
        val allEntries = mutableListOf<JournalEntity>()
        
        while (hasMore) {
            val response = api.getEntries(page, limit)
            
            if (!response.isSuccessful) {
                throw Exception("Failed to fetch entries: ${response.code()}")
            }
            
            response.body()?.let { paginatedResponse ->
                val entities = paginatedResponse.entries.map { dto ->
                    JournalEntity.fromServer(
                        id = dto.id,
                        userId = dto.userId,
                        date = dto.date,
                        temperature = dto.temperature,
                        description = dto.description,
                        photoUrl = dto.photoUrl,
                        latitude = dto.coords?.latitude ?: 0.0,
                        longitude = dto.coords?.longitude ?: 0.0
                    )
                }
                allEntries.addAll(entities)
                hasMore = paginatedResponse.hasMore
                page++
            } ?: run {
                hasMore = false
            }
        }
        
        // Merge with local entries (preserve unsynced local changes)
        journalDao.mergeWithServerEntries(allEntries)
        
        Log.d(TAG, "Fetched and merged ${allEntries.size} entries from server")
    }
}

/**
 * Data class for sync queue payload serialization.
 */
data class SyncPayload(
    val date: String,
    val temperature: Double,
    val description: String,
    val photoUrl: String? = null,
    val latitude: Double,
    val longitude: Double
)
