package com.royalenfield.provisioning.feature.dashboard.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.royalenfield.provisioning.core.theme.*

// Colors matched to the reference screenshot
private val ScreenBackground = Color(0xFF141716)
private val CardBackground = Color(0xFF1B1F1E)
private val TopBarBackButtonBg = Color(0xFF222725)
private val TextMutedGray = Color(0xFF9E9E9E)
private val LabelGray = Color(0xFF757575)
private val UnderlineColor = Color(0xFF38403C)
private val GreenButtonBg = Color(0xFF102820)
private val GreenButtonBorder = Color(0xFF2DD4BF)
private val GreenButtonText = Color(0xFF34D399)

/**
 * Unified Wi-Fi & ADB Setup Screen matching the reference layout
 * Displays Vehicle Wi-Fi and ADB Connect sections vertically one by one.
 */
@Composable
fun WifiAdbUnifiedSetupScreen(
    viewModel: DashboardViewModel,
    onNavigateToDashboard: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground)
    ) {
        // Top Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular Back Button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(TopBarBackButtonBg)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Centered Title
            Text(
                text = "Wi-Fi & ADB",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 40.dp) // Balance the back button
            )
        }

        // Scrollable Content displaying Wi-Fi and ADB sections vertically
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // ================= SECTION 1: VEHICLE WI-FI =================
            Text(
                text = "VEHICLE WI-FI",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Enter the vehicle's Wi-Fi SSID and passphrase to join its network.",
                color = TextMutedGray,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Wi-Fi SSID Input
            OutlinedTextField(
                value = uiState.ssidInput,
                onValueChange = { viewModel.onSsidChanged(it) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Wi-Fi SSID") },
                placeholder = { Text("Enter Wi-Fi SSID", color = LabelGray) },
                trailingIcon = {
                    if (uiState.ssidInput.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.onSsidChanged("") },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear SSID",
                                tint = LabelGray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                isError = uiState.ssidValidationError != null,
                supportingText = {
                    if (uiState.ssidValidationError != null) {
                        Text(
                            text = uiState.ssidValidationError ?: "",
                            color = RedPrimary,
                            fontSize = 12.sp
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = GreenButtonBorder,
                    unfocusedBorderColor = UnderlineColor,
                    errorBorderColor = RedPrimary,
                    focusedLabelColor = GreenButtonText,
                    unfocusedLabelColor = LabelGray,
                    cursorColor = GreenButtonText
                ),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Wi-Fi Passphrase Input
            OutlinedTextField(
                value = uiState.passwordInput,
                onValueChange = { viewModel.onPasswordChanged(it) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Wi-Fi Passphrase") },
                placeholder = { Text("Enter Passphrase", color = LabelGray) },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        if (uiState.passwordInput.isNotEmpty()) {
                            IconButton(
                                onClick = { viewModel.onPasswordChanged("") },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear Password",
                                    tint = LabelGray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        IconButton(
                            onClick = { passwordVisible = !passwordVisible },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle Password Visibility",
                                tint = LabelGray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = GreenButtonBorder,
                    unfocusedBorderColor = UnderlineColor,
                    errorBorderColor = RedPrimary,
                    focusedLabelColor = GreenButtonText,
                    unfocusedLabelColor = LabelGray,
                    cursorColor = GreenButtonText
                ),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Wi-Fi Connection Action Button
            Button(
                onClick = {
                    if (uiState.isWifiConnected) {
                        viewModel.disconnectWifi()
                    } else {
                        viewModel.connectWifi()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(
                    1.dp,
                    if (uiState.isWifiConnected) GreenButtonBorder else Color(0xFF26332E)
                ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.isWifiConnected) GreenButtonBg else Color(0xFF17201D),
                    contentColor = if (uiState.isWifiConnected) GreenButtonText else Color(0xFF94A3B8)
                ),
                enabled = !uiState.isWifiConnecting
            ) {
                if (uiState.isWifiConnecting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = GreenButtonText,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Connecting to Wi-Fi...", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                } else if (uiState.isWifiConnected) {
                    Text(
                        text = "Wi-Fi Connected",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GreenButtonText
                    )
                } else {
                    Text(
                        text = "Connect to Wi-Fi",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }

            if (uiState.wifiErrorMessage != null) {
                Text(
                    text = uiState.wifiErrorMessage ?: "",
                    color = RedPrimary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                )
            }

            // ================= SECTION 2: ADB CONNECT (Shown only once Wi-Fi is connected) =================
            AnimatedVisibility(
                visible = uiState.isWifiConnected,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Divider between Wi-Fi and ADB sections
                    HorizontalDivider(
                        color = Color(0xFF222825),
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 32.dp)
                    )

                    Text(
                        text = "ADB Connect",
                        color = Color.White,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Connect to the vehicle's Android unit over ADB now that the phone has joined its Wi-Fi network.",
                        color = TextMutedGray,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // Device IP Address Input
                    OutlinedTextField(
                        value = uiState.adbHostInput,
                        onValueChange = { viewModel.onAdbHostChanged(it) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Device IP Address") },
                        placeholder = { Text("Enter Device IP Address (e.g. 192.168.1.1)", color = LabelGray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        trailingIcon = {
                            if (uiState.adbHostInput.isNotEmpty()) {
                                IconButton(
                                    onClick = { viewModel.onAdbHostChanged("") },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear IP",
                                        tint = LabelGray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = GreenButtonBorder,
                            unfocusedBorderColor = UnderlineColor,
                            errorBorderColor = RedPrimary,
                            focusedLabelColor = GreenButtonText,
                            unfocusedLabelColor = LabelGray,
                            cursorColor = GreenButtonText
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // ADB Connection Action Button
                    Button(
                        onClick = {
                            if (uiState.isAdbConnected) {
                                viewModel.disconnectAdb()
                            } else {
                                viewModel.connectAdb()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(
                            1.dp,
                            if (uiState.isAdbConnected) GreenButtonBorder else Color(0xFF26332E)
                        ),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (uiState.isAdbConnected) GreenButtonBg else Color(0xFF17201D),
                            contentColor = if (uiState.isAdbConnected) GreenButtonText else Color(0xFF94A3B8)
                        ),
                        enabled = !uiState.isAdbConnecting
                    ) {
                        if (uiState.isAdbConnecting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = GreenButtonText,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Connecting ADB...", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        } else if (uiState.isAdbConnected) {
                            Text(
                                text = "ADB Connected — Disconnect",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = GreenButtonText
                            )
                        } else {
                            Text(
                                text = "Connect via ADB",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }

                    if (uiState.adbErrorMessage != null) {
                        Text(
                            text = uiState.adbErrorMessage ?: "",
                            color = RedPrimary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                        )
                    }

                    if (uiState.isAdbConnected) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onNavigateToDashboard,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                        ) {
                            Text("ENTER SERVICE DASHBOARD", fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null, tint = Color.White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// Aliases for compatibility
@Composable
fun WifiSetupScreen(
    viewModel: DashboardViewModel,
    onWifiConnected: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    WifiAdbUnifiedSetupScreen(
        viewModel = viewModel,
        onNavigateToDashboard = onWifiConnected,
        onBack = onBack
    )
}

@Composable
fun AdbSetupScreen(
    viewModel: DashboardViewModel,
    onAdbConnected: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    WifiAdbUnifiedSetupScreen(
        viewModel = viewModel,
        onNavigateToDashboard = onAdbConnected,
        onBack = onBack
    )
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
            .background(ScreenBackground)
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
            border = BorderStroke(1.dp, RedPrimary),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = RedPrimary)
        ) {
            Text("TERMINATE VEHICLE LINK", fontWeight = FontWeight.Bold)
        }
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
