package com.example.weatherapp.data.remote

import android.util.Log
import com.example.weatherapp.data.local.database.JournalDao
import com.example.weatherapp.data.local.database.entity.JournalEntity
import com.example.weatherapp.data.local.datastore.TokenManager
import com.example.weatherapp.util.Constants
import com.google.gson.Gson
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealtimeManager @Inject constructor(
    private val journalDao: JournalDao,
    private val tokenManager: TokenManager,
    private val gson: Gson
) {
    private var socket: Socket? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun connect() {
        if (socket?.connected() == true) return

        scope.launch {
            val token = tokenManager.getToken() ?: return@launch

            try {
                // 1. Configure Options with Auth Token
                val options = IO.Options().apply {
                    auth = mapOf("token" to token)
                }

                // 2. Initialize Socket (Use 10.0.2.2 for Emulator, or your local IP for device)
                // Note: Constants.BASE_URL usually ends with /, remove it for socket root
                val url = Constants.Api.BASE_URL.removeSuffix("/")
                socket = IO.socket(url, options)

                // 3. Listen for "New Entry"
                socket?.on("entry_added") { args ->
                    handleEntryUpdate(args[0], isNew = true)
                }

                // 4. Listen for "Update Entry"
                socket?.on("entry_updated") { args ->
                    handleEntryUpdate(args[0], isNew = false)
                }

                socket?.on(Socket.EVENT_CONNECT) {
                    Log.d("Realtime", "Connected to Socket.IO")
                }

                socket?.connect()

            } catch (e: Exception) {
                Log.e("Realtime", "Connection failed", e)
            }
        }
    }

    fun disconnect() {
        socket?.disconnect()
        socket?.off() // Remove listeners
    }

    private fun handleEntryUpdate(data: Any, isNew: Boolean) {
        try {
            val jsonObject = data as JSONObject
            // Convert JSON to Entity directly
            // Note: We manually map because server JSON might be slightly different than Room Entity
            val entity = JournalEntity(
                id = jsonObject.getString("id"),
                userId = jsonObject.getInt("user_id"),
                date = jsonObject.getString("date"),
                temperature = jsonObject.getDouble("temperature"),
                description = jsonObject.getString("description"),

                // Explicitly check for null. If it is null, use Kotlin null.
                // Otherwise read the string.
                photoUrl = if (jsonObject.isNull("photo_url")) null else jsonObject.getString("photo_url"),

                coords = com.example.weatherapp.data.local.database.entity.Coordinates(
                    jsonObject.getJSONObject("coords").getDouble("latitude"),
                    jsonObject.getJSONObject("coords").getDouble("longitude")
                ),
                isSynced = true,
                isDeleted = false
            )

            // Update Database in Background
            scope.launch {
                if (isNew) {
                    journalDao.insertEntry(entity)
                } else {
                    journalDao.updateEntry(entity)
                }
                Log.d("Realtime", "Database updated via Socket: ${entity.description}")
            }
        } catch (e: Exception) {
            Log.e("Realtime", "Failed to parse socket message", e)
        }
    }
}