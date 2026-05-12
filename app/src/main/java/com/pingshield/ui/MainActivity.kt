package com.pingshield.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PingShieldNav() }
    }
}

sealed class Screen(val route: String, val title: String) {
    data object Dashboard : Screen("dashboard", "Dashboard")
    data object Stats : Screen("stats", "Stats")
    data object Apps : Screen("apps", "Apps")
    data object Settings : Screen("settings", "Settings")
}

@Composable
fun PingShieldNav() {
    val navController = rememberNavController()
    val screens = listOf(Screen.Dashboard, Screen.Stats, Screen.Apps, Screen.Settings)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFF0A0A0F),
        bottomBar = {
            NavigationBar(
                modifier = Modifier.fillMaxWidth().height(60.dp),
                containerColor = Color(0xFF14141A),
                contentColor = Color(0xFF00E5CC)
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val current = navBackStackEntry?.destination
                screens.forEach { screen ->
                    NavigationBarItem(
                        icon = { Text(
                            text = when (screen) {
                                Screen.Dashboard -> "DH"
                                Screen.Stats -> "ST"
                                Screen.Apps -> "AP"
                                Screen.Settings -> "SG"
                            }, fontSize = 13.sp, fontWeight = FontWeight.Bold
                        )},
                        label = { Text(screen.title, fontSize = 10.sp) },
                        selected = current?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true; restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF00E5CC),
                            selectedTextColor = Color(0xFF00E5CC),
                            unselectedIconColor = Color(0xFF555555),
                            unselectedTextColor = Color(0xFF555555),
                            indicatorColor = Color(0xFF1A1A24)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(navController = navController, startDestination = Screen.Dashboard.route, modifier = Modifier.padding(innerPadding)) {
            composable(Screen.Dashboard.route) { DashboardScreen() }
            composable(Screen.Stats.route) { StatsScreen() }
            composable(Screen.Apps.route) { AppsScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}
