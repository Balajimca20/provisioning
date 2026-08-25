package com.royalenfield.provisioning.feature.ota.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.royalenfield.provisioning.core.theme.*

@Composable
fun OtaScreen(
    viewModel: OtaViewModel
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
        // Vehicle Cluster Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Installed Vehicle Firmware",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = uiState.currentInstalledVersion,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = BlueAccent.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "A/B DUAL SLOT",
                        color = BlueAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // Available OTA Packages List
        Text(
            text = "AVAILABLE FIRMWARE PACKAGES",
            style = MaterialTheme.typography.labelMedium,
            color = TextMuted,
            letterSpacing = 1.sp
        )

        uiState.availablePackages.forEach { pkg ->
            val isSelected = uiState.selectedPackage?.id == pkg.id
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.onSelectPackage(pkg) }
                    .border(
                        width = if (isSelected) 1.5.dp else 0.dp,
                        color = if (isSelected) AmberAccent else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) DarkSurfaceVariant else DarkSurface
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = pkg.vehicleModel,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = AmberAccent.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = pkg.sizeDisplay,
                                color = AmberAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Build: ${pkg.targetVersion}",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = pkg.notes,
                        fontSize = 12.sp,
                        color = TextMuted,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Pipeline Action Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Deployment Pipeline Control",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (uiState.pipelineStage != "IDLE" && uiState.pipelineStage != "COMPLETE" && uiState.pipelineStage != "AWAITING_REBOOT") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LinearProgressIndicator(
                            progress = { uiState.progressPercent / 100f },
                            modifier = Modifier.fillMaxWidth(),
                            color = AmberAccent,
                            trackColor = DarkSurfaceVariant
                        )
                        Text(
                            text = uiState.stageStatusText,
                            color = AmberAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else if (uiState.pipelineStage == "AWAITING_REBOOT") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = GreenAccent.copy(alpha = 0.15f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = GreenAccent)
                                Text("A/B partition flashed! Operator consent required to reboot vehicle.", color = GreenAccent, fontSize = 12.sp)
                            }
                        }

                        Button(
                            onClick = { viewModel.confirmReboot() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = AmberAccent),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PowerSettingsNew, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reboot Vehicle Cluster & Switch Slot", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Button(
                        onClick = { viewModel.startOtaPipeline() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState.selectedPackage != null,
                        colors = ButtonDefaults.buttonColors(containerColor = AmberAccent),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Deploy ${uiState.selectedPackage?.targetVersion ?: "Firmware"}", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }

                if (uiState.errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Deployment Error: ${uiState.errorMessage}",
                        color = RedPrimary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Live Pipeline Logs
        if (uiState.terminalLogs.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Pipeline Execution Stream",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkBackground)
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            uiState.terminalLogs.forEach { log ->
                                Text(
                                    text = log,
                                    color = if (log.contains("ERROR")) RedPrimary else if (log.contains("SUCCESS")) GreenAccent else TextSecondary,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
