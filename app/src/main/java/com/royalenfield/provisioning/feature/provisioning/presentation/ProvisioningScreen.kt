package com.royalenfield.provisioning.feature.provisioning.presentation

// ProvisioningScreen.kt
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.royalenfield.provisioning.core.theme.FfMechanicTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import java.io.File
import java.io.FileOutputStream

// --- Color Palette ---
val DarkBackground = Color(0xFF141416)
val CardBackground = Color(0xFF1B1B1E)
val InnerBoxBackground = Color(0xFF111113)
val BorderGray = Color(0xFF333338)
val AccentAmber = Color(0xFFFFB300)
val AccentPurple = Color(0xFF6A1B9A)
val VariantCyan = Color(0xFF00E5FF)
val DisabledPurple = Color(0xFF381554)
val PurpleButtonText = Color(0xFFE1BEE7)
val AbortRedBorder = Color(0xFFD32F2F)
val AbortRedBackground = Color(0xFF2C1214)
val AbortRedText = Color(0xFFFF5252)
val TextGray = Color(0xFF8E8E93)


@Preview(showBackground = true)
@Composable
private fun ProvisioningScreenPreview() {
    FfMechanicTheme {
        ProvisioningScreen(
            vinNumber = "",
            selectedVariant = VehicleVariant.FLYING_FLEA_C6,
            selectedRegion = Region.EU_FRANCE,
            provisioningStatus = ProvisioningStatus.Idle,
            provisioningLogs = emptyList(),
            progress = 10f,
            onChangeVinNumber = {},
            onSelectRegion = {},
            onSelectVariant = {},
            onStartProvisioning = {_,_->},
            onStopProvisioning = {},
            onAddLogs = {},
            rebootConvernDialog = false,
            onSendRebootConsent = {}
        )
    }
}

@Composable
fun ProvisioningRoute(
    provisioningViewModel: ProvisioningViewModel = koinViewModel()
) {
    val provisioningStatus = provisioningViewModel.status.collectAsStateWithLifecycle()
    val provisioningLogs = provisioningViewModel.logs.collectAsStateWithLifecycle()
    val provisioningUiState = provisioningViewModel.provisioningUiState.collectAsStateWithLifecycle()
    
    val rebootConvernDialog = provisioningViewModel.requestAdbDialog.collectAsStateWithLifecycle(false)

    ProvisioningScreen(
        vinNumber = provisioningUiState.value.vinNumber,
        selectedVariant = provisioningUiState.value.selectedVariant,
        selectedRegion = provisioningUiState.value.selectedRegion,
        provisioningStatus = provisioningStatus.value,
        provisioningLogs = provisioningLogs.value,
        progress = provisioningUiState.value.progress,

        onChangeVinNumber = {
            provisioningViewModel.updateVinNumber(it)
        },
        onSelectRegion = {
            provisioningViewModel.onSelectRegion(it)
        },
        onSelectVariant = {
            provisioningViewModel.onSelectVariant(it)
        },
        onStartProvisioning = { ip,payloadFiles ->
            provisioningViewModel.startProvisioning(ip = ip,payloadFiles)
        },
        onStopProvisioning = {
            provisioningViewModel.stopProvisioning()
        },
        onAddLogs = {
            provisioningViewModel.onPostLog(it)
        },
        rebootConvernDialog = rebootConvernDialog.value,
        onSendRebootConsent = {
            provisioningViewModel.onSendRebootConsent(it)
        }
    )
}

@Composable
fun ProvisioningScreen(
    vinNumber: String,
    selectedVariant: VehicleVariant,
    selectedRegion: Region,
    provisioningStatus: ProvisioningStatus,
    provisioningLogs: List<String>,
    progress: Float,

    onChangeVinNumber: (String) -> Unit,
    onSelectRegion: (Region) -> Unit,
    onSelectVariant: (VehicleVariant) -> Unit,
    onStartProvisioning: (String, List<File>) -> Unit,
    onStopProvisioning: () -> Unit,
    onAddLogs: (String) -> Unit,
    rebootConvernDialog: Boolean,
    onSendRebootConsent:(Boolean)-> Unit
) {
    var variantExpanded by remember { mutableStateOf(false) }
    var regionExpanded by remember { mutableStateOf(false) }

    var filePayloadList by remember { mutableStateOf<List<File>>(emptyList()) }

    val coroutineScope = rememberCoroutineScope()

    val context = LocalContext.current

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                filePayloadList = processSelectedFolder(context, uri)
                onAddLogs("[INFO] Selected folder: $uri")
            }
        }
    }

    var showRebootDialog by remember { mutableStateOf(false) }

    LaunchedEffect(
        key1 = rebootConvernDialog,
    ) {
        showRebootDialog = rebootConvernDialog
    }


    if (showRebootDialog){
        Dialog(
            onDismissRequest = {}
        ) {
            Card(
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        "⚠️ Prompting operator user for hardware reboot consent...",
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                showRebootDialog = false
                                onSendRebootConsent(false)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(8.dp)
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                showRebootDialog = false
                                onSendRebootConsent(true)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(8.dp)
                        ) {
                            Text("Ok")
                        }
                    }
                }
            }
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // --- Added UI: VIN, Variant, Region ---
        Column (
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // VIN Input
            ConfigInputItem(
                label = "VIN:",
                labelColor = Color(0xFFBA68C8),
                modifier = Modifier
            ) {
                BasicTextField(
                    value = vinNumber,
                    onValueChange = onChangeVinNumber,
                    textStyle = TextStyle(
                        color = Color(0xFFBA68C8),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    cursorBrush = SolidColor(Color.White),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { innerTextField ->
                        Box(
                            contentAlignment = Alignment.CenterStart,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (vinNumber.isEmpty()) {
                                Text("Enter Vin Number", color = Color(0xFF4A148C), fontSize = 12.sp)
                            }
                            innerTextField()
                        }
                    }
                )
            }

            // Variant Selection
            ConfigInputItem(
                label = "Variant:",
                labelColor = VariantCyan,
                modifier = Modifier,
                onClick = { variantExpanded = true }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = selectedVariant.description,
                        color = VariantCyan,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = VariantCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }
                DropdownMenu(
                    expanded = variantExpanded,
                    onDismissRequest = { variantExpanded = false },
                    modifier = Modifier
                        .background(CardBackground)
                        .border(1.dp, BorderGray)
                ) {
                    VehicleVariant.entries.forEach { variant ->
                        DropdownMenuItem(
                            text = { Text(variant.description, color = VariantCyan, fontSize = 12.sp) },
                            onClick = {
                                onSelectVariant(variant)
                                variantExpanded = false
                            }
                        )
                    }
                }
            }

            // Region Selection
            ConfigInputItem(
                label = "Region:",
                labelColor = AccentAmber,
                modifier = Modifier,
                onClick = { regionExpanded = true }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${selectedRegion.regionName} (${selectedRegion.country})",
                        color = AccentAmber,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = AccentAmber,
                        modifier = Modifier.size(16.dp)
                    )
                }
                DropdownMenu(
                    expanded = regionExpanded,
                    onDismissRequest = { regionExpanded = false },
                    modifier = Modifier
                        .background(CardBackground)
                        .border(1.dp, BorderGray)
                ) {
                    Region.entries.forEach { region ->
                        DropdownMenuItem(
                            text = { Text("${selectedRegion.regionName} (${selectedRegion.country})", color = AccentAmber, fontSize = 12.sp) },
                            onClick = {
                                onSelectRegion(region)
                                regionExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // --- Section 1: Folder Payload Box ---
        LabeledGroupCard(
            title = "⚠️ Folder Payload → /mnt/vendor/persist/c2c [REQUIRED]",
            titleColor = AccentAmber,
            modifier = Modifier.weight(1f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // Folder Content Preview Display
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(InnerBoxBackground)
                        .border(1.dp, BorderGray, RoundedCornerShape(6.dp))
                        .padding(8.dp)
                ) {
                    if (filePayloadList.isEmpty()) {
                        Text(
                            text = "No payload folder loaded...",
                            color = TextGray,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                        ) {
                            filePayloadList.forEach {
                                Text(
                                    text = it.name,
                                    color = Color.Green,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Text(
                                text = "Loaded: ${filePayloadList.size} files",
                                color = Color.Green,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action Controls Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Select Folder Button
                    Button(
                        onClick = {
                            folderPickerLauncher.launch(null)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("📁 SELECT FOLDER", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    // Clear Button
                    Button(
                        onClick = {
                            filePayloadList = emptyList()
                            onAddLogs("[INFO] Payload selection cleared.")
                        //    logs = logs + "[INFO] Payload selection cleared."
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C30)),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("🗑️ CLEAR", fontSize = 11.sp, color = TextGray)
                    }

                    // Status Indicator
                    Text(
                        text = if (filePayloadList.isEmpty()) "⚠️ No folder selected" else "✓ Ready",
                        color = if (filePayloadList.isEmpty()) AccentAmber else Color.Green,
                        fontSize = 11.sp,
                        fontStyle = FontStyle.Italic,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // --- Section 2: Execution Logs Box ---
        LabeledGroupCard(
            title = "Execution Logs",
            titleColor = Color.White,
            modifier = Modifier.weight(1.2f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black)
                    .border(1.dp, BorderGray, RoundedCornerShape(6.dp))
                    .padding(8.dp)
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(provisioningLogs) { log ->
                        Text(
                            text = log,
                            color = Color(0xFF00FF66),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // --- Section 3: Progress Bar Component ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .background(CardBackground, RoundedCornerShape(4.dp))
                .border(1.dp, BorderGray, RoundedCornerShape(4.dp))
                .padding(3.dp)
        ) {
            when(provisioningStatus){
                is ProvisioningStatus.Running -> {
                    val progressFraction = provisioningStatus.progress / 100f
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(2.dp)),
                        color = AccentPurple,
                        trackColor = Color.Transparent,
                    )
                }
                is ProvisioningStatus.Success -> {
                    LinearProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(2.dp)),
                        color = AccentPurple,
                        trackColor = Color.Transparent,
                    )
                }

                else -> {}
            }

        }

        // --- Section 4: Action Buttons Row ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Main Action Button
            Button(
                onClick = {
                    if (filePayloadList.isNotEmpty()) {
                        onStartProvisioning("",filePayloadList)
                        onAddLogs("[INFO] Starting provisioning process...")
                    }
                },
                enabled = vinNumber.isNotBlank() &&
                        filePayloadList.isNotEmpty() &&
                        provisioningStatus !is ProvisioningStatus.Running,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentPurple,
                    contentColor = PurpleButtonText,
                    disabledContainerColor = DisabledPurple,
                    disabledContentColor = Color.Gray
                ),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text(
                    text = if (filePayloadList.isEmpty()) "SELECT PAYLOAD FOLDER TO CONTINUE" else "START EXECUTION",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Abort Button
            OutlinedButton(
                onClick = {
                    onStopProvisioning()
                    onAddLogs("[ABORT] Process terminated by user.")
                },
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = AbortRedBackground,
                    contentColor = AbortRedText
                ),
                border = BorderStroke(1.dp, AbortRedBorder),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.height(48.dp),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Text("⛔ STOP / ABORT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ConfigInputItem(
    label: String,
    labelColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Row(
        modifier = modifier
            .height(IntrinsicSize.Min)
            .border(1.dp, BorderGray.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            .background(Color(0xFF0D0D0F))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .border(1.dp, BorderGray.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = labelColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            content()
        }
    }
}

// --- Custom Component for Fieldset-Style Labeled Borders ---
@Composable
fun LabeledGroupCard(
    title: String,
    titleColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier.fillMaxWidth()) {
        // Outer Outlined Box
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 8.dp)
                .background(CardBackground, RoundedCornerShape(6.dp))
                .border(1.dp, BorderGray, RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 12.dp)
        ) {
            content()
        }

        // Header Label Overlay
        Surface(
            color = CardBackground,
            modifier = Modifier
                .padding(start = 12.dp)
                .align(Alignment.TopStart)
        ) {
            Text(
                text = " $title ",
                color = titleColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Recursive worker that copies stream contents into localized app-specific files.
 */
private fun traverseAndCopyPayload(
    context: Context,
    directory: DocumentFile,
    cacheDir: File,
    payloadList: MutableList<File>
) {
    val items = directory.listFiles()
    for (item in items) {
        if (item.isFile) {
            val fileName = item.name ?: "temp_file_${System.currentTimeMillis()}"
            val localFile = File(cacheDir, fileName)

            try {
                // Read from Scoped Storage URI and write out directly to a local File
                context.contentResolver.openInputStream(item.uri)?.use { inputStream ->
                    FileOutputStream(localFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                // Successfully localized; add it to the List<File> payload array
                payloadList.add(localFile)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else if (item.isDirectory) {
            // Recurse deeper into sub-directories
            traverseAndCopyPayload(context, item, cacheDir, payloadList)
        }
    }
}

/**
 * Reads the directory structure and converts items into a List<File> payload.
 */
suspend fun processSelectedFolder(context: Context, folderUri: Uri): List<File> = withContext(Dispatchers.IO) {
    val payloadList = mutableListOf<File>()
    val rootFolder = DocumentFile.fromTreeUri(context, folderUri)

    if (rootFolder != null && rootFolder.isDirectory) {
        // Create a local app cache folder to hold the payload references safely
        val localCacheDirectory = File(context.cacheDir, "folder_payloads").apply {
            if (!exists()) mkdirs()
        }

        // Loop through all files in the folder automatically
        val items = rootFolder.listFiles()
        for (item in items) {
            if (item.isFile) {
                val fileName = item.name ?: "file_${System.currentTimeMillis()}"
                val localFile = File(localCacheDirectory, fileName)

                try {
                    // Pull the stream out of scoped storage into a genuine java.io.File
                    context.contentResolver.openInputStream(item.uri)?.use { input ->
                        FileOutputStream(localFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    payloadList.add(localFile)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    return@withContext payloadList
}