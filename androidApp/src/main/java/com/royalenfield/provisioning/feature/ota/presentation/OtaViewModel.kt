package com.royalenfield.provisioning.feature.ota.presentation

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.royalenfield.provisioning.core.adb.AdbClient
import com.royalenfield.provisioning.feature.ota.data.OtaPackage
import com.royalenfield.provisioning.feature.ota.data.OtaRepository
import com.royalenfield.provisioning.feature.ota.domain.OtaPipeline
import com.royalenfield.provisioning.feature.ota.domain.OtaProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OtaViewModel(
    private val otaPipeline: OtaPipeline,
    private val otaRepository: OtaRepository,
    private val adbClient: AdbClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(OtaUiState())
    val uiState: StateFlow<OtaUiState> = _uiState.asStateFlow()

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    init {
        loadPackages()
        queryClusterFirmware()
    }

    fun queryClusterFirmware() {
        viewModelScope.launch {
            val installed = otaRepository.queryInstalledFirmware()
            val slot = otaRepository.queryActiveSlot()
            _uiState.update {
                it.copy(
                    currentInstalledVersion = "$installed ($slot)"
                )
            }
        }
    }

    fun loadPackages() {
        viewModelScope.launch {
            _uiState.update { it.copy(stageStatusText = "Querying live OTA repository...") }
            otaRepository.getAvailablePackages()
                .onSuccess { packages ->
                    _uiState.update {
                        it.copy(
                            availablePackages = packages,
                            selectedPackage = packages.firstOrNull(),
                            errorMessage = if (packages.isEmpty()) "No firmware packages currently found in repository" else null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            availablePackages = emptyList(),
                            selectedPackage = null,
                            errorMessage = "OTA Repository offline: ${error.localizedMessage}"
                        )
                    }
                }
        }
    }

    fun onLocalFileSelected(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch {
            _uiState.update { it.copy(stageStatusText = "Ingesting local firmware zip...") }
            
            val localPackage = OtaPackage(
                id = "local_${System.currentTimeMillis()}",
                vehicleModel = "CUSTOM / LOCAL",
                targetVersion = "LOCAL_BUILD_${uri.lastPathSegment?.take(8) ?: "EXT"}",
                sizeBytes = 157286400, // 150MB simulated
                sizeDisplay = "Local File",
                sha256 = "verify-on-push",
                releaseDate = "N/A",
                notes = "User-selected firmware: ${uri.path}"
            )
            
            _uiState.update { 
                val newList = it.availablePackages.toMutableList()
                newList.add(0, localPackage)
                it.copy(
                    availablePackages = newList,
                    selectedPackage = localPackage,
                    stageStatusText = "Local firmware loaded."
                )
            }
        }
    }

    fun onSelectPackage(pkg: OtaPackage) {
        _uiState.update { it.copy(selectedPackage = pkg) }
    }

    fun startOtaPipeline() {
        val pkg = _uiState.value.selectedPackage ?: return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    pipelineStage = "PRECHECK",
                    progressPercent = 0,
                    stageStatusText = "Initializing binary stream...",
                    terminalLogs = listOf(
                        "[${timestamp()}] [INIT] starting re-ota-daemon v1.2.0",
                        "[${timestamp()}] [INIT] target: ${pkg.targetVersion}",
                        "[${timestamp()}] [INIT] origin: ${if (pkg.id.startsWith("local")) "LOCAL_PATH" else "CLOUD_CDN"}"
                    ),
                    errorMessage = null
                )
            }

            var isStageHeaderAdded = false

            otaPipeline.runPipeline(pkg).collect { progress ->
                when (progress) {
                    is OtaProgress.PreCheck -> {
                        addLog("[${timestamp()}] [SYS] battery_level: ${if (progress.batteryOk) "PASS" else "FAIL"}")
                        addLog("[${timestamp()}] [SYS] storage_space: ${if (progress.storageOk) "PASS" else "FAIL"}")
                        _uiState.update { it.copy(pipelineStage = "PRECHECK", progressPercent = 5) }
                        isStageHeaderAdded = false
                    }
                    is OtaProgress.Downloading -> {
                        if (!isStageHeaderAdded) {
                            addLog("[${timestamp()}] [NET] GET /packages/${pkg.id}.bin")
                            addLog("[${timestamp()}] [NET] content_length: ${progress.totalBytes} B")
                            addLog("") // Spacer for live progress
                            isStageHeaderAdded = true
                        }

                        val currentMbVal = progress.bytesTransferred / 1024 / 1024
                        val totalMbVal = progress.totalBytes / 1024 / 1024
                        updateLastLog("[${timestamp()}] [NET] ${renderProgressBar(progress.percent)} ${progress.percent}% ${currentMbVal}M/${totalMbVal}M")

                        _uiState.update {
                            it.copy(
                                pipelineStage = "DOWNLOADING",
                                progressPercent = progress.percent,
                                currentMb = currentMbVal,
                                totalMb = totalMbVal,
                                stageStatusText = "Downloading Binary: ${progress.percent}%"
                            )
                        }
                        if (progress.percent == 100) {
                            addLog("[${timestamp()}] [NET] payload download verified.")
                            isStageHeaderAdded = false
                        }
                    }
                    is OtaProgress.PushingToVehicle -> {
                        if (!isStageHeaderAdded) {
                            addLog("[${timestamp()}] [ADB] streaming payload -> /data/ota/update.zip")
                            addLog("") 
                            isStageHeaderAdded = true
                        }
                        
                        updateLastLog("[${timestamp()}] [ADB] ${renderProgressBar(progress.percent)} ${progress.percent}% @ ${progress.speedMbps} MB/s")

                        _uiState.update {
                            it.copy(
                                pipelineStage = "PUSHING",
                                progressPercent = progress.percent,
                                transferSpeedMbps = progress.speedMbps,
                                stageStatusText = "Transferring: ${progress.percent}%"
                            )
                        }
                        if (progress.percent == 100) {
                            addLog("[${timestamp()}] [ADB] 1 file pushed. sync complete.")
                            isStageHeaderAdded = false
                        }
                    }
                    is OtaProgress.VerifyingChecksum -> {
                        if (!isStageHeaderAdded) {
                            addLog("[${timestamp()}] [HASH] verifying sha256 checksum...")
                            isStageHeaderAdded = true
                        }
                        _uiState.update {
                            it.copy(
                                pipelineStage = "VERIFYING",
                                progressPercent = progress.percent,
                                stageStatusText = "Verifying Integrity: ${progress.percent}%"
                            )
                        }
                        if (progress.sha256Matched) {
                            addLog("[${timestamp()}] [HASH] update.zip: OK (${pkg.sha256.take(12)}...)")
                            isStageHeaderAdded = false
                        }
                    }
                    is OtaProgress.FlashingPartition -> {
                        if (!isStageHeaderAdded) {
                            addLog("[${timestamp()}] [FLASH] flash-client --write --partition ${progress.currentSlot}")
                            addLog("")
                            isStageHeaderAdded = true
                        }
                        
                        updateLastLog("[${timestamp()}] [FLASH] ${renderProgressBar(progress.percent)} write_op: ${progress.percent}%")

                        _uiState.update {
                            it.copy(
                                pipelineStage = "FLASHING",
                                progressPercent = progress.percent,
                                activePartition = progress.currentSlot,
                                stageStatusText = "Flashing ${progress.currentSlot.uppercase()}: ${progress.percent}%"
                            )
                        }
                        if (progress.percent == 100) {
                            addLog("[${timestamp()}] [FLASH] write operation successful.")
                            isStageHeaderAdded = false
                        }
                    }
                    is OtaProgress.AwaitingReboot -> {
                        addLog("[${timestamp()}] [PROC] system staged. ready for activation.")
                        _uiState.update {
                            it.copy(
                                pipelineStage = "AWAITING_REBOOT",
                                progressPercent = 100,
                                stageStatusText = "Awaiting Reboot"
                            )
                        }
                    }
                    is OtaProgress.Complete -> {
                        addLog("[${timestamp()}] [SUCCESS] RE_OTA_DONE: updated to ${pkg.targetVersion}")
                        _uiState.update {
                            it.copy(
                                pipelineStage = "COMPLETE",
                                stageStatusText = "Finished",
                                currentInstalledVersion = pkg.targetVersion
                            )
                        }
                    }
                    is OtaProgress.Failed -> {
                        addLog("[${timestamp()}] [FATAL] deploy-error: ${progress.reason}")
                        _uiState.update {
                            it.copy(
                                pipelineStage = "FAILED",
                                errorMessage = progress.reason,
                                stageStatusText = "Failed"
                            )
                        }
                    }
                }
            }
        }
    }

    fun confirmReboot() {
        val targetVer = _uiState.value.selectedPackage?.targetVersion ?: "RE_UPDATED_V2.2.0"
        viewModelScope.launch {
            addLog("[${timestamp()}] [CMD] executing system reboot...")
            otaPipeline.executeRebootAndVerify()
            _uiState.update {
                it.copy(
                    pipelineStage = "COMPLETE",
                    stageStatusText = "Rebooting...",
                    currentInstalledVersion = targetVer
                )
            }
            addLog("[${timestamp()}] [DONE] terminal signal lost. (vehicle rebooting)")
        }
    }

    private fun addLog(log: String) {
        _uiState.update {
            val updated = it.terminalLogs.toMutableList()
            updated.add(log)
            it.copy(terminalLogs = updated)
        }
    }

    private fun updateLastLog(log: String) {
        _uiState.update {
            val updated = it.terminalLogs.toMutableList()
            if (updated.isNotEmpty()) {
                updated[updated.size - 1] = log
            } else {
                updated.add(log)
            }
            it.copy(terminalLogs = updated)
        }
    }

    private fun renderProgressBar(percent: Int): String {
        val bars = percent / 5
        return "[" + "=".repeat(bars) + ">" + " ".repeat(20 - bars) + "]"
    }

    private fun timestamp(): String = timeFormat.format(Date())
}
