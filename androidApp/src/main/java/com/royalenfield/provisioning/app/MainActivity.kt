package com.royalenfield.provisioning.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.royalenfield.provisioning.feature.dashboard.presentation.DashboardScreen
import com.royalenfield.provisioning.feature.dashboard.presentation.DashboardViewModel
import com.royalenfield.provisioning.feature.ota.presentation.OtaScreen
import com.royalenfield.provisioning.feature.ota.presentation.OtaViewModel
import com.royalenfield.provisioning.feature.supplierfeed.presentation.SupplierFeedScreen
import com.royalenfield.provisioning.feature.supplierfeed.presentation.SupplierFeedViewModel
import com.royalenfield.provisioning.feature.terminal.presentation.TerminalScreen
import com.royalenfield.provisioning.feature.terminal.presentation.TerminalViewModel
import com.royalenfield.provisioning.feature.wifi.presentation.WifiScreen
import com.royalenfield.provisioning.feature.wifi.presentation.WifiViewModel
import org.koin.androidx.compose.koinViewModel

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object Wifi : Screen("wifi", "SoftAP", Icons.Default.Wifi)
    object Ota : Screen("ota", "OTA Flash", Icons.Default.SystemUpdateAlt)
    object SupplierFeed : Screen("supplier_feed", "Supplier", Icons.Default.DeviceHub)
    object Terminal : Screen("terminal", "ADB Shell", Icons.Default.Terminal)
}

val navItems = listOf(
    Screen.Dashboard,
    Screen.Wifi,
    Screen.Ota,
    Screen.SupplierFeed,
    Screen.Terminal
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

    val env = EnvironmentConfig.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "ROYAL ENFIELD",
                            color = TextPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "FF MECHANIC",
                            color = RedPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                },
                actions = {
                    // Interactive Build Variant Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = env.badgeBackground,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clickable { showVariantDialog = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(env.badgeColor, shape = RoundedCornerShape(3.dp))
                            )
                            Text(
                                text = EnvironmentConfig.formattedVariantDisplay,
                                color = env.badgeColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurface
            ) {
                navItems.forEach { screen ->
                    val selected = currentRoute == screen.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(imageVector = screen.icon, contentDescription = screen.title)
                        },
                        label = {
                            Text(screen.title)
                        },
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
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                val viewModel: DashboardViewModel = koinViewModel()
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToWifi = { navController.navigate(Screen.Wifi.route) },
                    onNavigateToOta = { navController.navigate(Screen.Ota.route) },
                    onNavigateToSupplierFeed = { navController.navigate(Screen.SupplierFeed.route) }
                )
            }

            composable(Screen.Wifi.route) {
                val viewModel: WifiViewModel = koinViewModel()
                WifiScreen(viewModel = viewModel)
            }

            composable(Screen.Ota.route) {
                val viewModel: OtaViewModel = koinViewModel()
                OtaScreen(viewModel = viewModel)
            }

            composable(Screen.SupplierFeed.route) {
                val viewModel: SupplierFeedViewModel = koinViewModel()
                SupplierFeedScreen(viewModel = viewModel)
            }

            composable(Screen.Terminal.route) {
                val viewModel: TerminalViewModel = koinViewModel()
                TerminalScreen(viewModel = viewModel)
            }
        }

        if (showVariantDialog) {
            AlertDialog(
                onDismissRequest = { showVariantDialog = false },
                containerColor = DarkSurface,
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = env.badgeColor)
                        Text(text = "Build Variant Details", color = TextPrimary, fontSize = 16.sp)
                    }
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        VariantInfoRow("Environment", "${env.title} (${env.badgeText})")
                        VariantInfoRow("Build Type", BuildConfig.BUILD_TYPE)
                        VariantInfoRow("Flavor", BuildConfig.BUILD_VARIANT)
                        VariantInfoRow("API Base URL", EnvironmentConfig.ffBaseUrl.ifEmpty { "https://api.ffmechanic.royalenfield.com" })
                        VariantInfoRow("Provision URL", EnvironmentConfig.provisionBaseUrl.ifEmpty { "https://provision.tripper.royalenfield.com" })
                        VariantInfoRow("Mock Fallbacks", if (EnvironmentConfig.isMockFallbackAllowed) "ENABLED" else "DISABLED (STRICT)")
                        VariantInfoRow("Debug Logging", if (EnvironmentConfig.isDebugLoggingEnabled) "ENABLED" else "DISABLED")
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showVariantDialog = false }) {
                        Text("Close", color = RedPrimary)
                    }
                }
            )
        }
    }
}

@Composable
private fun VariantInfoRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, color = TextSecondary, fontSize = 11.sp)
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold
        )
        Divider(color = DarkSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
    }
}
