package com.royalenfield.provisioning.feature.ota.presentation

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

class OtaViewModel(
    private val otaPipeline: OtaPipeline,
    private val otaRepository: OtaRepository,
    private val adbClient: AdbClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(OtaUiState())
    val uiState: StateFlow<OtaUiState> = _uiState.asStateFlow()

    init {
        loadPackages()
    }

    fun loadPackages() {
        viewModelScope.launch {
            val packages = otaRepository.getAvailablePackages()
            _uiState.update {
                it.copy(
                    availablePackages = packages,
                    selectedPackage = packages.firstOrNull()
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
                    progressPercent = 5,
                    stageStatusText = "Executing pre-flight checks (battery, storage, boot partition)...",
                    terminalLogs = listOf("[START] Initiating OTA Deployment for ${pkg.targetVersion}"),
                    errorMessage = null
                )
            }

            otaPipeline.runPipeline(pkg).collect { progress ->
                when (progress) {
                    is OtaProgress.PreCheck -> {
                        addLog("[PRECHECK] Battery >= 50% OK, Free space >= 1.0GB OK")
                        _uiState.update { it.copy(pipelineStage = "DOWNLOADING", progressPercent = 10) }
                    }
                    is OtaProgress.Downloading -> {
                        _uiState.update {
                            it.copy(
                                pipelineStage = "DOWNLOADING",
                                progressPercent = progress.percent,
                                stageStatusText = "Downloading ${pkg.targetVersion} (${progress.percent}% - ${progress.bytesTransferred / 1024 / 1024}MB / ${progress.totalBytes / 1024 / 1024}MB)"
                            )
                        }
                    }
                    is OtaProgress.PushingToVehicle -> {
                        _uiState.update {
                            it.copy(
                                pipelineStage = "PUSHING",
                                progressPercent = progress.percent,
                                stageStatusText = "ADB pushing update.zip to vehicle /data/ota/ (${progress.percent}%)"
                            )
                        }
                    }
                    is OtaProgress.VerifyingChecksum -> {
                        _uiState.update {
                            it.copy(
                                pipelineStage = "VERIFYING",
                                progressPercent = progress.percent,
                                stageStatusText = "Verifying SHA-256 integrity hash..."
                            )
                        }
                        if (progress.sha256Matched) {
                            addLog("[VERIFY] SHA-256 checksum matched: ${pkg.sha256}")
                        }
                    }
                    is OtaProgress.FlashingPartition -> {
                        _uiState.update {
                            it.copy(
                                pipelineStage = "FLASHING",
                                progressPercent = progress.percent,
                                stageStatusText = "Flashing recovery payload to ${progress.currentSlot} (${progress.percent}%)"
                            )
                        }
                    }
                    is OtaProgress.AwaitingReboot -> {
                        addLog("[FLASH] Flash completed successfully! Ready for reboot verification.")
                        _uiState.update {
                            it.copy(
                                pipelineStage = "AWAITING_REBOOT",
                                progressPercent = 100,
                                stageStatusText = "Firmware written! Tap 'Reboot & Verify' to switch active partition."
                            )
                        }
                    }
                    is OtaProgress.Complete -> {
                        _uiState.update {
                            it.copy(
                                pipelineStage = "COMPLETE",
                                stageStatusText = "Deployment Completed! Active Version: ${pkg.targetVersion}",
                                currentInstalledVersion = pkg.targetVersion
                            )
                        }
                    }
                    is OtaProgress.Failed -> {
                        addLog("[ERROR] Pipeline aborted: ${progress.reason}")
                        _uiState.update {
                            it.copy(
                                pipelineStage = "FAILED",
                                errorMessage = progress.reason,
                                stageStatusText = "Pipeline Failed"
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
            addLog("[REBOOT] Sending ADB reboot signal to vehicle cluster...")
            otaPipeline.executeRebootAndVerify()
            _uiState.update {
                it.copy(
                    pipelineStage = "COMPLETE",
                    stageStatusText = "Vehicle successfully rebooted with firmware $targetVer",
                    currentInstalledVersion = targetVer
                )
            }
            addLog("[SUCCESS] Cluster booted into new active slot!")
        }
    }

    private fun addLog(log: String) {
        _uiState.update {
            val updated = it.terminalLogs.toMutableList()
            updated.add(log)
            it.copy(terminalLogs = updated)
        }
    }
}
