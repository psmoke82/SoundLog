package com.soundlog.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.soundlog.app.SoundLogApp
import com.soundlog.app.service.ForegroundSchedulerService
import com.soundlog.app.ui.dashboard.DashboardScreen
import com.soundlog.app.ui.logs.LogsScreen
import com.soundlog.app.ui.settings.SettingsScreen
import com.soundlog.app.ui.theme.DarkBackground
import com.soundlog.app.ui.theme.PrimaryNeon
import com.soundlog.app.ui.theme.SoundLogTheme
import com.soundlog.app.ui.theme.SurfaceDark
import com.soundlog.app.ui.theme.TextMuted

class MainActivity : ComponentActivity() {

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        checkAndStartService()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SoundLogTheme {
                MainAppScreen()
            }
        }

        try {
            checkPermissionsAndStart()
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error in checkPermissionsAndStart: ${e.message}", e)
        }
    }

    private fun checkPermissionsAndStart() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    return
                }
            }
            checkAndStartService()
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error requesting permission: ${e.message}", e)
        }
    }

    private fun checkAndStartService() {
        try {
            val settings = SoundLogApp.instance.settingsRepository
            if (settings.isServiceEnabled) {
                ForegroundSchedulerService.startService(this)
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error in checkAndStartService: ${e.message}", e)
        }
    }
}

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "대시보드", Icons.Default.Dashboard)
    object Logs : Screen("logs", "식별 로그", Icons.Default.ListAlt)
    object Settings : Screen("settings", "설정", Icons.Default.Settings)
}

@Composable
fun MainAppScreen() {
    val navController = rememberNavController()
    val items = listOf(Screen.Dashboard, Screen.Logs, Screen.Settings)

    Scaffold(
        containerColor = DarkBackground,
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceDark,
                contentColor = PrimaryNeon
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentRoute == screen.route,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PrimaryNeon,
                            selectedTextColor = PrimaryNeon,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted,
                            indicatorColor = SurfaceDark
                        ),
                        onClick = {
                            navController.navigate(screen.route) {
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
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(onNavigateToLogs = {
                    navController.navigate(Screen.Logs.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                })
            }
            composable(Screen.Logs.route) { LogsScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}
