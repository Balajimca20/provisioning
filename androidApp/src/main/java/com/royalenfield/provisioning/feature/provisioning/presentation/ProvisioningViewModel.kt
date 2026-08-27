package com.royalenfield.provisioning.feature.provisioning.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.royalenfield.provisioning.feature.provisioning.data.repository.ProvisioningRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ProvisioningViewModel.kt
sealed class ProvisioningStatus {
    object Idle : ProvisioningStatus()
    data class Running(val step: String, val progress: Int) : ProvisioningStatus()
    data class Success(val message: String) : ProvisioningStatus()
    data class Error(val error: String) : ProvisioningStatus()
}

class ProvisioningViewModel(private val repository: ProvisioningRepository) : ViewModel() {

    private val _provisioningUiState = MutableStateFlow(ProvisioningStateModel())
    val provisioningUiState: StateFlow<ProvisioningStateModel> = _provisioningUiState

    private val _status = MutableStateFlow<ProvisioningStatus>(ProvisioningStatus.Idle)
    val status: StateFlow<ProvisioningStatus> = _status.asStateFlow()

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    private var provisioningJob: Job? = null

    fun startProvisioning(ip: String, payloadFiles: List<File>) {
        provisioningJob?.cancel()
        provisioningJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                log("🔓 Acquiring Root Access...")
                _status.value = ProvisioningStatus.Running("Acquiring Root Access", 10)
                executeShellCommand("adb connect $ip")
                executeShellCommand("adb root")

                log("🛑 Stopping target hub services...")
                executeShellCommand("adb shell stop c2c_hub_service")

                log("☁️ Registering Cloud Metadata...")
                _status.value = ProvisioningStatus.Running("Registering Cloud Metadata", 25)
                val isRegistered = repository.registerVehicleMetadata(
                    vin = _provisioningUiState.value.vinNumber,
                    modelCode = _provisioningUiState.value.selectedVariant.modelCode,
                    modelDesc = _provisioningUiState.value.selectedVariant.description,
                    region = _provisioningUiState.value.selectedRegion.regionName,
                    country = _provisioningUiState.value.selectedRegion.country
                ).getOrDefault(false)

                if (!isRegistered) log("⚠️ Cloud registration returned an unexpected status code.")

                log("🧹 Clearing target buffers...")
                executeShellCommand("adb shell rm -rf /data/vendor/c2c/tele_buff/*")
                executeShellCommand("adb shell rm -rf /mnt/vendor/persist/c2c/RFF/READY/*")

                log("📂 Pushing payload files...")
                payloadFiles.forEachIndexed { index, file ->
                    val progress = 45 + (((index + 1).toFloat() / payloadFiles.size) * 25).toInt()
                    _status.value = ProvisioningStatus.Running("Pushing ${file.name}", progress)
                    executeShellCommand("adb push ${file.absolutePath} /mnt/vendor/persist/c2c/")
                }

                log("🔄 Rebooting device...")
                executeShellCommand("adb reboot")

                log("🛰️ Monitoring Telemetry JSON...")
                monitorTelemetryState()

                _status.value = ProvisioningStatus.Success("Provisioning completed successfully!")
            } catch (e: Exception) {
                if (e is CancellationException) {
                    log("⛔ Provisioning cancelled.")
                } else {
                    log("❌ Error: ${e.message}")
                    _status.value = ProvisioningStatus.Error(e.message ?: "Execution failed")
                }
            }
        }
    }

    fun stopProvisioning() {
        provisioningJob?.cancel()
        _status.value = ProvisioningStatus.Idle
    }

    private suspend fun monitorTelemetryState() {
        var state = ""
        while (state != "PRE_REGIONAL_ACTIVE") {
            val output = executeShellCommand("adb shell cat /mnt/vendor/persist/c2c/c2c_vehicle.json")
            if (output.contains("PRE_REGIONAL_ACTIVE")) {
                log("🏆 Target State [PRE_REGIONAL_ACTIVE] Detected!")
                break
            }
            delay(2000)
        }
    }

    private fun executeShellCommand(cmd: String): String {
        return try {
            val process = Runtime.getRuntime().exec(cmd)
            process.inputStream.bufferedReader().readText()
        } catch (e: Exception) {
            ""
        }
    }

    private fun log(message: String) {
        _logs.value = _logs.value + "[${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(
            Date()
        )}] $message"
    }

    fun updateVinNumber(vin: String) {
        _provisioningUiState.value = _provisioningUiState.value.copy(vinNumber = vin)
    }

    fun onSelectRegion(region: Region) {
        _provisioningUiState.value = _provisioningUiState.value.copy(selectedRegion = region)
    }

    fun onSelectVariant(variant: VehicleVariant) {
        _provisioningUiState.value = _provisioningUiState.value.copy(selectedVariant = variant)
    }

    fun onPostLog(log: String) {
        _logs.value += log
    }
}

data class ProvisioningStateModel(
    val vinNumber: String = "",
    val selectedVariant : VehicleVariant = VehicleVariant.FLYING_FLEA_C6,
    val selectedRegion : Region = Region.EU_FRANCE,
    val progress: Float = 0f,
)

enum class VehicleVariant(val modelCode: String,val description: String){
    FLYING_FLEA_C6("VLQM91FX","FLYING FLEA C6, FLEA GREEN"),
    PARACHUTE_WHITE("VLQM91PW","FLYING FLEA C6, PARACHUTE WHITE"),
    STROM_BLACK("VLQM91UB","FLYING FLEA C6, STORM BLACK")
}

enum class Region(val regionName: String,val country : String){
    EU_FRANCE("EU","France")
}
