package com.example.weatherapp.ui.screens.forecast

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.weatherapp.ui.theme.PrimaryPurple
import com.example.weatherapp.ui.theme.SecondaryPurple
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.offset // For moving up/down
import androidx.compose.ui.graphics.ColorFilter
import com.example.weatherapp.util.WeatherIconMapper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForecastScreen(
    viewModel: ForecastViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState.error) {
        uiState.error?.let { errorMessage ->
            scope.launch {
                snackbarHostState.showSnackbar(errorMessage)
            }
        }
    }

    // Setup Location Client
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Function to fetch location
    @SuppressLint("MissingPermission")
    fun getLocation() {
        Log.d("ForecastScreen", "Requesting current location...")

        try {
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
            ).addOnSuccessListener { location ->
                if (location != null) {
                    Log.d("ForecastScreen", "Location found: ${location.latitude}, ${location.longitude}")
                    viewModel.loadWeatherByLocation(location.latitude, location.longitude)
                } else {
                    Log.e("ForecastScreen", "Location is null. Ensure GPS is on.")
                    Toast.makeText(context, "Could not get location. Ensure GPS is on.", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener { e ->
                Log.e("ForecastScreen", "Location error: ${e.message}")
                Toast.makeText(context, "Location error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } catch (e: SecurityException) {
            Log.e("ForecastScreen", "Security Exception: ${e.message}")
        }
    }

    // Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (isGranted) {
            getLocation()
        } else {
            Toast.makeText(context, "Location permission needed", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Current Forecast") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryPurple,
                    titleContentColor = Color.White
                ),
                actions = {
                    IconButton(
                        onClick = { viewModel.refresh() },
                        enabled = !uiState.isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refresh",
                            tint = Color.White
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (uiState.weather != null) {
                WeatherContent(
                    uiState = uiState,
                    onLocationClick = { viewModel.refresh() }
                )
            }
            // Only show the "Empty Error Box" if we truly have NO data and NO weather
            else if (uiState.error != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = uiState.error!!, color = Color.Red)
                }
            }

            // Loading Indicator (Overlay)
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

private fun checkAndRequestLocation(
    context: Context,
    launcher: androidx.activity.result.ActivityResultLauncher<Array<String>>,
    onGranted: () -> Unit
) {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    ) {
        onGranted()
    } else {
        launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }
}

@Composable
private fun WeatherContent(
    uiState: ForecastUiState,
    onLocationClick: () -> Unit
) {
    val weather = uiState.weather!!
    val background = getWeatherGradient(weather.conditionMain)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(brush = background)
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Location
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.9f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${weather.cityName}, ${weather.country}",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 1. Create the Infinite Transition
                    val infiniteTransition = rememberInfiniteTransition(label = "weather_icon_float")

                    // 2. Define the animation (Move Y axis by 10dp up and down)
                    val offsetY by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = -20f, // Float up
                        animationSpec = infiniteRepeatable(
                            animation = tween(2000, easing = LinearOutSlowInEasing), // 2 seconds (slower)
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "offset"
                    )

                    // Weather icon and temperature
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center, // UPDATED: Changed from SpaceEvenly to Center
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val iconResId = WeatherIconMapper.getIconResource(weather.icon)

                        // 2. Load the local image instantly
                        Image(
                            painter = painterResource(id = iconResId),
                            contentDescription = weather.description,
                            colorFilter = ColorFilter.tint(Color.White),
                            modifier = Modifier
                                .size(80.dp)
                                .offset(y = offsetY.dp) // Keep your existing animation!
                        )

                        // UPDATED: Added spacer so they don't touch
                        Spacer(modifier = Modifier.width(16.dp))

                        Row(verticalAlignment = Alignment.Top) {
                            Text(
                                text = "${weather.temperature.toInt()}",
                                fontSize = 72.sp,
                                fontWeight = FontWeight.Light,
                                color = Color.White
                            )
                            Text(
                                text = "°C",
                                fontSize = 24.sp,
                                color = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }

                    Text(
                        text = weather.description.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        WeatherDetailItem(
                            icon = Icons.Filled.Thermostat,
                            label = "Feels Like",
                            value = "${weather.feelsLike.toInt()}°C"
                        )
                        WeatherDetailItem(
                            icon = Icons.Filled.WaterDrop,
                            label = "Humidity",
                            value = "${weather.humidity}%"
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = onLocationClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.LocationOn, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Use Device Location")
        }
    }
}

@Composable
private fun MockWeatherContent(onLocationClick: () -> Unit) {
    // Same layout update applied to Mock content for consistency
    val background = Brush.linearGradient(listOf(PrimaryPurple, SecondaryPurple))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(brush = background)
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.9f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Sample Location",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(text = "🌤️", fontSize = 96.sp)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "22",
                            fontSize = 72.sp,
                            fontWeight = FontWeight.Light,
                            color = Color.White
                        )
                        Text(
                            text = "°C",
                            fontSize = 24.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    Text(
                        text = "Partly Cloudy",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        WeatherDetailItem(icon = Icons.Filled.Thermostat, label = "Feels Like", value = "20°C")
                        WeatherDetailItem(icon = Icons.Filled.WaterDrop, label = "Humidity", value = "65%")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onLocationClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.LocationOn, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Use Device Location")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Add your OpenWeatherMap API key to get real data",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun WeatherDetailItem(
    icon: ImageVector,
    label: String,
    value: String
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

private fun getWeatherGradient(condition: String): Brush {
    return when (condition) {
        "Clear" -> Brush.linearGradient(listOf(Color(0xFF667eea), Color(0xFF764ba2)))
        "Clouds" -> Brush.linearGradient(listOf(Color(0xFF8e9eab), Color(0xFFeef2f3)))
        "Rain", "Drizzle" -> Brush.linearGradient(listOf(Color(0xFF4b79a1), Color(0xFF283e51)))
        "Snow" -> Brush.linearGradient(listOf(Color(0xFFe0eafc), Color(0xFFcfdef3)))
        "Thunderstorm" -> Brush.linearGradient(listOf(Color(0xFF2c3e50), Color(0xFF34495e)))
        else -> Brush.linearGradient(listOf(PrimaryPurple, SecondaryPurple))
    }
}