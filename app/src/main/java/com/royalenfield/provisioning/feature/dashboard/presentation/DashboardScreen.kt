package com.royalenfield.provisioning.feature.dashboard.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.royalenfield.provisioning.core.theme.*

@Composable
fun LandingScreen(
    onStartSetup: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.TwoWheeler,
            contentDescription = null,
            tint = RedPrimary,
            modifier = Modifier.size(100.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "FF PROVISIONING",
            style = MaterialTheme.typography.headlineMedium,
            color = RedPrimary,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp
        )
        Text(
            text = "Vehicle Service & Connectivity Suite",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary
        )
        
        Spacer(modifier = Modifier.height(64.dp))
        
        Button(
            onClick = onStartSetup,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("BEGIN CONNECTION SETUP", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun WifiSetupScreen(
    viewModel: DashboardViewModel,
    onWifiConnected: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    // Automatically navigate once connected
    LaunchedEffect(uiState.isWifiConnected) {
        if (uiState.isWifiConnected) {
            onWifiConnected()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SetupHeader(step = 1, title = "Vehicle Wi-Fi")
        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(text = "Step 1: Local Network link", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                Text(text = "Connect to motorcycle SoftAP to begin telemetry sync.", style = MaterialTheme.typography.bodySmall, color = TextMuted, modifier = Modifier.padding(top = 4.dp, bottom = 24.dp))

                OutlinedTextField(
                    value = uiState.ssidInput,
                    onValueChange = { viewModel.onSsidChanged(it) },
                    label = { Text("Vehicle SSID") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = uiState.ssidValidationError != null,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RedPrimary,
                        unfocusedBorderColor = DarkSurfaceVariant,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                if (uiState.ssidValidationError != null) {
                    Text(text = uiState.ssidValidationError ?: "", color = RedPrimary, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = uiState.passwordInput,
                    onValueChange = { viewModel.onPasswordChanged(it) },
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RedPrimary,
                        unfocusedBorderColor = DarkSurfaceVariant,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { viewModel.connectWifi() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isWifiConnecting && uiState.ssidValidationError == null,
                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (uiState.isWifiConnecting) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                    else Text("CONNECT TO VEHICLE")
                }

                if (uiState.wifiErrorMessage != null) {
                    Text(text = uiState.wifiErrorMessage ?: "", color = RedPrimary, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 16.dp))
                }
            }
        }
    }
}

@Composable
fun AdbSetupScreen(
    viewModel: DashboardViewModel,
    onAdbConnected: () -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    // Automatically navigate once connected
    LaunchedEffect(uiState.isAdbConnected) {
        if (uiState.isAdbConnected) {
            onAdbConnected()
        }
    }

    // Safety: If Wi-Fi link drops, pop back
    LaunchedEffect(uiState.isWifiConnected) {
        if (!uiState.isWifiConnected) {
            onBack()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SetupHeader(step = 2, title = "ADB Bridge")
        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { 
                    viewModel.disconnectWifi()
                    onBack()
                }) {
                    Icon(imageVector = Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back", tint = TextSecondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Back to Wi-Fi", color = TextSecondary, fontSize = 14.sp)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(text = "Step 2: Service Tunnel", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                Text(text = "Connected to ${uiState.connectedSsid}. Establishing bridge...", style = MaterialTheme.typography.bodySmall, color = GreenAccent, modifier = Modifier.padding(top = 4.dp, bottom = 24.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = uiState.adbHostInput,
                        onValueChange = { viewModel.onAdbHostChanged(it) },
                        label = { Text("Vehicle IP") },
                        modifier = Modifier.weight(1.5f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BlueAccent,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    OutlinedTextField(
                        value = uiState.adbPortInput,
                        onValueChange = { viewModel.onAdbPortChanged(it) },
                        label = { Text("Port") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BlueAccent,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { viewModel.connectAdb() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isAdbConnecting,
                    colors = ButtonDefaults.buttonColors(containerColor = BlueAccent),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (uiState.isAdbConnecting) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                    else Text("ESTABLISH ADB LINK")
                }

                if (uiState.adbErrorMessage != null) {
                    Text(text = uiState.adbErrorMessage ?: "", color = RedPrimary, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 16.dp))
                }
            }
        }
    }
}

@Composable
fun DashboardFunctionalScreen(
    viewModel: DashboardViewModel,
    onNavigateToModule: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Quick Telemetry Overview
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.Dashboard, contentDescription = null, tint = RedPrimary, modifier = Modifier.size(16.dp))
                    Text(text = "VEHICLE LIVE STATUS", style = MaterialTheme.typography.labelMedium, color = RedPrimary, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickStatCard(modifier = Modifier.weight(1f), label = "Battery", value = uiState.batteryVoltage, icon = Icons.Default.BatteryChargingFull, tint = GreenAccent)
                    QuickStatCard(modifier = Modifier.weight(1f), label = "ECU Link", value = uiState.ecuStatus, icon = Icons.Default.Speed, tint = BlueAccent)
                    QuickStatCard(modifier = Modifier.weight(1f), label = "Storage", value = uiState.storageSpace, icon = Icons.Default.Storage, tint = AmberAccent)
                }
            }
        }

        // Service Module Grid
        Text(text = "SERVICE MODULES", style = MaterialTheme.typography.labelMedium, color = TextMuted, letterSpacing = 1.sp, modifier = Modifier.padding(horizontal = 4.dp))
        
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ModuleTile(modifier = Modifier.weight(1f), title = "SoftAP Manager", subtitle = "XML Sync", icon = Icons.Default.SettingsSuggest, color = BlueAccent, onClick = { onNavigateToModule("wifi") })
                ModuleTile(modifier = Modifier.weight(1f), title = "OTA Firmware", subtitle = "A/B Partition", icon = Icons.Default.SystemUpdateAlt, color = AmberAccent, onClick = { onNavigateToModule("ota") })
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ModuleTile(modifier = Modifier.weight(1f), title = "Supplier Feed", subtitle = "Raw Telemetry", icon = Icons.Default.DeviceHub, color = PurpleAccent, onClick = { onNavigateToModule("supplier_feed") })
                ModuleTile(modifier = Modifier.weight(1f), title = "ADB Shell", subtitle = "Terminal", icon = Icons.Default.Terminal, color = RedPrimary, onClick = { onNavigateToModule("terminal") })
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Link Management
        OutlinedButton(
            onClick = { viewModel.disconnectWifi() },
            modifier = Modifier.fillMaxWidth(),
            border = androidx.compose.foundation.BorderStroke(1.dp, RedPrimary),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = RedPrimary)
        ) {
            Text("TERMINATE VEHICLE LINK", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SetupHeader(step: Int, title: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(shape = RoundedCornerShape(12.dp), color = if (step == 1) RedPrimary.copy(alpha = 0.1f) else BlueAccent.copy(alpha = 0.1f)) {
            Text(text = "STEP $step", color = if (step == 1) RedPrimary else BlueAccent, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), fontWeight = FontWeight.Black, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = title, style = MaterialTheme.typography.headlineSmall, color = TextPrimary, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun QuickStatCard(modifier: Modifier = Modifier, label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color) {
    Surface(modifier = modifier, color = DarkSurfaceVariant.copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
                Text(text = label, fontSize = 10.sp, color = TextSecondary)
            }
            Text(text = value, fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ModuleTile(modifier: Modifier = Modifier, title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, onClick: () -> Unit) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = DarkSurface), shape = RoundedCornerShape(10.dp), onClick = onClick) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(shape = RoundedCornerShape(6.dp), color = color.copy(alpha = 0.1f)) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.padding(6.dp).size(20.dp))
            }
            Column {
                Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(text = subtitle, fontSize = 11.sp, color = TextSecondary)
            }
        }
    }
}
