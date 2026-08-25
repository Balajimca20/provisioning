package com.royalenfield.ffmechanic.app.feature.dashboard.presentation

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

private val GreenOk = Color(0xFF2E7D32)
private val RedError = Color(0xFFC62828)
private val OrangePending = Color(0xFFE65100)

@Composable
fun DashboardScreen(
    onReady: () -> Unit,
    enableAutoOpen: Boolean = true,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(state.canOpenFeatures, enableAutoOpen) {
        if (enableAutoOpen && state.canOpenFeatures) onReady()
    }

    LaunchedEffect(Unit) {
        viewModel.openWifiSettingsEvent.collect {
            context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {

        // ── Header ─────────────────────────────────────────────────────────
        Text(
            text = "Connection Dashboard",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Connect to the vehicle Wi-Fi and ADB to access features.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // ── Step status summary row ─────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StepBadge(
                step = 1,
                label = "Wi-Fi",
                icon = Icons.Filled.Wifi,
                done = state.isWifiConnected,
                active = !state.isWifiConnected,
                modifier = Modifier.weight(1f),
            )
            StepBadge(
                step = 2,
                label = "ADB",
                icon = Icons.Filled.Cable,
                done = state.isAdbConnected,
                active = state.isWifiConnected && !state.isAdbConnected,
                modifier = Modifier.weight(1f),
            )
        }

        // ── Wi-Fi card ──────────────────────────────────────────────────────
        ConnectionCard(
            title = "Step 1 — Wi-Fi",
            icon = Icons.Filled.Wifi,
            connected = state.isWifiConnected,
            connecting = state.isConnectingWifi,
            statusText = state.wifiStatus,
        ) {
            AnimatedVisibility(visible = !state.isWifiConnected) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.wifiSsid,
                        onValueChange = viewModel::onWifiSsidChanged,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !state.isConnectingWifi,
                        label = { Text("SSID") },
                        placeholder = { Text("e.g. RE_LXHD_250925") },
                        leadingIcon = {
                            Icon(Icons.Filled.Wifi, contentDescription = null, modifier = Modifier.size(20.dp))
                        },
                        isError = state.wifiValidationError != null,
                        supportingText = {
                            if (state.wifiValidationError != null) {
                                Text(
                                    text = state.wifiValidationError!!,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        },
                    )
                    OutlinedTextField(
                        value = state.wifiPassword,
                        onValueChange = viewModel::onWifiPasswordChanged,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !state.isConnectingWifi,
                        visualTransformation = if (passwordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        label = { Text("Password") },
                        placeholder = { Text("Wi-Fi password") },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) {
                                        Icons.Filled.VisibilityOff
                                    } else {
                                        Icons.Filled.Visibility
                                    },
                                    contentDescription = if (passwordVisible) {
                                        "Hide password"
                                    } else {
                                        "Show password"
                                    },
                                )
                            }
                        },
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = {
                        if (state.isWifiConnected) viewModel.disconnectWifi() else viewModel.connectWifi()
                    },
                    enabled = if (state.isWifiConnected) !state.isConnectingWifi else state.canConnectWifi,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = if (state.isWifiConnected) {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        )
                    } else {
                        ButtonDefaults.buttonColors()
                    },
                ) {
                    if (state.isConnectingWifi) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Waiting for Wi-Fi…")
                    } else {
                        Text(if (state.isWifiConnected) "Disconnect" else "Connect")
                    }
                }
                AnimatedVisibility(
                    visible = state.isConnectingWifi,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    TextButton(
                        onClick = viewModel::cancelWifiConnect,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                    ) { Text("Cancel") }
                }
            }
        }

        // ── ADB card ────────────────────────────────────────────────────────
        ConnectionCard(
            title = "Step 2 — ADB",
            icon = Icons.Filled.Cable,
            connected = state.isAdbConnected,
            connecting = state.isConnectingAdb,
            statusText = state.adbStatus,
            dimmed = !state.isWifiConnected,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AnimatedVisibility(
                    visible = !state.isAdbConnected,
                    modifier = Modifier.weight(1f),
                ) {
                    OutlinedTextField(
                        value = state.deviceHost,
                        onValueChange = viewModel::onHostChanged,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = state.isWifiConnected && !state.isConnectingAdb,
                        label = { Text("Device IP") },
                        placeholder = { Text("192.168.1.1") },
                    )
                }
                Button(
                    onClick = viewModel::connectAdb,
                    enabled = !state.isConnectingAdb && state.isWifiConnected && !state.isAdbConnected,
                    shape = RoundedCornerShape(10.dp),
                ) {
                    if (state.isConnectingAdb) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        when {
                            state.isConnectingAdb -> "Connecting…"
                            state.isAdbConnected -> "ADB Connected"
                            else -> "ADB Connect"
                        }
                    )
                }
            }
        }

        // ── Open features button ────────────────────────────────────────────
        AnimatedVisibility(visible = state.canOpenFeatures) {
            FilledTonalButton(
                onClick = onReady,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.Filled.Check, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Go to Main Screen",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

// ─── Reusable composables ────────────────────────────────────────────────────

@Composable
private fun ConnectionCard(
    title: String,
    icon: ImageVector,
    connected: Boolean,
    connecting: Boolean,
    statusText: String,
    dimmed: Boolean = false,
    content: @Composable () -> Unit,
) {

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Card header row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (connected) GreenOk.copy(alpha = 0.12f)
                            else MaterialTheme.colorScheme.secondaryContainer
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (connected) GreenOk else MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            connected  -> GreenOk
                            connecting -> OrangePending
                            else       -> MaterialTheme.colorScheme.error
                        },
                    )
                }
                // Right-side status dot
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                connected  -> GreenOk
                                connecting -> OrangePending
                                else       -> RedError
                            }
                        )
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = if (dimmed) Modifier.then(Modifier) else Modifier,
            ) {
                content()
            }
        }
    }
}

@Composable
private fun StepBadge(
    step: Int,
    label: String,
    icon: ImageVector,
    done: Boolean,
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    val bgColor = when {
        done   -> GreenOk.copy(alpha = 0.1f)
        active -> OrangePending.copy(alpha = 0.1f)
        else   -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when {
        done   -> GreenOk
        active -> OrangePending
        else   -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = if (done) Icons.Filled.Check else icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(18.dp),
            )
            Column {
                Text(
                    text = "Step $step",
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.7f),
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                )
            }
        }
    }
}
