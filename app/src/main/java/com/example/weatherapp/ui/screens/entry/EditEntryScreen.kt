package com.example.weatherapp.ui.screens.entry

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
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEntryScreen(
    entryId: String,
    onNavigateBack: () -> Unit,
    onNavigateToMap: (Double, Double) -> Unit,
    viewModel: EditEntryViewModel = hiltViewModel()
) {
    val entryState by viewModel.entryState.collectAsState()

    // Load data when screen opens
    LaunchedEffect(entryId) {
        viewModel.loadEntry(entryId)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Edit Entry") }) }
    ) { padding ->
        if (entryState == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            // We force a non-null variable here for cleaner usage below
            val entry = entryState!!

            // 1. Calculate Initial Date in Millis (for the Picker)
            val initialDateMillis = remember(entry.date) {
                try {
                    Instant.parse(entry.date).toEpochMilli()
                } catch (e: Exception) {
                    System.currentTimeMillis()
                }
            }

            // 2. Initialize Picker State
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = initialDateMillis
            )

            var showDatePicker by remember { mutableStateOf(false) }

            // 3. Mutable state for text fields
            // We use 'remember' with keys so they reset if the entry changes
            var description by remember(entry) { mutableStateOf(entry.description) }
            var temperature by remember(entry) { mutableStateOf(entry.temperature) }

            // 4. Create Display String (updates when you pick a NEW date)
            val displayDate = remember(datePickerState.selectedDateMillis) {
                val millis = datePickerState.selectedDateMillis ?: initialDateMillis
                val instant = Instant.ofEpochMilli(millis)
                val zoneId = ZoneId.systemDefault()
                DateTimeFormatter.ofPattern("MMM dd, yyyy").format(instant.atZone(zoneId))
            }

            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // --- Date Field ---
                OutlinedTextField(
                    value = displayDate,
                    onValueChange = { },
                    label = { Text("Date") },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Edit Date")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
                )

                // --- Description ---
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
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

                // --- Location Card ---
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    onClick = {
                        onNavigateToMap(entry.latitude, entry.longitude)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.LocationOn, null)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Location", style = MaterialTheme.typography.labelMedium)
                            Text(
                                "${entry.latitude.toString().take(7)}, ${entry.longitude.toString().take(7)}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        Icon(Icons.Default.Map, null)
                    }
                }

                // --- Update Button ---
                Button(
                    onClick = {
                        // 1. Calculate the new Date String
                        // (Logic: User's picked date + Current time + Local Timezone)
                        val pickedMillis = datePickerState.selectedDateMillis ?: initialDateMillis
                        val pickedInstant = Instant.ofEpochMilli(pickedMillis)
                        val pickedDate = pickedInstant.atZone(ZoneOffset.UTC).toLocalDate()
                        val currentTime = LocalTime.now()
                        val combinedDateTime = pickedDate.atTime(currentTime).atZone(ZoneId.systemDefault())
                        val isoDate = DateTimeFormatter.ISO_INSTANT.format(combinedDateTime.toInstant())

                        // 2. Call the ViewModel with the CORRECT signature
                        viewModel.updateEntry(
                            newDate = isoDate,
                            newTemperature = temperature.toDoubleOrNull() ?: 0.0,
                            newDescription = description,
                            onSuccess = onNavigateBack
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Update Entry")
                }
            }

            // --- Date Picker Dialog ---
            if (showDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = { TextButton(onClick = { showDatePicker = false }) { Text("OK") } },
                    dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
                ) {
                    DatePicker(state = datePickerState)
                }
            }
        }
    }
}