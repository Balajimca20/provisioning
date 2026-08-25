package com.royalenfield.provisioning.feature.dashboard.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.royalenfield.provisioning.core.theme.*

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToWifi: () -> Unit = {},
    onNavigateToOta: () -> Unit = {},
    onNavigateToSupplierFeed: () -> Unit = {}
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
        // Top Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TwoWheeler,
                        contentDescription = null,
                        tint = RedPrimary
                    )
                    Text(
                        text = "FF PROVISIONING",
                        style = MaterialTheme.typography.titleMedium,
                        color = RedPrimary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Vehicle Provisioning & Field Service Suite",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        // Quick Stats Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickStatCard(
                modifier = Modifier.weight(1f),
                label = "Battery",
                value = uiState.batteryVoltage,
                icon = Icons.Default.BatteryChargingFull,
                tint = GreenAccent
            )
            QuickStatCard(
                modifier = Modifier.weight(1f),
                label = "ECU Link",
                value = uiState.ecuStatus,
                icon = Icons.Default.Speed,
                tint = BlueAccent
            )
            QuickStatCard(
                modifier = Modifier.weight(1f),
                label = "Storage",
                value = uiState.storageSpace,
                icon = Icons.Default.Storage,
                tint = AmberAccent
            )
        }

        // STEP 1: Wi-Fi Provisioning Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (uiState.isWifiConnected) GreenAccent.copy(alpha = 0.2f) else RedPrimary.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "STEP 1",
                                color = if (uiState.isWifiConnected) GreenAccent else RedPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = "Vehicle SoftAP Connection",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                    }

                    if (uiState.isWifiConnected) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = GreenAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Connected",
                                color = GreenAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (!uiState.isWifiConnected) {
                    OutlinedTextField(
                        value = uiState.ssidInput,
                        onValueChange = { viewModel.onSsidChanged(it) },
                        label = { Text("Vehicle SSID (RE_XXXX_XXXXXX)") },
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
                        Text(
                            text = uiState.ssidValidationError ?: "",
                            color = RedPrimary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = uiState.passwordInput,
                        onValueChange = { viewModel.onPasswordChanged(it) },
                        label = { Text("WPA2 Pre-Shared Key") },
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

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.connectWifi() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isWifiConnecting && uiState.ssidValidationError == null,
                        colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (uiState.isWifiConnecting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Connecting...")
                        } else {
                            Icon(imageVector = Icons.Default.Wifi, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Connect Vehicle Wi-Fi")
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurfaceVariant.copy(alpha = 0.5f))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "SSID: ${uiState.connectedSsid ?: uiState.ssidInput}",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (uiState.isProcessBound) "✓ Process bound to non-internet Wi-Fi socket" else "Binding process...",
                            color = if (uiState.isProcessBound) GreenAccent else AmberAccent,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { viewModel.disconnectWifi() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RedPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Disconnect Wi-Fi")
                    }
                }

                if (uiState.wifiErrorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Error: ${uiState.wifiErrorMessage}",
                        color = RedPrimary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // STEP 2: ADB Bridge Card (Visible if Wi-Fi connected)
        AnimatedVisibility(visible = uiState.isWifiConnected) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (uiState.isAdbConnected) GreenAccent.copy(alpha = 0.2f) else BlueAccent.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "STEP 2",
                                    color = if (uiState.isAdbConnected) GreenAccent else BlueAccent,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = "ADB TCP Daemon Bridge",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                        }

                        if (uiState.isAdbConnected) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = if (uiState.isAdbRooted) GreenAccent else AmberAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (uiState.isAdbRooted) "Rooted (uid=0)" else "Connected (Unrooted)",
                                    color = if (uiState.isAdbRooted) GreenAccent else AmberAccent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (!uiState.isAdbConnected) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = uiState.adbHostInput,
                                onValueChange = { viewModel.onAdbHostChanged(it) },
                                label = { Text("IP Address") },
                                modifier = Modifier.weight(2f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BlueAccent,
                                    unfocusedBorderColor = DarkSurfaceVariant,
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
                                    unfocusedBorderColor = DarkSurfaceVariant,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { viewModel.connectAdb() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !uiState.isAdbConnecting,
                            colors = ButtonDefaults.buttonColors(containerColor = BlueAccent),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (uiState.isAdbConnecting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Establishing ADB Socket...")
                            } else {
                                Icon(imageVector = Icons.Default.Terminal, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Connect ADB TCP")
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSurfaceVariant.copy(alpha = 0.5f))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "Host: ${uiState.adbHostInput}:${uiState.adbPortInput}",
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Cluster Build: ${uiState.clusterBuild}",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.disconnectAdb() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = RedPrimary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Disconnect ADB")
                            }

                            Button(
                                onClick = { viewModel.rebootVehicle() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = AmberAccent),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reboot Cluster", color = Color.Black)
                            }
                        }
                    }

                    if (uiState.adbErrorMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "ADB Error: ${uiState.adbErrorMessage}",
                            color = RedPrimary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Quick Navigation Tiles
        Text(
            text = "MECHANIC WORKFLOW MODULES",
            style = MaterialTheme.typography.labelMedium,
            color = TextMuted,
            letterSpacing = 1.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ModuleTile(
                modifier = Modifier.weight(1f),
                title = "SoftAP Manager",
                subtitle = "XML Credential Sync",
                icon = Icons.Default.SettingsSuggest,
                color = BlueAccent,
                onClick = onNavigateToWifi
            )
            ModuleTile(
                modifier = Modifier.weight(1f),
                title = "OTA Firmware",
                subtitle = "A/B Partition Deploy",
                icon = Icons.Default.SystemUpdateAlt,
                color = AmberAccent,
                onClick = onNavigateToOta
            )
            ModuleTile(
                modifier = Modifier.weight(1f),
                title = "Supplier Feed",
                subtitle = "GraphQL Telemetry",
                icon = Icons.Default.DeviceHub,
                color = PurpleAccent,
                onClick = onNavigateToSupplierFeed
            )
        }
    }
}

@Composable
private fun QuickStatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = label,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = value,
                fontSize = 13.sp,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ModuleTile(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(10.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = color.copy(alpha = 0.15f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier
                        .padding(6.dp)
                        .size(20.dp)
                )
            }
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = TextSecondary,
                lineHeight = 14.sp
            )
        }
    }
}
