package com.example.weatherapp.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enum representing network connection status.
 */
enum class NetworkStatus {
    AVAILABLE,
    UNAVAILABLE,
    LOSING,
    LOST
}

/**
 * Data class containing detailed network information.
 */
data class NetworkState(
    val isConnected: Boolean,
    val status: NetworkStatus,
    val isWifi: Boolean = false,
    val isCellular: Boolean = false,
    val isMetered: Boolean = false,
    val hasInternet: Boolean = false
) {
    companion object {
        val DISCONNECTED = NetworkState(
            isConnected = false,
            status = NetworkStatus.UNAVAILABLE
        )
        
        val CONNECTED_WIFI = NetworkState(
            isConnected = true,
            status = NetworkStatus.AVAILABLE,
            isWifi = true,
            hasInternet = true
        )
        
        val CONNECTED_CELLULAR = NetworkState(
            isConnected = true,
            status = NetworkStatus.AVAILABLE,
            isCellular = true,
            isMetered = true,
            hasInternet = true
        )
    }
}

/**
 * NetworkMonitor provides real-time network connectivity status.
 * 
 * This replaces the Capacitor Network plugin from the original:
 * ```typescript
 * import { Network } from '@capacitor/network';
 * 
 * const listener = Network.addListener('networkStatusChange', status => {
 *     setIsOnline(status.connected);
 *     if (status.connected) {
 *         flushQueue();
 *     }
 * });
 * ```
 * 
 * Features:
 * - Real-time network state monitoring via ConnectivityManager
 * - StateFlow for reactive UI updates
 * - Detailed network information (WiFi, cellular, metered)
 * - Automatic sync triggering on reconnection
 * 
 * Usage:
 * ```kotlin
 * // In ViewModel
 * val isOnline = networkMonitor.isConnected.collectAsState()
 * 
 * // Observe network changes
 * networkMonitor.networkState.collect { state ->
 *     if (state.isConnected) {
 *         triggerSync()
 *     }
 * }
 * ```
 */
@Singleton
class NetworkMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "NetworkMonitor"
    }
    
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) 
            as ConnectivityManager
    
    /**
     * Mutable state for internal updates.
     */
    private val _networkState = MutableStateFlow(getCurrentNetworkState())
    
    /**
     * StateFlow of current network state.
     * Observable from ViewModels and Composables.
     */
    val networkState: StateFlow<NetworkState> = _networkState.asStateFlow()
    
    /**
     * Simple boolean Flow for connection status.
     * Convenience accessor for basic connected/disconnected state.
     */
    val isConnected: Flow<Boolean> = callbackFlow {
        // Initial value
        trySend(isCurrentlyConnected())
        
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d(TAG, "Network available")
                trySend(true)
            }
            
            override fun onLost(network: Network) {
                Log.d(TAG, "Network lost")
                trySend(false)
            }
            
            override fun onUnavailable() {
                Log.d(TAG, "Network unavailable")
                trySend(false)
            }
        }
        
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        connectivityManager.registerNetworkCallback(request, callback)
        
        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()
    
    /**
     * Flow with detailed network state changes.
     * Provides full NetworkState information on each change.
     */
    val networkStateFlow: Flow<NetworkState> = callbackFlow {
        // Initial value
        trySend(getCurrentNetworkState())
        
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d(TAG, "Network available")
                val state = getNetworkState(network)
                _networkState.value = state
                trySend(state)
            }
            
            override fun onLost(network: Network) {
                Log.d(TAG, "Network lost")
                val state = NetworkState(
                    isConnected = false,
                    status = NetworkStatus.LOST
                )
                _networkState.value = state
                trySend(state)
            }
            
            override fun onLosing(network: Network, maxMsToLive: Int) {
                Log.d(TAG, "Network losing (maxMsToLive: $maxMsToLive)")
                val state = _networkState.value.copy(status = NetworkStatus.LOSING)
                _networkState.value = state
                trySend(state)
            }
            
            override fun onUnavailable() {
                Log.d(TAG, "Network unavailable")
                val state = NetworkState.DISCONNECTED
                _networkState.value = state
                trySend(state)
            }
            
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                Log.d(TAG, "Network capabilities changed")
                val state = getNetworkStateFromCapabilities(networkCapabilities)
                _networkState.value = state
                trySend(state)
            }
        }
        
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            .build()
        
        connectivityManager.registerNetworkCallback(request, callback)
        
        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()
    
    /**
     * Check if currently connected (synchronous).
     * Use for immediate checks, prefer Flow for reactive updates.
     */
    fun isCurrentlyConnected(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
    
    /**
     * Get current network state synchronously.
     */
    fun getCurrentNetworkState(): NetworkState {
        val network = connectivityManager.activeNetwork
            ?: return NetworkState.DISCONNECTED
        
        return getNetworkState(network)
    }
    
    /**
     * Get network state for a specific network.
     */
    private fun getNetworkState(network: Network): NetworkState {
        val capabilities = connectivityManager.getNetworkCapabilities(network)
            ?: return NetworkState.DISCONNECTED
        
        return getNetworkStateFromCapabilities(capabilities)
    }
    
    /**
     * Create NetworkState from NetworkCapabilities.
     */
    private fun getNetworkStateFromCapabilities(capabilities: NetworkCapabilities): NetworkState {
        val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val isValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        val isConnected = hasInternet && isValidated
        
        return NetworkState(
            isConnected = isConnected,
            status = if (isConnected) NetworkStatus.AVAILABLE else NetworkStatus.UNAVAILABLE,
            isWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI),
            isCellular = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR),
            isMetered = !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED),
            hasInternet = hasInternet
        )
    }
    
    /**
     * Check if connected to WiFi.
     */
    fun isOnWifi(): Boolean {
        return getCurrentNetworkState().isWifi
    }
    
    /**
     * Check if connected to cellular.
     */
    fun isOnCellular(): Boolean {
        return getCurrentNetworkState().isCellular
    }
    
    /**
     * Check if connection is metered (should limit data usage).
     */
    fun isConnectionMetered(): Boolean {
        return getCurrentNetworkState().isMetered
    }
}

/**
 * Extension function to get human-readable network type.
 */
fun NetworkState.getNetworkTypeString(): String {
    return when {
        !isConnected -> "Offline"
        isWifi -> "WiFi"
        isCellular -> "Mobile Data"
        else -> "Connected"
    }
}

/**
 * Extension function to check if sync should proceed.
 * May skip sync on metered connections to save data.
 */
fun NetworkState.shouldSync(allowOnMetered: Boolean = true): Boolean {
    return isConnected && (allowOnMetered || !isMetered)
}
