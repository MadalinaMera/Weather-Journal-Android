package com.example.weatherapp.ui.screens.entry

import android.annotation.SuppressLint
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weatherapp.data.local.database.JournalDao
import com.example.weatherapp.data.local.database.entity.JournalEntity
import com.example.weatherapp.worker.SyncPayload
import com.example.weatherapp.worker.SyncWorker
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@HiltViewModel
class AddEntryViewModel @Inject constructor(
    private val journalDao: JournalDao,
    private val gson: Gson,
    private val locationClient: FusedLocationProviderClient,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation = _currentLocation.asStateFlow()

    private val _isLoadingLocation = MutableStateFlow(false)
    val isLoadingLocation = _isLoadingLocation.asStateFlow()

    fun addEntry(
        date: String,
        temperature: Double,
        description: String,
        latitude: Double,
        longitude: Double,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val id = UUID.randomUUID().toString()

            val entry = JournalEntity.createNew(
                id = id,
                date = date,
                temperature = temperature,
                description = description,
                latitude = latitude,
                longitude = longitude
            )

            val payload = SyncPayload(
                date = date,
                temperature = temperature,
                description = description,
                latitude = latitude,
                longitude = longitude
            )

            journalDao.insertEntryWithSync(entry, gson.toJson(payload))
            SyncWorker.triggerImmediateSync(context)
            onSuccess()
        }
    }

    fun updateLocation(latitude: Double, longitude: Double) {
        _currentLocation.value = android.location.Location("manual").apply {
            this.latitude = latitude
            this.longitude = longitude
        }
    }

    @SuppressLint("MissingPermission") // Checked in UI
    fun fetchLocation() {
        viewModelScope.launch {
            _isLoadingLocation.value = true
            try {
                // Try to get the last known location first (fast)
                var location = locationClient.lastLocation.await()

                // If null, try to get a fresh one (slower but accurate)
                if (location == null) {
                    location = locationClient.getCurrentLocation(
                        Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                        null
                    ).await()
                }

                _currentLocation.value = location
            } catch (e: Exception) {
                // Handle error (e.g., GPS off)
            } finally {
                _isLoadingLocation.value = false
            }
        }
    }

    // Helper to turn Google Tasks into Coroutines
    private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T? {
        return suspendCancellableCoroutine { continuation ->
            addOnSuccessListener { result ->
                continuation.resume(result)
            }
            addOnFailureListener { exception ->
                continuation.resumeWithException(exception)
            }
            addOnCanceledListener {
                continuation.cancel()
            }
        }
    }
}