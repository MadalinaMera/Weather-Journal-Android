package com.example.weatherapp.ui.screens.entry

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.weatherapp.util.Constants
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch

@Composable
fun LocationPickerScreen(
    onLocationSelected: (Double, Double) -> Unit,
    onBack: () -> Unit,
    initialLat: Double = 0.0,
    initialLong: Double = 0.0
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 1. Setup Camera State
    // Default to a fallback location (e.g., Romania) if GPS fails,
    // but we will update this immediately if we find the user.
    val defaultLocation = LatLng(Constants.Location.DEFAULT_LAT, Constants.Location.DEFAULT_LONG)
    val startPos = if (initialLat != 0.0) LatLng(initialLat, initialLong) else defaultLocation

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(startPos, 10f)
    }

    // State to track if we have permission to show the "Blue Dot"
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // 2. Setup Location Client
    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    // 3. Function to Fetch & Move Camera
    @SuppressLint("MissingPermission")
    fun moveToCurrentLocation() {
        if (!hasLocationPermission) return

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                val userLatLng = LatLng(it.latitude, it.longitude)
                scope.launch {
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngZoom(userLatLng, 15f),
                        1000 // Animation duration in ms
                    )
                }
            }
        }
    }

    // 4. Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermission = granted
        if (granted) {
            moveToCurrentLocation()
        }
    }

    // 5. Trigger permission request on start
    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else if (initialLat == 0.0) {
            // Only move to current location if we didn't pass a specific one
            moveToCurrentLocation()
        }
    }

    Scaffold(
        floatingActionButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.End
            ) {
                // Button to re-center on my location
                if (hasLocationPermission) {
                    FloatingActionButton(
                        onClick = { moveToCurrentLocation() },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = "My Location")
                    }
                }

                // Confirm Button
                ExtendedFloatingActionButton(
                    onClick = {
                        val target = cameraPositionState.position.target
                        onLocationSelected(target.latitude, target.longitude)
                    },
                    icon = { Icon(Icons.Default.Check, contentDescription = null) },
                    text = { Text("Confirm Location") }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                // Enable the "Blue Dot" layer
                properties = MapProperties(
                    isMyLocationEnabled = hasLocationPermission
                ),
                uiSettings = MapUiSettings(
                    myLocationButtonEnabled = false, // We made our own custom FAB above
                    zoomControlsEnabled = false
                )
            )

            // Fixed Center Pin (Red)
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Center",
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.Center)
                    // Offset the icon so the tip is exactly at the center
                    .offset(y = (-24).dp),
                tint = Color.Red
            )
        }
    }
}