package com.royalenfield.provisioning.feature.ota.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

// Exact Royal Enfield FF Mech color specifications
private val FFBackground = Color(0xFF0F1115)
private val FFSecondaryBlueGreen = Color(0xFF00D2B4) // .ffSecondaryBlueGreen
private val FFTerminalGreen = Color(0xFF00FF40)     // Color(red: 0, green: 1, blue: 0.25)
private val FFNeutralTwo = Color(0xFF8E929B)        // .ffNeutralTwo
private val FFSurfaceCard = Color(0xFF161920)

@Composable
fun CommandLineOTAView(
    viewModel: CommandLineOTAViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        viewModel.handlePickerResult(uri)
    }

    // Auto-scroll terminal log to bottom on new lines
    LaunchedEffect(uiState.logLines.size) {
        if (uiState.logLines.isNotEmpty()) {
            listState.animateScrollToItem(uiState.logLines.size - 1)
        }
    }

    // --- Reboot Consent Alert Dialog ---
    if (uiState.rebootConsentRequested) {
        AlertDialog(
            onDismissRequest = { viewModel.respondToRebootConsent(false) },
            title = {
                Text(
                    text = "Reboot Required",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = "Command line OTA update done, need a reboot to use the new software. Reboot now?",
                    color = Color.White.copy(alpha = 0.9f)
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.respondToRebootConsent(true) },
                    colors = ButtonDefaults.buttonColors(containerColor = FFSecondaryBlueGreen)
                ) {
                    Text("Yes", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.respondToRebootConsent(false) }
                ) {
                    Text("No", color = Color.White.copy(alpha = 0.7f))
                }
            },
            containerColor = FFSurfaceCard,
            shape = RoundedCornerShape(12.dp)
        )
    }

    // --- File Error Alert ---
    if (uiState.fileError != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissFileError() },
            title = { Text("File Error", fontWeight = FontWeight.Bold, color = Color.White) },
            text = { Text(uiState.fileError ?: "", color = Color.White.copy(alpha = 0.85f)) },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissFileError() },
                    colors = ButtonDefaults.buttonColors(containerColor = FFSecondaryBlueGreen)
                ) {
                    Text("OK", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = FFSurfaceCard,
            shape = RoundedCornerShape(12.dp)
        )
    }

    // --- OTA Result Alert ---
    if (uiState.resultAlertMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissResultAlert() },
            title = { Text("OTA Result", fontWeight = FontWeight.Bold, color = Color.White) },
            text = { Text(uiState.resultAlertMessage ?: "", color = Color.White.copy(alpha = 0.85f)) },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissResultAlert() },
                    colors = ButtonDefaults.buttonColors(containerColor = FFSecondaryBlueGreen)
                ) {
                    Text("OK", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = FFSurfaceCard,
            shape = RoundedCornerShape(12.dp)
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FFBackground)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // MARK: - File Picker Section
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Secondary Button: Select OTA Package (.zip)
                OutlinedButton(
                    onClick = { filePickerLauncher.launch("application/zip") },
                    enabled = !uiState.isRunning,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (!uiState.isRunning) Color.White else Color.White.copy(alpha = 0.4f)
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(
                            if (!uiState.isRunning) FFSecondaryBlueGreen else FFNeutralTwo.copy(alpha = 0.3f)
                        )
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Select OTA Package (.zip)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Verbose Mode Toggle Button
                OutlinedButton(
                    onClick = { viewModel.toggleVerboseMode() },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (uiState.isVerboseMode) FFTerminalGreen else FFNeutralTwo
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(
                            if (uiState.isVerboseMode) FFTerminalGreen.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.15f)
                        )
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (uiState.isVerboseMode) "Verbose: ON" else "Verbose: OFF",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                if (uiState.selectedFileName != null) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = uiState.selectedFileName ?: "",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (uiState.selectedFileSizeDescription != null) {
                            Text(
                                text = uiState.selectedFileSizeDescription ?: "",
                                fontSize = 11.sp,
                                color = FFNeutralTwo
                            )
                        }
                    }
                }
            }

            // Status Text (e.g. WAITING FOR DEVICE & ZIP PACKAGE…, 🛠️ INSTALLING: 45%)
            Text(
                text = uiState.statusText,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = FFSecondaryBlueGreen
            )
        }

        // MARK: - Monospaced Pure Black Terminal Screen
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black)
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(uiState.logLines, key = { it.id }) { line ->
                    Text(
                        text = line.text,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = FFTerminalGreen,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // MARK: - Progress View & Percentage Indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LinearProgressIndicator(
                progress = { uiState.progress.toFloat().coerceIn(0f, 1f) },
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp),
                color = FFSecondaryBlueGreen,
                trackColor = Color.White.copy(alpha = 0.1f),
                strokeCap = StrokeCap.Round
            )
            Text(
                text = "(${ (uiState.progress * 100.0).roundToInt() }%)",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = FFSecondaryBlueGreen,
                modifier = Modifier.widthIn(min = 48.dp)
            )
        }

        // MARK: - Primary Action Button
        val buttonTitle = when {
            uiState.isRunning -> "OTA UPGRADE RUNNING…"
            uiState.selectedFile == null -> "SELECT UPDATE.ZIP TO RUN OTA"
            else -> "START COMMAND LINE OTA UPGRADE"
        }

        val isEnabled = viewModel.canStartPipeline

        Button(
            onClick = { viewModel.startPipeline() },
            enabled = isEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = FFSecondaryBlueGreen,
                disabledContainerColor = FFSecondaryBlueGreen.copy(alpha = 0.25f),
                contentColor = Color.Black,
                disabledContentColor = Color.White.copy(alpha = 0.35f)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            if (uiState.isRunning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = Color.Black,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = buttonTitle,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                letterSpacing = 0.5.sp
            )
        }
    }
}
