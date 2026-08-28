package com.royalenfield.provisioning.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.royalenfield.provisioning.BuildConfig
import com.royalenfield.provisioning.core.config.EnvironmentConfig
import com.royalenfield.provisioning.core.theme.DarkBackground
import com.royalenfield.provisioning.core.theme.DarkSurface
import com.royalenfield.provisioning.core.theme.DarkSurfaceVariant
import com.royalenfield.provisioning.core.theme.FfMechanicTheme
import com.royalenfield.provisioning.core.theme.RedPrimary
import com.royalenfield.provisioning.core.theme.TextPrimary
import com.royalenfield.provisioning.core.theme.TextSecondary
import com.royalenfield.provisioning.feature.dashboard.presentation.AdbSetupScreen
import com.royalenfield.provisioning.feature.dashboard.presentation.DashboardFunctionalScreen
import com.royalenfield.provisioning.feature.dashboard.presentation.DashboardViewModel
import com.royalenfield.provisioning.feature.dashboard.presentation.LandingScreen
import com.royalenfield.provisioning.feature.dashboard.presentation.WifiSetupScreen
import com.royalenfield.provisioning.feature.ota.presentation.CommandLineOTAView
import com.royalenfield.provisioning.feature.ota.presentation.CommandLineOTAViewModel
import com.royalenfield.provisioning.feature.provisioning.presentation.ProvisioningRoute
import com.royalenfield.provisioning.feature.supplierfeed.presentation.SupplierFeedScreen
import com.royalenfield.provisioning.feature.supplierfeed.presentation.SupplierFeedViewModel
import com.royalenfield.provisioning.feature.terminal.presentation.TerminalScreen
import com.royalenfield.provisioning.feature.terminal.presentation.TerminalViewModel
import com.royalenfield.provisioning.feature.wifi.presentation.WifiScreen
import com.royalenfield.provisioning.feature.wifi.presentation.WifiViewModel
import org.koin.androidx.compose.koinViewModel

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Landing : Screen("landing", "Welcome")
    object WifiSetup : Screen("wifi_setup", "Wi-Fi Setup")
    object AdbSetup : Screen("adb_setup", "ADB Setup")
    
    // Core functional screens
    object Dashboard : Screen("dashboard", "Stats", Icons.Default.Dashboard)
    object Wifi : Screen("wifi", "SoftAP", Icons.Default.Wifi)
    object Ota : Screen("ota", "OTA Flash", Icons.Default.SystemUpdateAlt)
    object SupplierFeed : Screen("supplier_feed", "Supplier", Icons.Default.DeviceHub)
    object Terminal : Screen("terminal", "ADB Shell", Icons.Default.Terminal)

    object SystemProvisioning : Screen("system_provisioning", "Provisioning", Icons.Default.FlashOn)
}

val serviceNavItems = listOf(
    Screen.Wifi,
    Screen.Ota,
    Screen.SupplierFeed,
//    Screen.Terminal,
    Screen.SystemProvisioning
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FfMechanicTheme {
                MainAppContent()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    var showVariantDialog by remember { mutableStateOf(false) }
    
    // Shared Connectivity State
    val dashboardViewModel: DashboardViewModel = koinViewModel()
    val uiState by dashboardViewModel.uiState.collectAsState()

    val env = EnvironmentConfig.current

    // Automatically navigate to Wifi or ADB screen whenever disconnected based on status
    LaunchedEffect(uiState.isWifiConnected, uiState.isAdbConnected, currentRoute) {
        if (currentRoute != Screen.Landing.route) {
            if (!uiState.isWifiConnected) {
                if (currentRoute != Screen.WifiSetup.route) {
                    navController.navigate(Screen.WifiSetup.route) {
                        popUpTo(Screen.Landing.route) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            } else if (!uiState.isAdbConnected) {
                if (currentRoute != Screen.AdbSetup.route && currentRoute != Screen.WifiSetup.route) {
                    navController.navigate(Screen.AdbSetup.route) {
                        popUpTo(Screen.Landing.route) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(imageVector = Icons.Default.TwoWheeler, contentDescription = null, tint = RedPrimary)
                        Text(text = "FF PROVISIONING", color = RedPrimary, fontWeight = FontWeight.Black, fontSize = 15.sp, letterSpacing = 1.sp)
                    }
                },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = env.badgeBackground,
                        modifier = Modifier.padding(end = 12.dp).clickable { showVariantDialog = true }
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(6.dp).background(env.badgeColor, shape = RoundedCornerShape(3.dp)))
                            Text(text = EnvironmentConfig.formattedVariantDisplay, color = env.badgeColor, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        bottomBar = {
            // Navigation bar only shown when ADB is linked and we are out of setup
            val isSetupScreen = currentRoute == Screen.Landing.route || 
                              currentRoute == Screen.WifiSetup.route || 
                              currentRoute == Screen.AdbSetup.route
            if (uiState.isAdbConnected && !isSetupScreen) {
                NavigationBar(containerColor = DarkSurface) {
                    serviceNavItems.forEach { screen ->
                        val selected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(imageVector = screen.icon!!, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = TextPrimary,
                                selectedTextColor = RedPrimary,
                                indicatorColor = RedPrimary.copy(alpha = 0.25f),
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Landing.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Landing.route) {
                LandingScreen(onStartSetup = { navController.navigate(Screen.WifiSetup.route) })
            }

            composable(Screen.WifiSetup.route) {
                WifiSetupScreen(
                    viewModel = dashboardViewModel,
                    onWifiConnected = {
                        navController.navigate(Screen.AdbSetup.route)
                    }
                )
            }

            composable(Screen.AdbSetup.route) {
                AdbSetupScreen(
                    viewModel = dashboardViewModel,
                    onAdbConnected = {
                        // After bridge established, move to dashboard and clear setup flow
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Landing.route) { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Dashboard.route) {
                DashboardFunctionalScreen(
                    viewModel = dashboardViewModel,
                    onNavigateToModule = { route -> navController.navigate(route) }
                )
            }

            composable(Screen.Wifi.route) {
                val viewModel: WifiViewModel = koinViewModel()
                WifiScreen(viewModel = viewModel)
            }

            composable(Screen.Ota.route) {
                val viewModel: CommandLineOTAViewModel = koinViewModel()
                CommandLineOTAView(viewModel = viewModel)
            }

            composable(Screen.SupplierFeed.route) {
                val viewModel: SupplierFeedViewModel = koinViewModel()
                SupplierFeedScreen(viewModel = viewModel)
            }

            composable(Screen.Terminal.route) {
                val viewModel: TerminalViewModel = koinViewModel()
                TerminalScreen(viewModel = viewModel)
            }

            composable(Screen.SystemProvisioning.route) {
                ProvisioningRoute()
            }
        }

        if (showVariantDialog) {
            AlertDialog(
                onDismissRequest = { showVariantDialog = false },
                containerColor = DarkSurface,
                title = { Text(text = "Build Variant Details", color = TextPrimary, fontSize = 16.sp) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        VariantInfoRow("Environment", env.title)
                        VariantInfoRow("Build Type", BuildConfig.BUILD_TYPE)
                        VariantInfoRow("Flavor", BuildConfig.BUILD_VARIANT)
                        VariantInfoRow("API Base URL", EnvironmentConfig.ffBaseUrl)
                    }
                },
                confirmButton = { TextButton(onClick = { showVariantDialog = false }) { Text("Close", color = RedPrimary) } }
            )
        }
    }
}

@Composable
private fun VariantInfoRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, color = TextSecondary, fontSize = 11.sp)
        Text(text = value, color = TextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        HorizontalDivider(color = DarkSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
    }
}
