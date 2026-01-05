package com.example.weatherapp.ui.screens.entry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weatherapp.data.local.database.JournalDao
import com.example.weatherapp.worker.SyncPayload
import com.example.weatherapp.worker.SyncWorker
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditEntryViewModel @Inject constructor(
    private val journalDao: JournalDao,
    private val gson: Gson,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {

    // State to hold the entry data
    private val _entryState = MutableStateFlow<EntryUiState?>(null)
    val entryState = _entryState.asStateFlow()

    // Helper class to hold form data
    data class EntryUiState(
        val id: String,
        val date: String,
        val temperature: String,
        val description: String,
        val latitude: Double,
        val longitude: Double
    )

    fun loadEntry(entryId: String) {
        viewModelScope.launch {
            val entry = journalDao.getEntryById(entryId)
            if (entry != null) {
                _entryState.value = EntryUiState(
                    id = entry.id,
                    date = entry.date,
                    temperature = entry.temperature.toString(),
                    description = entry.description,
                    latitude = entry.coords.latitude,
                    longitude = entry.coords.longitude
                )
            }
        }
    }

    // Updates the location in the state (called when returning from Map)
    fun updateLocation(lat: Double, long: Double) {
        _entryState.value = _entryState.value?.copy(latitude = lat, longitude = long)
    }

    fun updateEntry(
        newDate: String,
        newTemperature: Double,
        newDescription: String,
        onSuccess: () -> Unit
    ) {
        val currentState = _entryState.value ?: return

        viewModelScope.launch {
            val original = journalDao.getEntryById(currentState.id) ?: return@launch

            val updatedEntry = original.copy(
                date = newDate,
                temperature = newTemperature,
                description = newDescription,

                // For location, we use 'currentState' because updateLocation() updates it
                coords = com.example.weatherapp.data.local.database.entity.Coordinates(
                    currentState.latitude,
                    currentState.longitude
                ),
                isSynced = false
            )

            val payload = SyncPayload(
                date = updatedEntry.date,
                temperature = updatedEntry.temperature,
                description = updatedEntry.description,
                latitude = updatedEntry.coords.latitude,
                longitude = updatedEntry.coords.longitude
            )

            journalDao.updateEntryWithSync(updatedEntry, gson.toJson(payload))

            // Trigger Sync
            SyncWorker.triggerImmediateSync(context)

            onSuccess()
        }
    }
}