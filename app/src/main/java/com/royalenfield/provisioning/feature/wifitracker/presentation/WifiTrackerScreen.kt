package com.royalenfield.provisioning.feature.wifitracker.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.royalenfield.provisioning.feature.wifitracker.data.WifiLogRecord

// Exact UI color palette
private val BgDark = Color(0xFF0D1117)
private val BoxBorderDark = Color(0xFF1F2937)
private val CardBg = Color(0xFF111827)

private val SectionHeaderPurple = Color(0xFFD946EF)
private val SectionHeaderCyan = Color(0xFF06B6D4)
private val SectionHeaderGreen = Color(0xFF10B981)

private val PrimaryBlueButton = Color(0xFF0284C7)
private val PrimaryBlueButtonDisabled = Color(0xFF1E3A5F)

private val CsvExportBtnGreen = Color(0xFF15803D)
private val CsvExcelBtnEmerald = Color(0xFF059669)

private val ConsoleBackground = Color(0xFF050505)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiTrackerScreen(
    viewModel: WifiTrackerViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val mainScrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
                .verticalScroll(mainScrollState),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. VEHICLE IDENTIFICATION SECTION
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Vehicle Identification",
                    color = SectionHeaderPurple,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(CardBg)
                        .border(1.dp, Color(0xFF6B21A8), RoundedCornerShape(4.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "VIN Scanner Input:",
                        color = SectionHeaderPurple,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 12.dp)
                    )

                    BasicTextField(
                        value = uiState.vinInput,
                        onValueChange = { viewModel.onVinChanged(it) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !uiState.isChangingPassword,
                        textStyle = TextStyle(
                            color = Color(0xFFE879F9),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        cursorBrush = SolidColor(SectionHeaderPurple),
                        decorationBox = { innerTextField ->
                            if (uiState.vinInput.isEmpty()) {
                                Text(
                                    text = "Enter or scan 17-character VIN",
                                    color = Color(0xFFA855F7).copy(alpha = 0.6f),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp
                                )
                            }
                            innerTextField()
                        }
                    )
                }

                if (uiState.vinError != null) {
                    Text(
                        text = uiState.vinError ?: "",
                        color = Color(0xFFEF4444),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                }
            }

            // 2. PASSWORD GENERATION SECTION
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .border(1.dp, BoxBorderDark, RoundedCornerShape(4.dp))
                    .padding(10.dp)
            ) {
                Text(
                    text = "Password Generation",
                    color = SectionHeaderCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Generate New Password Button
                    Button(
                        onClick = { viewModel.generateNewPassword() },
                        enabled = !uiState.isChangingPassword,
                        modifier = Modifier
                            .weight(1.1f)
                            .height(42.dp),
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0F243A),
                            disabledContainerColor = Color(0xFF091420)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0284C7)),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text("🎲 ", fontSize = 14.sp)
                            Text(
                                text = "Generate New Password",
                                color = if (!uiState.isChangingPassword) Color(0xFF38BDF8) else Color(0xFF64748B),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Display Box for Generated Password
                    Box(
                        modifier = Modifier
                            .weight(2f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF0B132B))
                            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(4.dp))
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (uiState.generatedPassword.isNotEmpty()) {
                                uiState.generatedPassword
                            } else {
                                "Click to generate (8 chars)"
                            },
                            color = if (uiState.generatedPassword.isNotEmpty()) {
                                Color(0xFF4ADE80)
                            } else {
                                Color(0xFF10B981).copy(alpha = 0.7f)
                            },
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            letterSpacing = if (uiState.generatedPassword.isNotEmpty()) 2.sp else 0.sp
                        )
                    }
                }
            }

            // 3. MAIN ACTION BUTTON: CHANGE THE WIFI PASSWORD
            Button(
                onClick = { viewModel.changeWifiPassword() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryBlueButton,
                    disabledContainerColor = PrimaryBlueButtonDisabled
                ),
                enabled = !uiState.isChangingPassword
            ) {
                if (uiState.isChangingPassword) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Updating Wi-Fi via ADB (${uiState.progressPercent}%)...",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("📶 ", fontSize = 14.sp)
                        Text(
                            text = "Change the WIFI password",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // 4. TRANSACTION LOGS CSV EXPORT SECTION
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .border(1.dp, BoxBorderDark, RoundedCornerShape(4.dp))
                    .padding(10.dp)
            ) {
                Text(
                    text = "Transaction Logs  CSV Export",
                    color = SectionHeaderCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // View Transaction Log Table Button
                    Button(
                        onClick = { viewModel.openLogBottomSheet() },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F243A)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0284C7)),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📊 ", fontSize = 12.sp)
                            Text(
                                text = "View Transaction Log Table",
                                color = Color(0xFF38BDF8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Export / Save CSV As... Button
                    Button(
                        onClick = { viewModel.exportCsv(context, openDirectly = false) },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CsvExportBtnGreen),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("💾 ", fontSize = 12.sp)
                            Text(
                                text = "Export / Save CSV As...",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Open CSV in Excel Button
                    Button(
                        onClick = { viewModel.exportCsv(context, openDirectly = true) },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CsvExcelBtnEmerald),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🟢 ", fontSize = 12.sp)
                            Text(
                                text = "Open CSV in Excel",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 5. EXECUTION LOG CONSOLE SECTION (Matching Python HTML colored log tray)
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Execution Log Console",
                    color = SectionHeaderGreen,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(ConsoleBackground)
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(4.dp))
                        .padding(10.dp)
                ) {
                    val consoleScroll = rememberScrollState()
                    LaunchedEffect(uiState.consoleLogs.size) {
                        consoleScroll.animateScrollTo(consoleScroll.maxValue)
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(consoleScroll),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        uiState.consoleLogs.forEach { logItem ->
                            val parsedColor = try {
                                Color(android.graphics.Color.parseColor(logItem.colorHex))
                            } catch (e: Exception) {
                                Color(0xFFD4D4D4)
                            }
                            Row(verticalAlignment = Alignment.Top) {
                                Text(
                                    text = "[${logItem.timestamp}] ",
                                    color = Color(0xFF6A9955), // Python timestamp green
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp
                                )
                                Text(
                                    text = logItem.message,
                                    color = parsedColor,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // SUCCESS ALERT DIALOG (QMessageBox.information equivalent)
        if (uiState.showSuccessDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissSuccessDialog() },
                containerColor = Color(0xFF0F172A),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("✅ ", fontSize = 18.sp)
                        Text(
                            text = "Success",
                            color = Color(0xFF4ADE80),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                },
                text = {
                    Text(
                        text = uiState.dialogMessage.ifEmpty { "Wi-Fi Password updated successfully!\nTarget device rebooting..." },
                        color = Color(0xFFE2E8F0),
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.dismissSuccessDialog() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                    ) {
                        Text("OK", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // ERROR / WARNING ALERT DIALOG (QMessageBox.warning / QMessageBox.critical equivalent)
        if (uiState.showErrorDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissErrorDialog() },
                containerColor = Color(0xFF1E1010),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⚠️ ", fontSize = 18.sp)
                        Text(
                            text = "Notification",
                            color = Color(0xFFEF4444),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                },
                text = {
                    Text(
                        text = uiState.dialogMessage,
                        color = Color(0xFFFCA5A5),
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.dismissErrorDialog() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                    ) {
                        Text("OK", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // FULL-SCREEN TRANSACTION LOG VIEWER (Renders above native BottomBar)
        if (uiState.isLogBottomSheetOpen) {
            Dialog(
                onDismissRequest = { viewModel.closeLogBottomSheet() },
                properties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    decorFitsSystemWindows = false
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f))
                        .systemBarsPadding()
                ) {
                    WifiTransactionLogDialogContent(
                        uiState = uiState,
                        onSearchQueryChanged = { viewModel.onLogSearchQueryChanged(it) },
                        onRefresh = { viewModel.loadAuditLogs() },
                        onExportCsv = { viewModel.exportCsv(context, openDirectly = false) },
                        onOpenExcel = { viewModel.exportCsv(context, openDirectly = true) },
                        onClose = { viewModel.closeLogBottomSheet() }
                    )
                }
            }
        }
    }
}

/**
 * Wi-Fi Password Transaction Log Viewer Bottom Sheet Dialog
 */
@Composable
private fun WifiTransactionLogDialogContent(
    uiState: WifiTrackerUiState,
    onSearchQueryChanged: (String) -> Unit,
    onRefresh: () -> Unit,
    onExportCsv: () -> Unit,
    onOpenExcel: () -> Unit,
    onClose: () -> Unit
) {
    val filteredLogs = remember(uiState.transactionLogs, uiState.logSearchQuery) {
        if (uiState.logSearchQuery.isEmpty()) {
            uiState.transactionLogs
        } else {
            val q = uiState.logSearchQuery.lowercase()
            uiState.transactionLogs.filter {
                it.vin.lowercase().contains(q) ||
                it.wifiSsid.lowercase().contains(q) ||
                it.wifiMacId.lowercase().contains(q) ||
                it.newWifiPassword.lowercase().contains(q)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1222))
            .padding(12.dp)
    ) {
        // Dialog Title Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("📊 ", fontSize = 14.sp)
                Text(
                    text = "Wi-Fi Password Change History",
                    color = Color(0xFF06B6D4),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black
                )

                // Close Icon View on Top-Right Corner
                IconButton(onClick = onClose,) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Search Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF08101E))
                    .border(1.dp, Color(0xFF1E3A8A), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🔍 Search: ", color = Color(0xFF60A5FA), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                BasicTextField(
                    value = uiState.logSearchQuery,
                    onValueChange = onSearchQueryChanged,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = TextStyle(
                        color = Color(0xFF93C5FD),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    ),
                    cursorBrush = SolidColor(Color(0xFF60A5FA)),
                    decorationBox = { innerTextField ->
                        if (uiState.logSearchQuery.isEmpty()) {
                            Text(
                                text = "Filter by VIN, SSID, or MAC...",
                                color = Color(0xFF3B82F6).copy(alpha = 0.5f),
                                fontSize = 11.sp
                            )
                        }
                        innerTextField()
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Data Table Container
        val horizontalScrollState = rememberScrollState()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(4.dp))
                .border(1.dp, Color(0xFF1E3A8A), RoundedCornerShape(4.dp))
                .background(Color(0xFF070B18))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(horizontalScrollState)
            ) {
                // Table Header Row
                Row(
                    modifier = Modifier
                        .width(760.dp)
                        .background(Color(0xFF0B1736))
                        .border(1.dp, Color(0xFF1E3A8A))
                        .padding(vertical = 8.dp)
                ) {
                    TableHeaderCell(text = "VIN", width = 160.dp)
                    TableHeaderCell(text = "Wi-Fi SSID", width = 150.dp)
                    TableHeaderCell(text = "New Wi-Fi Password", width = 160.dp)
                    TableHeaderCell(text = "Wi-Fi MAC ID", width = 140.dp)
                    TableHeaderCell(text = "Timestamp", width = 150.dp)
                }

                // Table Rows / Empty State
                if (filteredLogs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .width(760.dp)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (uiState.transactionLogs.isEmpty()) {
                                "No password transactions recorded yet. Run a Wi-Fi password change."
                            } else {
                                "No matching records found for '${uiState.logSearchQuery}'."
                            },
                            color = Color(0xFF475569),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .width(760.dp)
                            .fillMaxHeight()
                    ) {
                        itemsIndexed(filteredLogs) { index, record ->
                            val rowBg = if (index % 2 == 0) Color(0xFF070B18) else Color(0xFF0A1024)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(rowBg)
                                    .border(0.5.dp, Color(0xFF1E293B).copy(alpha = 0.5f))
                                    .padding(vertical = 7.dp)
                            ) {
                                TableDataCell(text = record.vin.ifEmpty { "-" }, width = 160.dp, color = Color(0xFF38BDF8))
                                TableDataCell(text = record.wifiSsid.ifEmpty { "-" }, width = 150.dp)
                                TableDataCell(text = record.newWifiPassword.ifEmpty { "-" }, width = 160.dp, color = Color(0xFF4ADE80))
                                TableDataCell(text = record.wifiMacId.ifEmpty { "-" }, width = 140.dp)
                                TableDataCell(text = record.timestamp.ifEmpty { "-" }, width = 150.dp, color = Color(0xFF94A3B8))
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Dialog Footer Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Refresh Log Button
            Button(
                onClick = onRefresh,
                modifier = Modifier.height(34.dp),
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0369A1)),
                contentPadding = PaddingValues(horizontal = 10.dp)
            ) {
                Text("🔄 Refresh Log", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            // Export / Save CSV As... Button
            Button(
                onClick = onExportCsv,
                modifier = Modifier.height(34.dp),
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)),
                contentPadding = PaddingValues(horizontal = 10.dp)
            ) {
                Text("💾 Export / Save CSV As...", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            // Open in Excel Button
            Button(
                onClick = onOpenExcel,
                modifier = Modifier.height(34.dp),
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                contentPadding = PaddingValues(horizontal = 10.dp)
            ) {
                Text("🟢 Open in Excel", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun TableHeaderCell(text: String, width: androidx.compose.ui.unit.Dp) {
    Text(
        text = text,
        color = Color(0xFF38BDF8),
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.width(width)
    )
}

@Composable
private fun TableDataCell(
    text: String,
    width: androidx.compose.ui.unit.Dp,
    color: Color = Color(0xFFE2E8F0)
) {
    Text(
        text = text,
        color = color,
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.width(width)
    )
}
