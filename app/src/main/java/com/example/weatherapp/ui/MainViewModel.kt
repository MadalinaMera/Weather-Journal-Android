package com.example.weatherapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weatherapp.data.local.datastore.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Main ViewModel for app-level state like authentication status.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    tokenManager: TokenManager
) : ViewModel() {
    
    /**
     * Observable authentication state.
     */
    val isLoggedIn: StateFlow<Boolean> = tokenManager.isLoggedIn
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )
}
