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
    onNavigateToMap: (Double, Double) -> Unit, // Pass current loc to map
    viewModel: EditEntryViewModel = hiltViewModel()
) {
    val entryState by viewModel.entryState.collectAsState()

    // Load data when screen opens
    LaunchedEffect(entryId) {
        viewModel.loadEntry(entryId)
    }

    // Mutable state for the form fields
    var description by remember { mutableStateOf("") }
    var temperature by remember { mutableStateOf("") }
    var dateString by remember { mutableStateOf("") }

    // Date Picker state
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    // When data loads from DB, fill the form
    LaunchedEffect(entryState) {
        entryState?.let {
            description = it.description
            temperature = it.temperature
            dateString = it.date
            // Set date picker to saved date
            try {
                val instant = Instant.parse(it.date) // Expects ISO format like 2023-01-01T12:00:00Z
                datePickerState.selectedDateMillis = instant.toEpochMilli()
            } catch (e: Exception) { /* ignore parse error */ }
        }
    }

    // Formatter for display
    val displayDate = remember(datePickerState.selectedDateMillis, dateString) {
        val millis = datePickerState.selectedDateMillis
        if (millis != null) {
            val instant = Instant.ofEpochMilli(millis)
            DateTimeFormatter.ISO_LOCAL_DATE.format(instant.atZone(ZoneId.systemDefault()))
        } else {
            // Fallback to parsing the string string if picker is empty
            try {
                val instant = Instant.parse(dateString)
                DateTimeFormatter.ISO_LOCAL_DATE.format(instant.atZone(ZoneId.systemDefault()))
            } catch (e: Exception) { dateString }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Edit Entry") }) }
    ) { padding ->
        if (entryState == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier.padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // --- Date ---
                OutlinedTextField(
                    value = displayDate,
                    onValueChange = { },
                    label = { Text("Date") },
                    readOnly = true,
                    trailingIcon = { Icon(Icons.Default.CalendarToday, null) },
                    modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }
                )

                // --- Description ---
                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        description = it
                        // Update VM state immediately so it persists on rotation
                        // (Simplified: In a real app, update via event)
                    },
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
                        // Open map with current coordinates
                        onNavigateToMap(entryState!!.latitude, entryState!!.longitude)
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
                                "${entryState!!.latitude.toString().take(7)}, ${entryState!!.longitude.toString().take(7)}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        Icon(Icons.Default.Map, null)
                    }
                }

                // --- Update Button ---
                Button(
                    onClick = {
                        val finalDate = if (datePickerState.selectedDateMillis != null) {
                            // A. User picked a NEW date.
                            // We must preserve the OLD time.

                            // 1. Get new date from picker
                            val pickedInstant = Instant.ofEpochMilli(datePickerState.selectedDateMillis!!)
                            val newDate = pickedInstant.atZone(ZoneOffset.UTC).toLocalDate()

                            // 2. Extract time from the ORIGINAL date string
                            val originalTime = try {
                                Instant.parse(dateString)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalTime()
                            } catch (e: Exception) {
                                LocalTime.now() // Fallback if parsing fails
                            }

                            // 3. Combine New Date + Old Time
                            val combined = newDate.atTime(originalTime).atZone(ZoneId.systemDefault())
                            DateTimeFormatter.ISO_INSTANT.format(combined.toInstant())

                        } else {
                            // B. User kept the OLD date. Use it as is.
                            dateString
                        }

                        viewModel.updateEntry(
                            newDate = finalDate,
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
        }

        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = { TextButton(onClick = { showDatePicker = false }) { Text("OK") } }
            ) { DatePicker(state = datePickerState) }
        }
    }
}