package com.royalenfield.provisioning.feature.wifi.presentation

import androidx.compose.foundation.background
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
fun WifiScreen(
    viewModel: WifiViewModel
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
        // Current Configuration Card
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
                    Text(
                        text = "Active SoftAP Configuration",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = GreenAccent.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "ACTIVE",
                            color = GreenAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InfoTile(
                        modifier = Modifier.weight(1f),
                        label = "SSID",
                        value = uiState.currentSsid
                    )
                    InfoTile(
                        modifier = Modifier.weight(1f),
                        label = "Security",
                        value = uiState.currentSecurity
                    )
                    InfoTile(
                        modifier = Modifier.weight(1f),
                        label = "Frequency",
                        value = uiState.currentBand
                    )
                }
            }
        }

        // Edit SoftAP Form Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Update SoftAP Credentials",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Modifies /data/misc/apexdata/com.android.wifi/WifiConfigStoreSoftAp.xml",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = uiState.newSsidInput,
                    onValueChange = { viewModel.onSsidChanged(it) },
                    label = { Text("New SSID (RE_XXXX_XXXXXX)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = uiState.validationError != null,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BlueAccent,
                        unfocusedBorderColor = DarkSurfaceVariant,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                if (uiState.validationError != null) {
                    Text(
                        text = uiState.validationError ?: "",
                        color = RedPrimary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = uiState.newPassphraseInput,
                    onValueChange = { viewModel.onPassphraseChanged(it) },
                    label = { Text("New Passphrase") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BlueAccent,
                        unfocusedBorderColor = DarkSurfaceVariant,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (uiState.isUpdating) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { uiState.progressPercent / 100f },
                            modifier = Modifier.fillMaxWidth(),
                            color = BlueAccent,
                            trackColor = DarkSurfaceVariant
                        )
                        Text(
                            text = uiState.currentStep ?: "Executing ADB workflow...",
                            color = BlueAccent,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                } else {
                    Button(
                        onClick = { viewModel.executeWorkflow() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState.validationError == null,
                        colors = ButtonDefaults.buttonColors(containerColor = BlueAccent),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Sync, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Apply & Sync via ADB Workflow")
                    }
                }

                if (uiState.updateSuccess) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = GreenAccent.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = GreenAccent)
                            Text("SoftAP XML successfully written and verified on vehicle partition.", color = GreenAccent, fontSize = 12.sp)
                        }
                    }
                }

                if (uiState.errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Error: ${uiState.errorMessage}",
                        color = RedPrimary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Raw XML Inspector
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
                    Text(
                        text = "SoftAP XML Inspector",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    IconButton(onClick = { viewModel.loadRawXml() }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkBackground)
                        .padding(12.dp)
                ) {
                    Text(
                        text = uiState.rawXmlContent,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Audit Trail History
        if (uiState.auditLogs.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "SoftAP Update Audit History",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    uiState.auditLogs.take(5).forEach { log ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${log.oldSsid} → ${log.newSsid}",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = log.status,
                                    color = if (log.status == "SUCCESS") GreenAccent else RedPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "${log.timestamp} • ${log.operator}",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                            Divider(modifier = Modifier.padding(top = 6.dp), color = DarkSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoTile(
    modifier: Modifier = Modifier,
    label: String,
    value: String
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = DarkSurfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(text = label, fontSize = 10.sp, color = TextSecondary)
            Text(text = value, fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
        }
    }
}
