package com.example.weatherapp.ui.screens.journal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.weatherapp.data.local.database.entity.JournalEntity
import com.example.weatherapp.ui.theme.JournalPink
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(
    onLogout: () -> Unit,
    onAddEntry: () -> Unit,
    viewModel: JournalViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    
    // Handle logout
    LaunchedEffect(uiState.isLoggedOut) {
        if (uiState.isLoggedOut) {
            onLogout()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("My Journal")
                        if (!isOnline) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Filled.CloudOff,
                                contentDescription = "Offline",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = JournalPink,
                    titleContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = { viewModel.logout() }) {
                        Icon(
                            imageVector = Icons.Filled.Logout,
                            contentDescription = "Logout",
                            tint = Color.White
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddEntry,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Entry")
            }
        }
    ) { paddingValues ->

        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = { viewModel.refresh() }, // <--- Calls your function!
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 2. MOVE YOUR EXISTING COLUMN/LAZYCOLUMN HERE
            // Note: Remove 'padding(paddingValues)' from the Column below
            // because we moved it to the PullToRefreshBox above.
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Offline banner
                if (!isOnline) {
                    // ... existing banner code ...
                }

                when {
                    // Remove the 'isLoading' check here so the list
                    // remains visible while refreshing
                    uiState.entries.isEmpty() && !uiState.isLoading -> {
                        EmptyJournalState()
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.entries) { entry ->
                                JournalEntryCard(entry = entry)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyJournalState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Cloud,
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "No Entries Yet",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        
        Text(
            text = "Tap the + button to add your first weather entry",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun JournalEntryCard(entry: JournalEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Thumbnail
            if (!entry.photoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = entry.photoUrl,
                    contentDescription = "Entry photo",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            
            // Content
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = formatDate(entry.date),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = formatTime(entry.date),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Temperature chip
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = "${entry.temperature.toInt()}°C",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = entry.description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Sync status indicator
                if (!entry.isSynced) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• Pending sync",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFffa726)
                    )
                }
            }
        }
    }
}

private fun formatDate(isoDate: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val date = inputFormat.parse(isoDate.substringBefore(".").substringBefore("Z"))
        outputFormat.format(date!!)
    } catch (e: Exception) {
        isoDate.substringBefore("T")
    }
}

private fun formatTime(isoDate: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val date = inputFormat.parse(isoDate.substringBefore(".").substringBefore("Z"))
        outputFormat.format(date!!)
    } catch (e: Exception) {
        ""
    }
}
