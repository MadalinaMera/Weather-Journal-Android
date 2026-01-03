package com.example.weatherapp.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Login
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.weatherapp.ui.screens.forecast.ForecastScreen
import com.example.weatherapp.ui.screens.journal.JournalScreen
import com.example.weatherapp.ui.screens.login.LoginScreen
import com.example.weatherapp.util.Constants

/**
 * Navigation item data class.
 */
data class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

/**
 * Main app composable with bottom navigation.
 */
@Composable
fun WeatherJournalApp(
    viewModel: MainViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    
    // Define navigation items based on auth state
    val navItems = if (isLoggedIn) {
        listOf(
            BottomNavItem(
                route = Constants.Routes.FORECAST,
                title = "Forecast",
                selectedIcon = Icons.Filled.Cloud,
                unselectedIcon = Icons.Outlined.Cloud
            ),
            BottomNavItem(
                route = Constants.Routes.JOURNAL,
                title = "Journal",
                selectedIcon = Icons.Filled.Book,
                unselectedIcon = Icons.Outlined.Book
            )
        )
    } else {
        listOf(
            BottomNavItem(
                route = Constants.Routes.FORECAST,
                title = "Forecast",
                selectedIcon = Icons.Filled.Cloud,
                unselectedIcon = Icons.Outlined.Cloud
            ),
            BottomNavItem(
                route = Constants.Routes.LOGIN,
                title = "Login",
                selectedIcon = Icons.Filled.Login,
                unselectedIcon = Icons.Outlined.Login
            )
        )
    }
    
    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                
                navItems.forEach { item ->
                    val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                    
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.title
                            )
                        },
                        label = { Text(item.title) },
                        selected = selected,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Constants.Routes.FORECAST,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Constants.Routes.FORECAST) {
                ForecastScreen()
            }
            
            composable(Constants.Routes.LOGIN) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Constants.Routes.JOURNAL) {
                            popUpTo(Constants.Routes.LOGIN) { inclusive = true }
                        }
                    }
                )
            }
            
            composable(Constants.Routes.JOURNAL) {
                JournalScreen(
                    onLogout = {
                        navController.navigate(Constants.Routes.FORECAST) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
