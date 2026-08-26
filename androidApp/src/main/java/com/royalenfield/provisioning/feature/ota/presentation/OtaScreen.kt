package com.royalenfield.provisioning.feature.ota.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.runtime.*
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
    val terminalScrollState = rememberScrollState()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        viewModel.onLocalFileSelected(uri)
    }

    // Auto-scroll terminal to bottom when new telemetry logs arrive
    LaunchedEffect(uiState.terminalLogs.size) {
        terminalScrollState.animateScrollTo(terminalScrollState.maxValue)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Target Info Card ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Active Firmware", fontSize = 11.sp, color = TextMuted)
                    Text(
                        text = uiState.currentInstalledVersion,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = BlueAccent.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, BlueAccent.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "A/B REDUNDANT",
                        color = BlueAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // --- Deployment Control Card (Real-time Telemetry Dashboard) ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(12.dp),
            border = if (uiState.pipelineStage != "IDLE" && uiState.pipelineStage != "COMPLETE") 
                BorderStroke(1.dp, AmberAccent.copy(alpha = 0.5f)) else null
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "DEPLOYMENT PIPELINE",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (uiState.pipelineStage == "IDLE" || uiState.pipelineStage == "COMPLETE" || uiState.pipelineStage == "FAILED") {
                    Button(
                        onClick = { viewModel.startOtaPipeline() },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        enabled = uiState.selectedPackage != null,
                        colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("INITIATE FIRMWARE FLASH", fontWeight = FontWeight.Bold)
                    }
                } else if (uiState.pipelineStage == "AWAITING_REBOOT") {
                    Button(
                        onClick = { viewModel.confirmReboot() },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AmberAccent),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PowerSettingsNew, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("REBOOT & ACTIVATE SLOT", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                } else {
                    // Active Telemetry Dashboard
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        LinearProgressIndicator(
                            progress = { uiState.progressPercent / 100f },
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = AmberAccent,
                            trackColor = DarkSurfaceVariant,
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = uiState.stageStatusText.uppercase(),
                                color = AmberAccent,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            
                            // Real-time bitstream data
                            Text(
                                text = when (uiState.pipelineStage) {
                                    "DOWNLOADING" -> "${uiState.currentMb}MB / ${uiState.totalMb}MB"
                                    "PUSHING" -> "${uiState.transferSpeedMbps} MB/s"
                                    "FLASHING" -> "Partition: ${uiState.activePartition}"
                                    else -> "STAGING..."
                                },
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                if (uiState.errorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "CRITICAL_ERROR: ${uiState.errorMessage}",
                        color = RedPrimary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // --- CLI / STDOUT Stream ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.Black),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, DarkSurfaceVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SYS_STDOUT_STREAM",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 180.dp, max = 400.dp)
                        .verticalScroll(terminalScrollState)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        uiState.terminalLogs.forEach { log ->
                            Text(
                                text = log,
                                color = when {
                                    log.contains("[FATAL]") || log.contains("deploy-error") || log.contains("❌") -> RedPrimary
                                    log.contains("[SUCCESS]") || log.contains("OK") || log.contains("PASS") || log.contains("✅") -> GreenAccent
                                    log.contains("[SYS]") || log.contains("[INIT]") || log.contains("⚙️") -> BlueAccent
                                    log.contains("[NET]") || log.contains("[ADB]") || log.contains("🚀") || log.contains("📲") || log.contains("🛠️") || log.contains("🔍") -> AmberAccent
                                    else -> TextSecondary
                                },
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 14.sp
                            )
                        }
                        
                        if (uiState.pipelineStage != "COMPLETE" && uiState.pipelineStage != "FAILED") {
                            BlinkingPrompt()
                        }
                    }
                }
            }
        }
        
        // --- Package Picker ---
        if (uiState.pipelineStage == "IDLE") {
            Text(
                text = "FIRMWARE CATALOG",
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted,
                modifier = Modifier.padding(top = 8.dp)
            )
            
            uiState.availablePackages.forEach { pkg ->
                val isSelected = uiState.selectedPackage?.id == pkg.id
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.onSelectPackage(pkg) }
                        .border(
                            width = if (isSelected) 1.dp else 0.dp,
                            color = if (isSelected) AmberAccent else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) DarkSurfaceVariant else DarkSurface
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = pkg.vehicleModel, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(text = pkg.targetVersion, fontSize = 12.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (pkg.id.startsWith("local")) PurpleAccent.copy(alpha = 0.1f) else AmberAccent.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = pkg.sizeDisplay,
                                color = if (pkg.id.startsWith("local")) PurpleAccent else AmberAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
            
            OutlinedButton(
                onClick = { filePickerLauncher.launch("application/zip") },
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, DarkSurfaceVariant),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(imageVector = Icons.Default.FileUpload, contentDescription = null, tint = TextPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("LOAD LOCAL ARCHIVE (.ZIP)", color = TextPrimary)
            }
        }
    }
}

@Composable
private fun BlinkingPrompt() {
    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes { durationMillis = 1000; 0.7f at 500 },
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorAlpha"
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "re_mechanic@vehicle:~$ ",
            color = GreenAccent,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp
        )
        Box(
            modifier = Modifier
                .width(6.dp)
                .height(12.dp)
                .background(TextPrimary.copy(alpha = alpha))
        )
    }
}
