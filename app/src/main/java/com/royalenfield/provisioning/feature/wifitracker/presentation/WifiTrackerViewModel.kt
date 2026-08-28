package com.royalenfield.provisioning.feature.wifitracker.presentation

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.royalenfield.provisioning.core.adb.AdbManager
import com.royalenfield.provisioning.core.validation.SsidValidator
import com.royalenfield.provisioning.feature.wifitracker.data.WifiTrackerRepository
import com.royalenfield.provisioning.feature.wifitracker.data.WifiLogRecord
import com.royalenfield.provisioning.feature.wifitracker.domain.WifiUpdateWorkflow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.*

class WifiTrackerViewModel(
    private val workflow: WifiUpdateWorkflow,
    private val logRepository: WifiTrackerRepository,
    private val adbManager: AdbManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(WifiTrackerUiState())
    val uiState: StateFlow<WifiTrackerUiState> = _uiState.asStateFlow()

    private val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    init {
        val now = timeFormatter.format(Date())
        _uiState.update {
            it.copy(
                consoleLogs = listOf(
                    ConsoleLogItem(now, "Application initialized. Ready for VIN entry.", "#808080")
                ),
                executionLogs = listOf(
                    "[$now] Application initialized. Ready for VIN entry."
                )
            )
        }
        loadAuditLogs()
        fetchDeviceWifiDetails()
    }

    /**
     * Python matching log function with timestamp and color mapping
     */
    fun log(message: String, colorName: String = "black") {
        val colorHex = when (colorName.lowercase()) {
            "green" -> "#4EC9B0"
            "red" -> "#F44747"
            "orange" -> "#CE9178"
            "gray" -> "#808080"
            "cyan", "blue" -> "#38BDF8"
            else -> "#D4D4D4" // default light/white
        }
        val time = timeFormatter.format(Date())
        val newItem = ConsoleLogItem(timestamp = time, message = message, colorHex = colorHex)

        _uiState.update {
            it.copy(
                consoleLogs = it.consoleLogs + newItem,
                executionLogs = it.executionLogs + "[$time] $message"
            )
        }
    }

    /**
     * Handles VIN input parsing: alphanumeric only, max 17 uppercase characters
     */
    fun onVinChanged(text: String) {
        val cleanedVin = text.filter { it.isLetterOrDigit() }.uppercase(Locale.getDefault()).take(17)
        val error = if (cleanedVin.isNotEmpty() && cleanedVin.length != 17) {
            "VIN must be exactly 17 alphanumeric characters (current: ${cleanedVin.length})"
        } else null

        _uiState.update {
            it.copy(
                vinInput = cleanedVin,
                vinError = error
            )
        }
    }

    /**
     * Generates a secure 8-character password matching Python script:
     * special_chars = "!@#$%&[]{}"
     * alphabet = string.ascii_letters + string.digits + special_chars
     */
    fun generateNewPassword() {
        val specialChars = "!@#$%&[]{}"
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789" + specialChars
        val random = SecureRandom()
        val pwd = (1..8)
            .map { alphabet[random.nextInt(alphabet.length)] }
            .joinToString("")

        _uiState.update {
            it.copy(
                generatedPassword = pwd,
                isPasswordGenerated = true,
                newPassphraseInput = pwd
            )
        }
        log("New 8-character password generated: $pwd", "black")
    }

    fun openLogBottomSheet() {
        loadAuditLogs()
        _uiState.update { it.copy(isLogBottomSheetOpen = true) }
    }

    fun closeLogBottomSheet() {
        _uiState.update { it.copy(isLogBottomSheetOpen = false) }
    }

    fun onLogSearchQueryChanged(query: String) {
        _uiState.update { it.copy(logSearchQuery = query) }
    }

    fun dismissSuccessDialog() {
        _uiState.update { it.copy(showSuccessDialog = false) }
    }

    fun dismissErrorDialog() {
        _uiState.update { it.copy(showErrorDialog = false) }
    }

    private fun fetchDeviceWifiDetails() {
        viewModelScope.launch {
            try {
                val macRes = adbManager.readSoftApXml()
                macRes.onSuccess { xml ->
                    val ssidRegex = Regex("<string name=\"(?:SSID|Ssid)\">(?:&quot;)?(.*?)(?:&quot;)?</string>")
                    val match = ssidRegex.find(xml)
                    if (match != null) {
                        val ssid = match.groupValues[1].replace("\"", "").trim()
                        _uiState.update { it.copy(targetSsid = ssid, currentSsid = ssid) }
                    }
                }
                val mac = adbManager.getHardwareMacAddress()
                if (mac.isNotEmpty() && mac != "N/A") {
                    _uiState.update { it.copy(targetMacId = mac) }
                }
            } catch (e: Exception) {
                // Real-time device queries
            }
        }
    }

    /**
     * Main action: starts the Wi-Fi password change process (matching Python start_wifi_change_process)
     */
    // WifiTrackerViewModel.kt
    fun changeWifiPassword() {
        val state = _uiState.value
        val vin = state.vinInput.trim()
        val password = state.generatedPassword.trim()

        // ... Validation checks ...

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isChangingPassword = true,
                    updateSuccess = false,
                    errorMessage = null,
                    progressPercent = 0
                )
            }

            workflow.execute(
                vin = vin,
                newPassword = password,
                onLogMessage = { msg ->
                    val color = when {
                        msg.contains("successfully", ignoreCase = true) -> "green"
                        msg.startsWith("Error", ignoreCase = true) -> "red"
                        else -> "black"
                    }
                    log(msg, color)
                }
            ).collect { progress ->
                _uiState.update {
                    it.copy(
                        currentStep = progress.step,
                        progressPercent = progress.percent,
                        updateSuccess = progress.isComplete,
                        isChangingPassword = !progress.isComplete && progress.error == null,
                        errorMessage = progress.error,
                        targetSsid = if (progress.ssid.isNotEmpty() && progress.ssid != "N/A") progress.ssid else it.targetSsid,
                        targetMacId = if (progress.macAddress.isNotEmpty() && progress.macAddress != "N/A") progress.macAddress else it.targetMacId
                    )
                }
            }

            val finalState = _uiState.value
            if (finalState.updateSuccess) {
                // Keep user on the current screen and display success dialog
                _uiState.update {
                    it.copy(
                        showSuccessDialog = true,
                        dialogMessage = "Successfully updated Wi-Fi passphrase!\nTarget device is rebooting..."
                    )
                }
            } else if (finalState.errorMessage != null) {
                _uiState.update {
                    it.copy(
                        showErrorDialog = true,
                        dialogMessage = "Wi-Fi configuration change failed: ${finalState.errorMessage}"
                    )
                }
            }

            loadAuditLogs()
        }
    }

    fun loadAuditLogs() {
        viewModelScope.launch {
            val logs = logRepository.getLogs()
            _uiState.update {
                it.copy(
                    transactionLogs = logs,
                    auditLogs = logs
                )
            }
        }
    }

    /**
     * Exports or opens Wifi_Password_Tracker.csv
     */
    fun exportCsv(context: Context, openDirectly: Boolean = false) {
        viewModelScope.launch {
            try {
                val csvFile = logRepository.exportLogsToCsv()
                log("Exported transaction log CSV to: ${csvFile.name} (${csvFile.length()} bytes)", "green")

                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    csvFile
                )

                if (openDirectly) {
                    val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "text/csv")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    try {
                        context.startActivity(Intent.createChooser(viewIntent, "Open in Excel / Sheets"))
                    } catch (e: Exception) {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/csv"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Open / Share CSV"))
                    }
                } else {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/csv"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_SUBJECT, "Wi-Fi Password Transaction History CSV")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Save / Export CSV As..."))
                }
            } catch (e: Exception) {
                log("Failed to export CSV: ${e.localizedMessage}", "red")
            }
        }
    }

    // Legacy support functions
    fun onSsidChanged(newSsid: String) {
        val sanitized = SsidValidator.sanitize(newSsid)
        _uiState.update { it.copy(newSsidInput = sanitized, targetSsid = sanitized) }
    }

    fun onPassphraseChanged(newPass: String) {
        _uiState.update { it.copy(newPassphraseInput = newPass, generatedPassword = newPass) }
    }

    fun loadRawXml() {
        viewModelScope.launch {
            _uiState.update { it.copy(isReadingXml = true) }
            val result = adbManager.readSoftApXml()
            result.onSuccess { xml ->
                _uiState.update { it.copy(rawXmlContent = xml, isReadingXml = false) }
            }.onFailure {
                _uiState.update {
                    it.copy(
                        rawXmlContent = "<!-- SoftAP XML -->\n<WifiConfigStoreSoftAp>\n  <SoftApConfiguration>\n    <string name=\"SSID\">&quot;${_uiState.value.currentSsid}&quot;</string>\n  </SoftApConfiguration>\n</WifiConfigStoreSoftAp>",
                        isReadingXml = false
                    )
                }
            }
        }
    }

    fun executeWorkflow() {
        changeWifiPassword()
    }
}
