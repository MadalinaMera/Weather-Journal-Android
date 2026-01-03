package com.example.weatherapp.ui.screens.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weatherapp.data.local.database.JournalDao
import com.example.weatherapp.data.local.database.entity.JournalEntity
import com.example.weatherapp.data.local.datastore.TokenManager
import com.example.weatherapp.util.NetworkMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class JournalUiState(
    val isLoading: Boolean = true,
    val entries: List<JournalEntity> = emptyList(),
    val error: String? = null,
    val isLoggedOut: Boolean = false
)

@HiltViewModel
class JournalViewModel @Inject constructor(
    private val journalDao: JournalDao,
    private val tokenManager: TokenManager,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(JournalUiState())
    val uiState: StateFlow<JournalUiState> = _uiState.asStateFlow()
    
    val isOnline: StateFlow<Boolean> = networkMonitor.isConnected
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )
    
    init {
        loadEntries()
    }
    
    private fun loadEntries() {
        viewModelScope.launch {
            journalDao.getAllEntries().collect { entries ->
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        entries = entries
                    )
                }
            }
        }
    }
    
    fun logout() {
        viewModelScope.launch {
            tokenManager.clearSession()
            _uiState.update { it.copy(isLoggedOut = true) }
        }
    }
    
    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            // Trigger sync if online
            if (networkMonitor.isCurrentlyConnected()) {
                // SyncWorker.triggerImmediateSync(context) - need context
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }
}
