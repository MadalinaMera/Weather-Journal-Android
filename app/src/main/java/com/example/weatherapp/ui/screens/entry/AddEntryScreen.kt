package com.example.weatherapp.ui.screens.entry

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEntryScreen(
    onNavigateBack: () -> Unit,
    onNavigateToMap: () -> Unit,
    viewModel: AddEntryViewModel = hiltViewModel()
) {
    var description by remember { mutableStateOf("") }
    var temperature by remember { mutableStateOf("") }

    // Date Picker State
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    // Formatted date string for display
    val selectedDateMillis = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
    val formattedDate = remember(selectedDateMillis) {
        val instant = Instant.ofEpochMilli(selectedDateMillis)
        DateTimeFormatter.ISO_LOCAL_DATE.format(instant.atZone(ZoneId.systemDefault()))
    }

    // Location State
    val location by viewModel.currentLocation.collectAsState()
    val isLoadingLocation by viewModel.isLoadingLocation.collectAsState()

    // Permission Launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        ) {
            viewModel.fetchLocation()
        }
    }

    // Ask for permission immediately when screen opens
    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("New Entry") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- Date Selection ---
            OutlinedTextField(
                value = formattedDate,
                onValueChange = { },
                label = { Text("Date") },
                readOnly = true, // Prevent typing, force picking
                trailingIcon = {
                    Icon(Icons.Default.CalendarToday, contentDescription = "Select Date")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true } // Click anywhere to open
            )

            // --- Description ---
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("How is the weather?") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            // --- Temperature ---
            OutlinedTextField(
                value = temperature,
                onValueChange = { temperature = it },
                label = { Text("Temperature (°C)") },
                modifier = Modifier.fillMaxWidth()
            )

            // --- Location Info ---
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                onClick = onNavigateToMap // Click card to open map
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Location",
                            style = MaterialTheme.typography.labelMedium
                        )
                        if (location != null) {
                            Text(
                                text = "${location!!.latitude.toString().take(7)}, ${location!!.longitude.toString().take(7)}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        } else {
                            Text("Tap to select location", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    Icon(Icons.Default.Map, contentDescription = "Open Map")
                }
            }

            // --- Save Button ---
            Button(
                onClick = {
                    val instant = Instant.ofEpochMilli(selectedDateMillis)
                    val isoDate = DateTimeFormatter.ISO_INSTANT.format(instant)

                    viewModel.addEntry(
                        date = isoDate,
                        temperature = temperature.toDoubleOrNull() ?: 0.0,
                        description = description,
                        latitude = location?.latitude ?: 0.0,
                        longitude = location?.longitude ?: 0.0,
                        onSuccess = onNavigateBack
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = description.isNotBlank() // Disable if empty
            ) {
                Text("Save Entry")
            }
        }

        // --- Date Picker Dialog ---
        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Cancel")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}