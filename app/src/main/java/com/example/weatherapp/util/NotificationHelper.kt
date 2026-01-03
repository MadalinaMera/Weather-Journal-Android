package com.example.weatherapp.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * NotificationHelper manages system notifications for the Weather Journal app.
 * 
 * Handles:
 * - Sync success/failure notifications
 * - Offline mode alerts
 * - General app notifications
 * 
 * Required Permission (Android 13+):
 * <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
 * 
 * Usage:
 * ```kotlin
 * notificationHelper.showSyncSuccessNotification(5)
 * notificationHelper.showSyncFailedNotification("Network error")
 * ```
 */
@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        /**
         * Notification channel IDs.
         */
        const val CHANNEL_ID_SYNC = "weather_journal_sync"
        const val CHANNEL_ID_GENERAL = "weather_journal_general"
        const val CHANNEL_ID_OFFLINE = "weather_journal_offline"
        
        /**
         * Notification IDs.
         */
        const val NOTIFICATION_ID_SYNC_PROGRESS = 1001
        const val NOTIFICATION_ID_SYNC_SUCCESS = 1002
        const val NOTIFICATION_ID_SYNC_FAILED = 1003
        const val NOTIFICATION_ID_OFFLINE = 1004
        const val NOTIFICATION_ID_GENERAL = 1005
    }
    
    private val notificationManager = NotificationManagerCompat.from(context)
    
    init {
        createNotificationChannels()
    }
    
    /**
     * Create notification channels (required for Android 8.0+).
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Sync channel
            val syncChannel = NotificationChannel(
                CHANNEL_ID_SYNC,
                "Sync Notifications",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for background sync status"
                enableVibration(false)
                setShowBadge(false)
            }
            
            // General channel
            val generalChannel = NotificationChannel(
                CHANNEL_ID_GENERAL,
                "General Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "General app notifications"
            }
            
            // Offline channel
            val offlineChannel = NotificationChannel(
                CHANNEL_ID_OFFLINE,
                "Offline Mode",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Offline mode status notifications"
                enableVibration(false)
            }
            
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannels(
                listOf(syncChannel, generalChannel, offlineChannel)
            )
        }
    }
    
    /**
     * Check if notification permission is granted (Android 13+).
     */
    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
    
    /**
     * Show sync in progress notification.
     */
    fun showSyncProgressNotification(itemCount: Int = 0) {
        if (!hasNotificationPermission()) return
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_SYNC)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setContentTitle("Syncing...")
            .setContentText(
                if (itemCount > 0) "Syncing $itemCount items"
                else "Synchronizing your journal"
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setProgress(0, 0, true)
            .setAutoCancel(false)
            .build()
        
        try {
            notificationManager.notify(NOTIFICATION_ID_SYNC_PROGRESS, notification)
        } catch (e: SecurityException) {
            // Permission not granted
        }
    }
    
    /**
     * Show sync success notification.
     * 
     * Called from SyncWorker when sync completes successfully.
     * 
     * @param syncedCount Number of entries synced
     */
    fun showSyncSuccessNotification(syncedCount: Int) {
        if (!hasNotificationPermission()) return
        
        // Cancel progress notification
        notificationManager.cancel(NOTIFICATION_ID_SYNC_PROGRESS)
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_SYNC)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Sync Complete")
            .setContentText(
                when (syncedCount) {
                    0 -> "Everything is up to date"
                    1 -> "1 entry synced successfully"
                    else -> "$syncedCount entries synced successfully"
                }
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setTimeoutAfter(5000) // Auto-dismiss after 5 seconds
            .build()
        
        try {
            notificationManager.notify(NOTIFICATION_ID_SYNC_SUCCESS, notification)
        } catch (e: SecurityException) {
            // Permission not granted
        }
    }
    
    /**
     * Show sync failed notification.
     * 
     * Called from SyncWorker when sync fails.
     * 
     * @param errorMessage Error description
     */
    fun showSyncFailedNotification(errorMessage: String) {
        if (!hasNotificationPermission()) return
        
        // Cancel progress notification
        notificationManager.cancel(NOTIFICATION_ID_SYNC_PROGRESS)
        
        // Create retry intent (optional - opens app)
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = intent?.let {
            PendingIntent.getActivity(
                context,
                0,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_SYNC)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Sync Failed")
            .setContentText(errorMessage)
            .setStyle(NotificationCompat.BigTextStyle().bigText(errorMessage))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_popup_sync,
                "Retry",
                pendingIntent
            )
            .build()
        
        try {
            notificationManager.notify(NOTIFICATION_ID_SYNC_FAILED, notification)
        } catch (e: SecurityException) {
            // Permission not granted
        }
    }
    
    /**
     * Show offline mode notification.
     * Persistent notification while device is offline.
     */
    fun showOfflineModeNotification() {
        if (!hasNotificationPermission()) return
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_OFFLINE)
            .setSmallIcon(android.R.drawable.ic_menu_close_clear_cancel)
            .setContentTitle("Offline Mode")
            .setContentText("Your changes will sync when connection is restored")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()
        
        try {
            notificationManager.notify(NOTIFICATION_ID_OFFLINE, notification)
        } catch (e: SecurityException) {
            // Permission not granted
        }
    }
    
    /**
     * Hide offline mode notification.
     * Call when device comes back online.
     */
    fun hideOfflineModeNotification() {
        notificationManager.cancel(NOTIFICATION_ID_OFFLINE)
    }
    
    /**
     * Show general notification.
     * 
     * @param title Notification title
     * @param message Notification message
     */
    fun showNotification(title: String, message: String) {
        if (!hasNotificationPermission()) return
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_GENERAL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        
        try {
            notificationManager.notify(NOTIFICATION_ID_GENERAL, notification)
        } catch (e: SecurityException) {
            // Permission not granted
        }
    }
    
    /**
     * Show notification for pending sync items.
     * 
     * @param pendingCount Number of items waiting to sync
     */
    fun showPendingSyncNotification(pendingCount: Int) {
        if (!hasNotificationPermission() || pendingCount == 0) return
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_SYNC)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setContentTitle("Pending Sync")
            .setContentText(
                when (pendingCount) {
                    1 -> "1 entry waiting to sync"
                    else -> "$pendingCount entries waiting to sync"
                }
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()
        
        try {
            notificationManager.notify(NOTIFICATION_ID_SYNC_PROGRESS, notification)
        } catch (e: SecurityException) {
            // Permission not granted
        }
    }
    
    /**
     * Cancel all notifications.
     */
    fun cancelAllNotifications() {
        notificationManager.cancelAll()
    }
    
    /**
     * Cancel specific notification by ID.
     */
    fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }
}
