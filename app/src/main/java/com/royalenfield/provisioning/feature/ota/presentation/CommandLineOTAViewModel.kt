package com.royalenfield.provisioning.feature.ota.presentation

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.royalenfield.provisioning.core.adb.AdbClient
import com.royalenfield.provisioning.core.adb.AdbResult
import com.royalenfield.provisioning.feature.ota.domain.OTAPayloadInfo
import com.royalenfield.provisioning.feature.ota.domain.OTAZipInspector
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.regex.Pattern
import java.util.zip.CRC32

data class OTALogLine(
    val id: String = UUID.randomUUID().toString(),
    val text: String
)

data class CommandLineOtaUiState(
    val selectedFile: File? = null,
    val selectedFileName: String? = null,
    val selectedFileSizeDescription: String? = null,
    val fileError: String? = null,
    val logLines: List<OTALogLine> = emptyList(),
    val progress: Double = 0.0,
    val statusText: String = "WAITING FOR DEVICE & ZIP PACKAGE…",
    val isRunning: Boolean = false,
    val rebootConsentRequested: Boolean = false,
    val resultAlertMessage: String? = null
)

class CommandLineOTAViewModel(
    private val adbClient: AdbClient,
    private val context: Context
) : ViewModel() {

    private val TAG = "CommandLineOTA"
    private val _uiState = MutableStateFlow(CommandLineOtaUiState())
    val uiState: StateFlow<CommandLineOtaUiState> = _uiState.asStateFlow()

    private val remoteOTADirectory = "/data/ota_package"
    private var rebootConsentDeferred: CompletableDeferred<Boolean>? = null

    val canStartPipeline: Boolean
        get() = _uiState.value.selectedFile != null && !_uiState.value.isRunning

    fun handlePickerResult(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(statusText = "⚙️ LOADING FILE…")
                selectUri(uri)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    fileError = "Couldn't read the selected file: ${e.localizedMessage}",
                    statusText = "❌ FILE ERROR"
                )
            }
        }
    }

    fun selectLocalFile(file: File) {
        val sizeDesc = formatByteCount(file.length())
        _uiState.value = _uiState.value.copy(
            selectedFile = file,
            selectedFileName = file.name,
            selectedFileSizeDescription = sizeDesc,
            fileError = null,
            statusText = "READY: ${file.name}"
        )
    }

    private suspend fun selectUri(uri: Uri) = withContext(Dispatchers.IO) {
        // 1. If it's already a file URI, use it directly (common for local path selection)
        if (uri.scheme == "file") {
            val path = uri.path ?: throw IllegalArgumentException("Invalid file path")
            val file = File(path)
            if (file.exists()) {
                Log.d(TAG, "Using file directly: ${file.absolutePath}")
                selectLocalFile(file)
                return@withContext
            }
        }

        // 2. Query original size from metadata first to detect partial copies later
        val expectedSize = getFileSizeMetadata(uri)
        val fileName = getFileName(uri) ?: "update.zip"

        // 3. Prepare destination in cacheDir (Content URIs must be copied to a local File for RandomAccess)
        val destination = File(context.cacheDir, "ota_staging_${System.currentTimeMillis()}.zip")

        // Cleanup old staging files in cache
        context.cacheDir.listFiles { f -> f.name.startsWith("ota_staging_") }?.forEach { it.delete() }

        // 4. Perform copy from the content resolver stream to the physical file
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destination).use { output ->
                input.copyTo(output)
                output.flush()
                try {
                    output.fd.sync() // Force write to physical storage to ensure accurate .length()
                } catch (e: Exception) {
                    Log.w(TAG, "Storage sync failed: ${e.message}")
                }
            }
        } ?: throw IllegalStateException("Unable to open stream for $uri")

        // 5. Verify the copy integrity
        val finalSize = destination.length()
        if (expectedSize > 0 && finalSize != expectedSize) {
            destination.delete()
            throw IllegalStateException("Size mismatch! Source reported $expectedSize but only $finalSize was copied. Device storage may be full.")
        }

        _uiState.value = _uiState.value.copy(
            selectedFile = destination,
            selectedFileName = fileName,
            selectedFileSizeDescription = formatByteCount(finalSize),
            fileError = null,
            statusText = "READY: $fileName"
        )
    }

    private fun getFileSizeMetadata(uri: Uri): Long {
        return try {
            if (uri.scheme == "content") {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                        if (sizeIndex != -1) return cursor.getLong(sizeIndex)
                    }
                }
            } else if (uri.scheme == "file") {
                return uri.path?.let { File(it).length() } ?: 0L
            }
            -1L
        } catch (e: Exception) {
            -1L
        }
    }

    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val idx = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) name = it.getString(idx)
                }
            }
        }
        return name ?: uri.path?.substringAfterLast('/')
    }

    private fun formatByteCount(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 * 1024 -> String.format(Locale.US, "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
            bytes >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
            else -> "$bytes bytes"
        }
    }

    private fun log(text: String) {
        _uiState.value = _uiState.value.copy(
            logLines = _uiState.value.logLines + OTALogLine(text = text)
        )
    }

    private suspend fun convertFileToChecksum(path: String): Long {
        return withContext(Dispatchers.IO) {
            val crc = CRC32()
            val file = File(path)
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(64 * 1024)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    crc.update(buffer, 0, bytesRead)
                }
            }
            crc.value
        }
    }

    private fun handleEngineLine(line: String) {
        log("📲 $line")

        // Progress parsing logic
        val dlMatch = Pattern.compile("UPDATE_STATUS_DOWNLOADING\\s*\\(\\d+\\),\\s*([0-9.]+)").matcher(line)
        if (dlMatch.find()) {
            val dlPct = dlMatch.group(1)?.toDoubleOrNull() ?: 0.0
            _uiState.value = _uiState.value.copy(
                progress = 0.55 + (dlPct * 0.25),
                statusText = "🛠️ INSTALLING: ${String.format(Locale.US, "%.1f", dlPct * 100)}%"
            )
            return
        }

        val vfMatch = Pattern.compile("UPDATE_STATUS_VERIFYING\\s*\\(\\d+\\),\\s*([0-9.]+)").matcher(line)
        if (vfMatch.find()) {
            val vfPct = vfMatch.group(1)?.toDoubleOrNull() ?: 0.0
            _uiState.value = _uiState.value.copy(
                progress = 0.80 + (vfPct * 0.15),
                statusText = "🔍 VERIFYING: ${String.format(Locale.US, "%.1f", vfPct * 100)}%"
            )
            return
        }
    }

    fun dismissFileError() {
        _uiState.value = _uiState.value.copy(fileError = null)
    }

    fun dismissResultAlert() {
        _uiState.value = _uiState.value.copy(resultAlertMessage = null)
    }

    // Renamed from onRebootConsent to match the call sites in CommandLineOTAView.kt
    // (viewModel.respondToRebootConsent(...)), which previously referenced a function
    // that didn't exist on this class.
    fun respondToRebootConsent(accepted: Boolean) {
        rebootConsentDeferred?.complete(accepted)
        _uiState.value = _uiState.value.copy(rebootConsentRequested = false)
    }

    // MARK: - Pipeline Execution

    fun startPipeline() {
        val file = _uiState.value.selectedFile ?: return
        if (_uiState.value.isRunning) return

        _uiState.value = _uiState.value.copy(
            isRunning = true,
            logLines = emptyList(),
            progress = 0.0,
            resultAlertMessage = null
        )

        viewModelScope.launch {
            val (success, message) = runPipeline(file)
            _uiState.value = _uiState.value.copy(
                isRunning = false,
                statusText = if (success) "OTA COMPLETE" else "OTA FAILED",
                resultAlertMessage = message
            )
        }
    }

    private suspend fun runPipeline(localZipFile: File): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        log("=== Starting Command Line OTA Upgrade ===")
        Log.i(TAG, "Payload target: ${localZipFile.name}, size: ${localZipFile.length()} bytes")

        // 1. Perform checksum
        log("🔍 Calculating local package CRC32 checksum…")
        val checksum = convertFileToChecksum(localZipFile.absolutePath)
        log("🔍 Calculated CRC32 Checksum: $checksum (0x${checksum.toString(16).uppercase(Locale.US)})")

        _uiState.value = _uiState.value.copy(
            statusText = "🔓 GAINING ROOT ACCESS…",
            progress = 0.05
        )
        log("🔓 Acquiring root permissions on the Android device…")

        // AdbClient.restartAsRoot() now genuinely verifies root (via dadb.root() + whoami)
        // instead of reporting Success regardless of outcome — treat Failure as fatal, since
        // every step below (staging under /data, update_engine_client) requires real root.
        val rootResult = adbClient.restartAsRoot()
        if (rootResult is AdbResult.Failure) {
            log("❌ Root escalation failed: ${rootResult.message}")
            return@withContext Pair(false, "Root escalation failed: ${rootResult.message}")
        }
        log("✅ Root confirmed: ${(rootResult as AdbResult.Success).data}")

        val remoteZipPath = "$remoteOTADirectory/update.zip"
        _uiState.value = _uiState.value.copy(statusText = "🚀 PUSHING OTA ZIP PACKAGE…")
        log("🚀 Checking and staging OTA package to $remoteZipPath…")

        adbClient.runShell("mkdir -p $remoteOTADirectory && chmod 777 $remoteOTADirectory")

        // Check if file is already on device with identical size
        val localSize = localZipFile.length()
        val checkRes = adbClient.runShell("stat -c %s $remoteZipPath || ls -l $remoteZipPath")
        val alreadyStaged = checkRes is AdbResult.Success && checkRes.data.contains(localSize.toString())

        if (alreadyStaged) {
            log("⚡ Package already staged on device. Skipping transfer.")
            _uiState.value = _uiState.value.copy(progress = 0.5)
        } else {
            val startTime = System.currentTimeMillis()
            val pushResult = adbClient.pushFile(localZipFile, remoteZipPath) { sent, total ->
                val fraction = if (total > 0) sent.toDouble() / total.toDouble() else 0.0
                _uiState.value = _uiState.value.copy(progress = 0.05 + fraction * 0.45)
            }
            // pushFile now returns Failure whenever the transfer didn't actually complete
            // (including the previously-silent case where nothing was really pushed) — no
            // change needed here, this check now means what it always should have.
            if (pushResult is AdbResult.Failure) {
                log("❌ Push failed: ${pushResult.message}")
                return@withContext Pair(false, "Transfer failed: ${pushResult.message}")
            }
            log("✅ Staged $localSize bytes in ${((System.currentTimeMillis() - startTime)/1000.0)}s.")
            _uiState.value = _uiState.value.copy(progress = 0.5)
        }

        _uiState.value = _uiState.value.copy(statusText = "⚙️ ANALYZING PACKAGE…")
        log("⚙️ Extracting payload specs from the ZIP header…")
        val payloadInfo: OTAPayloadInfo
        try {
            payloadInfo = OTAZipInspector.inspect(localZipFile)
        } catch (e: Exception) {
            log("❌ ZIP header extraction failed: ${e.localizedMessage}")
            return@withContext Pair(false, "Couldn't read the OTA package: ${e.localizedMessage}")
        }
        log("⚙️ Resolved payload.bin at offset=${payloadInfo.payloadOffset}, size=${payloadInfo.payloadSize}")

        val remotePropsPath = "$remoteOTADirectory/payload_properties.txt"
        val writePropsCmd = "printf '%s\\n' '${payloadInfo.rawPropertiesText.replace("'", "'\\''")}' > $remotePropsPath && chmod 666 $remotePropsPath"
        adbClient.runShell(writePropsCmd)

        val updateCommand = "UE_BIN=\$(which update_engine_client_android 2>/dev/null || which update_engine_client 2>/dev/null || echo update_engine_client_android); " +
                "\$UE_BIN --update --follow --payload=file://$remoteZipPath" +
                " --offset=${payloadInfo.payloadOffset} --size=${payloadInfo.payloadSize}" +
                " --headers=\"\$(cat $remotePropsPath | tr -d '\\r')\""

        _uiState.value = _uiState.value.copy(statusText = "🔥 STARTING UPDATE ENGINE…", progress = 0.55)

        adbClient.runShell("setenforce 0 || true")
        adbClient.runShell("chmod -R 777 $remoteOTADirectory || true")
        adbClient.runShell("update_engine_client_android --cancel || true")

        log("🔥 Spawning update_engine daemon. Streaming output…")

        var sawNeedRebootSignature = false
        var sawPayloadCompleteSignature = false

        try {
            adbClient.runShellStreaming("cd $remoteOTADirectory && $updateCommand").collect { line ->
                handleEngineLine(line)
                if (line.contains("UPDATED_NEED_REBOOT", ignoreCase = true)) sawNeedRebootSignature = true
                if (line.contains("ErrorCode::kSuccess", ignoreCase = true)) sawPayloadCompleteSignature = true
            }
        } catch (e: Exception) {
            log("⚠️ Streaming notice: ${e.localizedMessage}")
        }

        if (sawNeedRebootSignature || sawPayloadCompleteSignature) {
            log("🎉 OTA updates successfully registered in A/B slots!")
            _uiState.value = _uiState.value.copy(rebootConsentRequested = true, progress = 0.95)

            rebootConsentDeferred = CompletableDeferred()
            if (rebootConsentDeferred?.await() == true) {
                log("🔄 Requesting device reboot…")
                adbClient.reboot()
                return@withContext Pair(true, "OTA Successfully applied. Device rebooting…")
            } else {
                return@withContext Pair(true, "OTA Successfully applied. Reboot skipped.")
            }
        } else {
            return@withContext Pair(false, "Update engine closed without success signal.")
        }
    }
}