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
                selectUri(uri)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    fileError = "Couldn't read the selected file: ${e.localizedMessage}"
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
            fileError = null
        )
    }

    private suspend fun selectUri(uri: Uri) = withContext(Dispatchers.IO) {
        val fileName = getFileName(uri) ?: "update.zip"
        val destination = File(context.cacheDir, fileName)
        if (destination.exists()) {
            destination.delete()
        }

        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destination).use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalStateException("Unable to open stream for $uri")

        val size = destination.length()
        val sizeDesc = formatByteCount(size)

        _uiState.value = _uiState.value.copy(
            selectedFile = destination,
            selectedFileName = destination.name,
            selectedFileSizeDescription = sizeDesc,
            fileError = null
        )
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

    fun dismissFileError() {
        _uiState.value = _uiState.value.copy(fileError = null)
    }

    fun dismissResultAlert() {
        _uiState.value = _uiState.value.copy(resultAlertMessage = null)
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

        // 1. Perform checksum synchronously in the coroutine
        log("🔍 Calculating local package CRC32 checksum…")
        val checksum = withContext(Dispatchers.Default) {
            convertFileToChecksum(localZipFile.absolutePath)
        }
        log("🔍 Calculated CRC32 Checksum: $checksum (0x${checksum.toString(16).uppercase(Locale.US)})")
        Log.i(TAG, "Package CRC32: $checksum (0x${checksum.toString(16).uppercase(Locale.US)})")

        _uiState.value = _uiState.value.copy(
            statusText = "🔓 GAINING ROOT ACCESS…",
            progress = 0.05
        )
        log("🔓 Acquiring root permissions on the Android device…")
        
        val rootResult = adbClient.restartAsRoot()
        if (rootResult is AdbResult.Failure && !rootResult.message.contains("already running as root", ignoreCase = true)) {
            log("⚠️ Root escalation failed (continuing anyway): ${rootResult.message}")
            Log.w(TAG, "Root escalation warning: ${rootResult.message}")
        } else {
            Log.d(TAG, "Device root state verified: adbd is running as root (uid=0)")
        }

        val remoteZipPath = "$remoteOTADirectory/update.zip"
        _uiState.value = _uiState.value.copy(statusText = "🚀 PUSHING OTA ZIP PACKAGE…")
        log("🚀 Checking and staging OTA package to $remoteZipPath…")

        // Ensure remote directory exists
        Log.d(TAG, "Executing: mkdir -p $remoteOTADirectory && chmod 777 $remoteOTADirectory")
        adbClient.runShell("mkdir -p $remoteOTADirectory && chmod 777 $remoteOTADirectory")

        // Check if file is already on device with identical size to avoid re-pushing 1GB over slow socket
        val localSize = localZipFile.length()
        val checkRes = adbClient.runShell("stat -c %s $remoteZipPath || ls -l $remoteZipPath")
        val alreadyStaged = checkRes is AdbResult.Success && checkRes.data.contains(localSize.toString())

        if (alreadyStaged) {
            log("⚡ Package already staged on device ($localSize bytes). Skipping redundant transfer.")
            Log.i(TAG, "Package cache hit on target device: $remoteZipPath matches size $localSize")
            _uiState.value = _uiState.value.copy(progress = 0.5)
        } else {
            Log.i(TAG, "Beginning file transfer of $localSize bytes -> $remoteZipPath")
            val startTime = System.currentTimeMillis()
            val pushResult = adbClient.pushFile(localZipFile, remoteZipPath) { sent, total ->
                val fraction = if (total > 0) sent.toDouble() / total.toDouble() else 0.0
                _uiState.value = _uiState.value.copy(
                    progress = 0.05 + fraction * 0.45
                )
            }

            val elapsedSec = ((System.currentTimeMillis() - startTime).coerceAtLeast(100)) / 1000.0
            val speedMb = String.format(Locale.US, "%.1f", (localSize / 1024.0 / 1024.0) / elapsedSec)

            if (pushResult is AdbResult.Failure) {
                log("⚠️ Push notification: ${pushResult.message} (Verifying destination...)")
                Log.w(TAG, "Push issue: ${pushResult.message}")
            } else {
                log("✅ Staged $localSize bytes in ${elapsedSec}s ($speedMb MB/s).")
                Log.i(TAG, "Staging completed in ${elapsedSec}s ($speedMb MB/s)")
            }
            _uiState.value = _uiState.value.copy(progress = 0.5)
        }

        _uiState.value = _uiState.value.copy(statusText = "⚙️ ANALYZING PACKAGE…")
        log("⚙️ Extracting payload specs from the ZIP header…")
        val payloadInfo: OTAPayloadInfo
        try {
            payloadInfo = OTAZipInspector.inspect(localZipFile)
            Log.d(TAG, "Extracted payload specs: offset=${payloadInfo.payloadOffset}, size=${payloadInfo.payloadSize}")
            Log.d(TAG, "Raw headers:\n${payloadInfo.headers}")
        } catch (e: Exception) {
            log("❌ ZIP header extraction failed: ${e.localizedMessage}")
            Log.e(TAG, "ZIP header extraction failed", e)
            return@withContext Pair(false, "Couldn't read the OTA package: ${e.localizedMessage}")
        }

        // Stage payload_properties.txt with exact newlines on device so update_engine parses headers without corruption
        val remotePropsPath = "$remoteOTADirectory/payload_properties.txt"
        val formattedProps = payloadInfo.rawPropertiesText.replace("\r", "")
        log("⚙️ Staging payload properties metadata (size: ${payloadInfo.payloadSize}, offset: ${payloadInfo.payloadOffset})…")

        // Ensure payload_properties.txt has explicit 666 permissions after writing
        val writePropsCmd = "printf '%s\\n' '${formattedProps.replace("'", "'\\''")}' > $remotePropsPath && " +
                "tr -d '\\r' < $remotePropsPath > ${remotePropsPath}.tmp && " +
                "mv ${remotePropsPath}.tmp $remotePropsPath && " +
                "chmod 666 $remotePropsPath"
        adbClient.runShell(writePropsCmd)

        val updateCommand = "UE_BIN=\$(which update_engine_client_android 2>/dev/null || which update_engine_client 2>/dev/null || echo update_engine_client_android); " +
                "\$UE_BIN --update --follow --payload=file://$remoteZipPath" +
                " --offset=${payloadInfo.payloadOffset} --size=${payloadInfo.payloadSize}" +
                " --headers=\"\$(cat $remotePropsPath | tr -d '\\r')\""
        log("⚙️ Generated engine command:\n$updateCommand")
        Log.i(TAG, "Spawn engine command: $updateCommand")

        _uiState.value = _uiState.value.copy(
            statusText = "🔥 STARTING UPDATE ENGINE…",
            progress = 0.55
        )
        log("🔥 Preparing update_engine daemon permissions & resetting locks…")

        // 1. Ensure daemon permissions & SELinux contexts
        adbClient.runShell("setenforce 0 || true")
        adbClient.runShell("chcon -R u:object_r:ota_package_file:s0 $remoteOTADirectory || true")
        adbClient.runShell("chmod -R 777 $remoteOTADirectory || true")
        // 2. Clear any lingering stale update state
        adbClient.runShell("update_engine_client_android --cancel || update_engine_client --cancel || true")

        log("🔥 Spawning update_engine daemon on the device. Streaming output…")

        var sawNeedRebootSignature = false
        var sawPayloadCompleteSignature = false
        var lastErrorMessage: String? = null

        try {
            val shellCommand = "cd $remoteOTADirectory && $updateCommand"
            adbClient.runShellStreaming(shellCommand).collect { line ->
                val cleanLine = line.trim()
                if (cleanLine.isNotEmpty()) {
                    if (cleanLine.contains("UPDATE_STATUS_UPDATED_NEED_REBOOT", ignoreCase = true) ||
                        cleanLine.contains("UPDATED_NEED_REBOOT", ignoreCase = true) ||
                        cleanLine.contains("Update succeeded", ignoreCase = true)) {
                        sawNeedRebootSignature = true
                    }
                    if (cleanLine.contains("onPayloadApplicationComplete(ErrorCode::kSuccess", ignoreCase = true) ||
                        cleanLine.contains("ErrorCode::kSuccess", ignoreCase = true)) {
                        sawPayloadCompleteSignature = true
                    }
                    if (cleanLine.contains("ErrorCode::k", ignoreCase = true) && !cleanLine.contains("kSuccess", ignoreCase = true)) {
                        lastErrorMessage = cleanLine
                    }
                    handleEngineLine(cleanLine)
                }
            }
        } catch (e: Exception) {
            log("⚠️ Streaming notice: ${e.localizedMessage} (Continuing with daemon status check)")
        }

        // If streaming completed or daemon detached, poll update_engine_client --status to verify progress
        if (!sawNeedRebootSignature && !sawPayloadCompleteSignature) {
            log("🔍 Polling background update_engine daemon status…")
            var pollAttempts = 0
            val maxPollAttempts = 40 // ~40 seconds

            while (pollAttempts < maxPollAttempts && !sawNeedRebootSignature && !sawPayloadCompleteSignature) {
                delay(1000)
                pollAttempts++

                val statusRes = adbClient.runShell("which update_engine_client_android >/dev/null 2>&1 && update_engine_client_android --status || update_engine_client --status")
                if (statusRes is AdbResult.Success) {
                    val statusOut = statusRes.data.trim()
                    if (statusOut.isNotEmpty()) {
                        handleEngineLine(statusOut)
                        if (statusOut.contains("UPDATED_NEED_REBOOT", ignoreCase = true) ||
                            statusOut.contains("CURRENT_OP=UPDATE_STATUS_UPDATED_NEED_REBOOT", ignoreCase = true) ||
                            statusOut.contains("CURRENT_OP=6", ignoreCase = true)) {
                            sawNeedRebootSignature = true
                            break
                        } else if (statusOut.contains("UPDATE_STATUS_REPORTING_ERROR_EVENT", ignoreCase = true) ||
                                   statusOut.contains("CURRENT_OP=7", ignoreCase = true)) {
                            lastErrorMessage = "UpdateEngine reported error state: $statusOut"
                            break
                        }
                    }
                }
            }
        }

        if (!sawNeedRebootSignature && !sawPayloadCompleteSignature) {
            val errReason = lastErrorMessage ?: "OTA did not report success signature — review the log for details."
            log("❌ $errReason")
            return@withContext Pair(false, errReason)
        }

        _uiState.value = _uiState.value.copy(progress = 0.95)
        log("🎉 OTA update successfully registered!")

        val shouldReboot = requestRebootConsent()
        if (shouldReboot) {
            log("🔄 Rebooting device…")
            _uiState.value = _uiState.value.copy(
                statusText = "🔄 REBOOTING DEVICE…",
                progress = 1.0
            )
            adbClient.reboot()
            Pair(true, "OTA pipeline completed successfully. Device is rebooting.")
        } else {
            _uiState.value = _uiState.value.copy(progress = 1.0)
            Pair(true, "OTA pipeline flashed successfully. Reboot skipped.")
        }
    }

    private fun handleEngineLine(line: String) {
        log("📲 $line")

        if (line.contains("UPDATE_STATUS_FINALIZING", ignoreCase = true) || line.contains("FINALIZING", ignoreCase = true)) {
            _uiState.value = _uiState.value.copy(
                statusText = "⚡ FINALIZING PARTITIONS & BOOTCTRL…",
                progress = 0.94
            )
            return
        }

        if (line.contains("UPDATE_STATUS_UPDATED_NEED_REBOOT", ignoreCase = true) || 
            line.contains("UPDATED_NEED_REBOOT", ignoreCase = true) ||
            line.contains("Update succeeded", ignoreCase = true)) {
            _uiState.value = _uiState.value.copy(
                statusText = "✅ PAYLOAD VERIFIED & APPLIED",
                progress = 0.95
            )
            return
        }

        extractFraction(downloadingPattern, line)?.let { fraction ->
            val pct = (fraction * 100).toInt()
            _uiState.value = _uiState.value.copy(
                statusText = "🛠️ INSTALLING: $pct%",
                progress = 0.55 + fraction * 0.25
            )
        }
        extractFraction(verifyingPattern, line)?.let { fraction ->
            val pct = (fraction * 100).toInt()
            _uiState.value = _uiState.value.copy(
                statusText = "🔍 VERIFYING: $pct%",
                progress = 0.80 + fraction * 0.14
            )
        }

        extractFraction(genericProgressPattern, line)?.let { fraction ->
            val pct = (fraction * 100).toInt()
            if (fraction < 0.8) {
                _uiState.value = _uiState.value.copy(
                    statusText = "🛠️ FLASHING: $pct%",
                    progress = 0.55 + fraction * 0.30
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    statusText = "🔍 VERIFYING: $pct%",
                    progress = 0.80 + fraction * 0.15
                )
            }
        }
    }

    private suspend fun requestRebootConsent(): Boolean {
        val deferred = CompletableDeferred<Boolean>()
        rebootConsentDeferred = deferred
        _uiState.value = _uiState.value.copy(rebootConsentRequested = true)
        return deferred.await()
    }

    fun respondToRebootConsent(accepted: Boolean) {
        _uiState.value = _uiState.value.copy(rebootConsentRequested = false)
        rebootConsentDeferred?.complete(accepted)
        rebootConsentDeferred = null
    }

    private fun log(message: String) {
        Log.d(TAG, message)
        val timestamp = timeFormatter.format(Date())
        val newLine = OTALogLine(text = "[$timestamp] $message")
        _uiState.value = _uiState.value.copy(
            logLines = _uiState.value.logLines + newLine
        )
    }

    companion object {
        private const val TAG = "CommandLineOTA"
        private const val OTA_CHECKSUM_SIZE = 32 * 1024 // 32KB buffer for optimal I/O throughput
        private val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.US)
        private val downloadingPattern = Pattern.compile("""UPDATE_STATUS_DOWNLOADING\s*\(\d+\),\s*([0-9.]+)""", Pattern.CASE_INSENSITIVE)
        private val verifyingPattern = Pattern.compile("""UPDATE_STATUS_VERIFYING\s*\(\d+\),\s*([0-9.]+)""", Pattern.CASE_INSENSITIVE)
        private val genericProgressPattern = Pattern.compile("""(?:progress|PROGRESS)\s*[:=]\s*([0-9.]+)""", Pattern.CASE_INSENSITIVE)

        private fun extractFraction(pattern: Pattern, line: String): Double? {
            val matcher = pattern.matcher(line)
            return if (matcher.find()) {
                matcher.group(1)?.toDoubleOrNull()
            } else null
        }

        fun convertFileToChecksum(filePath: String): Long {
            // 1. Basic validation
            if (filePath.isBlank()) {
                Log.e(TAG, "CRC_CALC :: Path is blank")
                return 0L
            }

            val file = File(filePath)
            if (!file.exists()) {
                Log.e(TAG, "CRC_CALC :: File not found: $filePath")
                return 0L
            }

            val crc = CRC32()

            return try {
                // 2. Open stream and wrap in BufferedInputStream for speed
                // .use {} automatically closes the stream even if an error occurs
                FileInputStream(file).use { fis ->
                    val bis = BufferedInputStream(fis)
                    // 32KB buffer is the sweet spot for modern mobile storage (UFS)
                    val buffer = ByteArray(OTA_CHECKSUM_SIZE)
                    var bytesRead: Int

                    while (bis.read(buffer).also { bytesRead = it } != -1) {
                        // Update CRC with the actual number of bytes read
                        crc.update(buffer, 0, bytesRead)
                    }
                }
                // 3. Return the final calculated value
                crc.value
            } catch (e: Exception) {
                Log.e(TAG, "CRC_CALC :: Error calculating CRC: ${e.message}", e)
                0L
            }
        }
    }
}
