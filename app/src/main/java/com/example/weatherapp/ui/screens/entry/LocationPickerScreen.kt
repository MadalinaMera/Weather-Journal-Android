package com.example.weatherapp.ui.screens.entry

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@Composable
fun LocationPickerScreen(
    onLocationSelected: (Double, Double) -> Unit,
    onBack: () -> Unit,
    initialLat: Double = 0.0,
    initialLong: Double = 0.0
) {
    // Start map at current location or default (0,0)
    val startPos = LatLng(if (initialLat == 0.0) 46.07 else initialLat, if (initialLong == 0.0) 23.5 else initialLong)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(startPos, 10f)
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    val target = cameraPositionState.position.target
                    onLocationSelected(target.latitude, target.longitude)
                },
                icon = { Icon(Icons.Default.Check, contentDescription = null) },
                text = { Text("Confirm Location") }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState
            )

            // Fixed Center Pin
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Center",
                modifier = Modifier.size(48.dp).align(Alignment.Center),
                tint = Color.Red
            )
        }
    }
}