package com.royalenfield.provisioning.feature.ota.presentation

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.royalenfield.provisioning.feature.ota.data.OtaPackage
import com.royalenfield.provisioning.feature.ota.data.OtaRepository
import com.royalenfield.provisioning.feature.ota.domain.OtaPipeline
import com.royalenfield.provisioning.feature.ota.domain.OtaProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class OtaViewModel(
    private val context: Context,
    private val otaPipeline: OtaPipeline,
    private val otaRepository: OtaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OtaUiState())
    val uiState: StateFlow<OtaUiState> = _uiState.asStateFlow()

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private var selectedLocalFileUri: Uri? = null

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
        selectedLocalFileUri = uri
        viewModelScope.launch {
            _uiState.update { it.copy(stageStatusText = "Ingesting local firmware zip...") }
            
            // Try to get the actual file size
            var fileSize: Long = 0
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use {
                fileSize = it.length
            }

            val localPackage = OtaPackage(
                id = "local_${System.currentTimeMillis()}",
                vehicleModel = "CUSTOM / LOCAL",
                targetVersion = "LOCAL_BUILD_${uri.lastPathSegment?.take(8) ?: "EXT"}",
                sizeBytes = fileSize,
                sizeDisplay = if (fileSize > 0) "${fileSize / 1024 / 1024} MB" else "Local File",
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
        if (!pkg.id.startsWith("local")) {
            selectedLocalFileUri = null
        }
    }

    fun startOtaPipeline() {
        val pkg = _uiState.value.selectedPackage ?: return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    pipelineStage = "PRECHECK",
                    progressPercent = 0,
                    stageStatusText = "Initializing binary stream...",
                    terminalLogs = emptyList(),
                    errorMessage = null
                )
            }
            addLog("$ ota-deploy --target ${pkg.targetVersion} --verbose")

            // Resolve local file if needed
            val localFile = if (pkg.id.startsWith("local") && selectedLocalFileUri != null) {
                copyUriToTempFile(selectedLocalFileUri!!)
            } else {
                null
            }

            otaPipeline.runPipeline(pkg, localFile).collect { progress ->
                when (progress) {
                    is OtaProgress.Log -> addLog(progress.message)
                    is OtaProgress.PreCheck -> {
                        _uiState.update { it.copy(pipelineStage = "PRECHECK", progressPercent = 5) }
                    }
                    is OtaProgress.Downloading -> {
                        _uiState.update {
                            it.copy(
                                pipelineStage = "DOWNLOADING",
                                progressPercent = progress.percent,
                                currentMb = progress.bytesTransferred / 1024 / 1024,
                                totalMb = progress.totalBytes / 1024 / 1024,
                                stageStatusText = "DOWNLOADING: ${progress.percent}%"
                            )
                        }
                    }
                    is OtaProgress.PushingToVehicle -> {
                        _uiState.update {
                            it.copy(
                                pipelineStage = "PUSHING",
                                progressPercent = progress.percent,
                                transferSpeedMbps = progress.speedMbps,
                                stageStatusText = "TRANSFERRING: ${progress.percent}%"
                            )
                        }
                    }
                    is OtaProgress.VerifyingChecksum -> {
                        _uiState.update {
                            it.copy(
                                pipelineStage = "VERIFYING",
                                progressPercent = progress.percent,
                                stageStatusText = "VERIFYING: ${progress.percent}%"
                            )
                        }
                    }
                    is OtaProgress.FlashingPartition -> {
                        _uiState.update {
                            it.copy(
                                pipelineStage = "FLASHING",
                                progressPercent = progress.percent,
                                activePartition = progress.currentSlot,
                                stageStatusText = "FLASHING ${progress.currentSlot.uppercase()}: ${progress.percent}%"
                            )
                        }
                    }
                    is OtaProgress.AwaitingReboot -> {
                        _uiState.update {
                            it.copy(
                                pipelineStage = "AWAITING_REBOOT",
                                progressPercent = 100,
                                stageStatusText = "STAGED: PENDING REBOOT"
                            )
                        }
                    }
                    is OtaProgress.Complete -> {
                        _uiState.update {
                            it.copy(
                                pipelineStage = "COMPLETE",
                                stageStatusText = "DEPLOYMENT SUCCESSFUL",
                                currentInstalledVersion = pkg.targetVersion
                            )
                        }
                    }
                    is OtaProgress.Failed -> {
                        _uiState.update {
                            it.copy(
                                pipelineStage = "FAILED",
                                errorMessage = progress.reason,
                                stageStatusText = "DEPLOYMENT FAILED"
                            )
                        }
                    }
                }
            }
            
            // Cleanup temp file
            localFile?.delete()
        }
    }

    private fun copyUriToTempFile(uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val tempFile = File(context.cacheDir, "update_staging.zip")
            FileOutputStream(tempFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            tempFile
        } catch (e: Exception) {
            null
        }
    }

    fun confirmReboot() {
        val targetVer = _uiState.value.selectedPackage?.targetVersion ?: "Rebooting..."
        viewModelScope.launch {
            addLog("[CMD] executing sys-reboot...")
            otaPipeline.executeRebootAndVerify()
            _uiState.update {
                it.copy(
                    pipelineStage = "COMPLETE",
                    stageStatusText = "REBOOTING...",
                    currentInstalledVersion = targetVer
                )
            }
            addLog("[DONE] terminal signal lost. (vehicle rebooting)")
        }
    }

    private fun addLog(log: String) {
        _uiState.update {
            val updated = it.terminalLogs.toMutableList()
            updated.add("[${timestamp()}] $log")
            it.copy(terminalLogs = updated)
        }
    }

    private fun timestamp(): String = timeFormat.format(Date())
}
