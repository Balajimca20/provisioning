package com.royalenfield.provisioning.feature.ota.presentation

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.royalenfield.provisioning.core.adb.AdbClient
import com.royalenfield.provisioning.core.adb.AdbResult
import com.royalenfield.provisioning.feature.ota.domain.OTAPayloadInfo
import com.royalenfield.provisioning.feature.ota.domain.OTAZipInspector
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.regex.Pattern

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

        _uiState.value = _uiState.value.copy(
            statusText = "🔓 GAINING ROOT ACCESS…",
            progress = 0.05
        )
        log("🔓 Acquiring root permissions on the Android device…")
        
        val rootResult = adbClient.restartAsRoot()
        if (rootResult is AdbResult.Failure && !rootResult.message.contains("already running as root", ignoreCase = true)) {
            log("⚠️ Root escalation failed (continuing anyway): ${rootResult.message}")
        }

        val remoteZipPath = "$remoteOTADirectory/update.zip"
        _uiState.value = _uiState.value.copy(statusText = "🚀 PUSHING OTA ZIP PACKAGE…")
        log("🚀 Checking and staging OTA package to $remoteZipPath…")

        // Ensure remote directory exists
        adbClient.runShell("mkdir -p $remoteOTADirectory && chmod 777 $remoteOTADirectory")

        // Check if file is already on device with identical size to avoid re-pushing 1GB over slow socket
        val localSize = localZipFile.length()
        val checkRes = adbClient.runShell("stat -c %s $remoteZipPath || ls -l $remoteZipPath")
        val alreadyStaged = checkRes is AdbResult.Success && checkRes.data.contains(localSize.toString())

        if (alreadyStaged) {
            log("⚡ Package already staged on device ($localSize bytes). Skipping redundant transfer.")
            _uiState.value = _uiState.value.copy(progress = 0.5)
        } else {
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
            } else {
                log("✅ Staged $localSize bytes in ${elapsedSec}s ($speedMb MB/s).")
            }
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

        val updateCommand = "update_engine_client --update --follow --payload=file://$remoteZipPath" +
                " --offset=${payloadInfo.payloadOffset} --size=${payloadInfo.payloadSize}" +
                " --headers=\"${payloadInfo.headers}\""
        log("⚙️ Generated engine command:\n$updateCommand")

        _uiState.value = _uiState.value.copy(
            statusText = "🔥 STARTING UPDATE ENGINE…",
            progress = 0.55
        )
        log("🔥 Spawning update_engine_client on the device. Streaming output…")

        var sawNeedRebootSignature = false
        var sawPayloadCompleteSignature = false

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
                    handleEngineLine(cleanLine)
                }
            }
        } catch (e: Exception) {
            log("❌ Error while streaming update_engine_client output: ${e.localizedMessage}")
            return@withContext Pair(false, "The update engine command failed: ${e.localizedMessage}")
        }

        if (!sawNeedRebootSignature && !sawPayloadCompleteSignature) {
            log("❌ Process closed without a successful registration signature.")
            return@withContext Pair(false, "OTA did not report success — review the log for details.")
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
                progress = 0.80 + fraction * 0.15
            )
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
        val timestamp = timeFormatter.format(Date())
        val newLine = OTALogLine(text = "[$timestamp] $message")
        _uiState.value = _uiState.value.copy(
            logLines = _uiState.value.logLines + newLine
        )
    }

    companion object {
        private val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.US)
        private val downloadingPattern = Pattern.compile("""UPDATE_STATUS_DOWNLOADING\s*\(\d+\),\s*([0-9.]+)""")
        private val verifyingPattern = Pattern.compile("""UPDATE_STATUS_VERIFYING\s*\(\d+\),\s*([0-9.]+)""")

        private fun extractFraction(pattern: Pattern, line: String): Double? {
            val matcher = pattern.matcher(line)
            return if (matcher.find()) {
                matcher.group(1)?.toDoubleOrNull()
            } else null
        }
    }
}
