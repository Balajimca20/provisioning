package com.royalenfield.ffmechanic.app.app.navigation

import android.app.Activity
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.royalenfield.ffmechanic.app.feature.dashboard.presentation.DashboardScreen
import com.royalenfield.ffmechanic.app.feature.ota.presentation.OtaScreen
import com.royalenfield.ffmechanic.app.feature.supplierfeed.presentation.SupplierFeedScreen
import com.royalenfield.ffmechanic.app.feature.wifi.presentation.WifiScreen

sealed class Tab(val route: String, val label: String) {
    // NOTE: the original tool also has a fourth "⚡ SYSTEM PROVISIONING" tab (VIN registration
    // via adb+curl, ECU/model metadata, monitor_logic()) that wasn't in the three features you
    // scoped for this pass — see the README for what's ported vs. still Python-only.
    data object SupplierFeed : Tab("supplier_feed", "Supplier Feed")
    data object Wifi : Tab("wifi", "Wi-Fi Tracker")
    data object Ota : Tab("ota", "OTA")
}

private val tabs = listOf(Tab.SupplierFeed, Tab.Wifi, Tab.Ota)

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    var isFeatureAccessEnabled by remember { mutableStateOf(false) }
    var blockDashboardAutoOpen by rememberSaveable { mutableStateOf(false) }
    var lastDashboardBackPressAt by rememberSaveable { mutableStateOf(0L) }
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    if (!isFeatureAccessEnabled) {
        BackHandler {
            val now = SystemClock.elapsedRealtime()
            if (now - lastDashboardBackPressAt < 2_000L) {
                (context as? Activity)?.finish() ?: backDispatcher?.onBackPressed()
            } else {
                lastDashboardBackPressAt = now
                Toast.makeText(
                    context,
                    "Press back again to exit",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
        DashboardScreen(
            onReady = {
                blockDashboardAutoOpen = false
                isFeatureAccessEnabled = true
            },
            enableAutoOpen = !blockDashboardAutoOpen,
        )
        return
    }

    // Any system back press on feature screens returns users to the dashboard gate.
    BackHandler(enabled = isFeatureAccessEnabled) {
        blockDashboardAutoOpen = true
        isFeatureAccessEnabled = false
    }

    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = backStackEntry?.destination

                tabs.forEach { tab ->
                    val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                when (tab) {
                                    Tab.SupplierFeed -> Icons.Filled.Storage
                                    Tab.Wifi -> Icons.Filled.Wifi
                                    Tab.Ota -> Icons.Filled.CloudSync
                                },
                                contentDescription = tab.label,
                            )
                        },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Tab.SupplierFeed.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(Tab.SupplierFeed.route) { SupplierFeedScreen() }
            composable(Tab.Wifi.route) { WifiScreen() }
            composable(Tab.Ota.route) { OtaScreen() }
        }
    }
}
