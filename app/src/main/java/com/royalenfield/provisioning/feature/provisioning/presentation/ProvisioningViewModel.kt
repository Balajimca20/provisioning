package com.royalenfield.provisioning.feature.provisioning.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.royalenfield.provisioning.core.adb.AdbClient
import com.royalenfield.provisioning.core.adb.AdbResult
import com.royalenfield.provisioning.feature.provisioning.data.repository.ProvisioningRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
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

class ProvisioningViewModel(
    private val repository: ProvisioningRepository,
    private val adbClient: AdbClient,
) : ViewModel() {

    private val _provisioningUiState = MutableStateFlow(ProvisioningStateModel())
    val provisioningUiState: StateFlow<ProvisioningStateModel> = _provisioningUiState

    private val _status = MutableStateFlow<ProvisioningStatus>(ProvisioningStatus.Idle)
    val status: StateFlow<ProvisioningStatus> = _status.asStateFlow()

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    private var provisioningJob: Job? = null

    /**
     * Stops the ongoing provisioning process by cancelling the coroutine job
     * and resetting the status to Idle.
     */
    fun stopProvisioning() {
        if (provisioningJob?.isActive == true) {
            provisioningJob?.cancel()
            provisioningJob = null
            _status.value = ProvisioningStatus.Idle
            log("⛔ Provisioning process terminated by user.")
            
            // Optional: Restart the hub service if it was stopped during provisioning
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    adbClient.runShell("start c2c_hub_service")
                } catch (e: Exception) {
                    // Ignore cleanup errors
                }
            }
        }
    }

    private suspend fun monitorTelemetryState() {
        log("🔍 Monitoring telemetry state...")

        while (true) {
            when (
                val result = adbClient.runShell(
                    "cat /mnt/vendor/persist/c2c/c2c_vehicle.json"
                )
            ) {
                is AdbResult.Success -> {
                    val output = result.data
                    log("📡 Telemetry JSON: $output")

                    if (output.contains("PRE_REGIONAL_ACTIVE")) {
                        log("🏆 Target State [PRE_REGIONAL_ACTIVE] Detected!")
                        break
                    }
                }

                is AdbResult.Failure -> {
                    log("❌ Failed to read telemetry state: ${result}")
                }
            }

            delay(2000) // Suspension point that checks for cancellation
        }

        log("🛑 Telemetry monitoring stopped")
    }


    private fun log(message: String) {
        _logs.value += "[${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(
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

    fun startProvisioning(
        ip: String,
        payloadFiles: List<File>
    ) {
        provisioningJob?.cancel()
        provisioningJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Check connection
                log("🔌 Checking ADB connection...")
                if (!adbClient.isConnected) {
                    throw Exception("ADB device is not connected")
                }

                // 2. Acquire root
                log("🔓 Acquiring Root Access...")
                _status.value = ProvisioningStatus.Running("Acquiring Root Access", 5)

                val rootResult = adbClient.restartAsRoot()
                when(rootResult){
                    is AdbResult.Failure -> {
                        log("❌ Root escalation failed: ${rootResult.message}")
                        Log.e("TAG", "startProvisioning: Root Failed ${(rootResult as AdbResult.Failure).message}")
                    }
                    is AdbResult.Success -> {
                        log("✅ Root confirmed: ${(rootResult as AdbResult.Success).data}")
                        Log.e("TAG", "startProvisioning: Root Success ${(rootResult as AdbResult.Success).data}")
                    }
                }

            //    adbClient.runShell("root")

                // 3. Stop C2C service
                log("🛑 Stopping target hub services...")
                _status.value = ProvisioningStatus.Running("Stopping target services", 10)
                adbClient.runShell("stop c2c_hub_service")

                // 4. Register cloud metadata
                log("☁️ Registering Cloud Metadata...")
                _status.value = ProvisioningStatus.Running("Registering Cloud Metadata", 20)
                val isRegistered = repository.registerVehicleMetadata(
                    vin = _provisioningUiState.value.vinNumber,
                    modelCode = _provisioningUiState.value.selectedVariant.modelCode,
                    modelDesc = _provisioningUiState.value.selectedVariant.description,
                    region = _provisioningUiState.value.selectedRegion.regionName,
                    country = _provisioningUiState.value.selectedRegion.country
                ).getOrDefault(false)

                if (!isRegistered) {
                    log("⚠️ Cloud registration returned unexpected status")
                }

                // 5. Clear telemetry buffer
                log("🧹 Clearing target buffers...")
                _status.value = ProvisioningStatus.Running("Clearing target buffers", 30)
                adbClient.runShell("rm -rf /data/vendor/c2c/tele_buff/*")
                adbClient.runShell("rm -rf /mnt/vendor/persist/c2c/RFF/READY/*")

                // 6. Push payload files
                log("📂 Pushing ${payloadFiles.size} payload files...")
                pushPayloadFiles(payloadFiles)

                // 7. Validate payload files
                log("🔍 Validating pushed files...")
                _status.value = ProvisioningStatus.Running("Validating payload files", 90)
                validatePayloadFiles(payloadFiles)

                // 8. Reboot
                log("🔄 Rebooting device...")
                _status.value = ProvisioningStatus.Running("Rebooting device", 95)
                adbClient.runShell("reboot")

                // 9. Monitor telemetry
                log("🛰️ Monitoring Telemetry JSON...")
                monitorTelemetryState()

                // 10. Success
                _status.value = ProvisioningStatus.Success("Provisioning completed successfully!")

            } catch (e: Exception) {
                if (e is CancellationException) {
                    log("⛔ Provisioning cancelled.")
                } else {
                    log("❌ Provisioning failed: ${e.message}")
                    _status.value = ProvisioningStatus.Error(e.message ?: "Execution failed")
                }
            }
        }
    }

    private suspend fun pushPayloadFiles(payloadFiles: List<File>) {
        payloadFiles.forEachIndexed { index, file ->
            yield() // Check for cancellation before each file push

            Log.e("PayLoadFiles", "pushPayloadFiles: ${file.name} -- Index : ${index}")
            val adbPush = adbClient.push(file,"/mnt/vendor/persist/c2c/${file.name}")

            when(adbPush){
                is AdbResult.Failure -> {
                    Log.e("TAG", "pushPayloadFiles: ${adbPush.message}")
                }
                is AdbResult.Success -> {
                    Log.e("TAG", "pushPayloadFiles: ${adbPush.data}")
                }
            }

//            adbClient.pushFile(file, "/mnt/vendor/persist/c2c/${file.name}") { sent, total ->
//                val overallSent = fileStartBytes + sent
//                val uploadPercent = if (totalBytes > 0) (overallSent * 100 / totalBytes).toInt() else 0
//
//                // Upload represents 30% -> 85% range in the UI
//                val progress = 30 + (uploadPercent * 55 / 100)
//                _status.value = ProvisioningStatus.Running(
//                    "Pushing ${index + 1}/${payloadFiles.size}: ${file.name}",
//                    progress
//                )
//            }
//
//            uploadedBytes += file.length()
            log("✅ Pushed ${file.name}")
        }
    }

    private suspend fun validatePayloadFiles(payloadFiles: List<File>) {
        payloadFiles.forEachIndexed { index, file ->
            yield() // Check for cancellation
            val remotePath = "/mnt/vendor/persist/c2c/${file.name}"
            log("🔍 Checking ${index + 1}/${payloadFiles.size}: ${file.name}")

            val result = adbClient.runShell("stat -c %s \"$remotePath\"")

            when(result){
                is AdbResult.Failure -> {
                    throw Exception("Failed to validate ${file.name}")
                }
                is AdbResult.Success -> {
                    val remoteSize = result.data.trim().toLongOrNull()
                    if (remoteSize == null || remoteSize != file.length()) {
                        throw Exception("Size mismatch for ${file.name}: local=${file.length()}, remote=$remoteSize")
                    }
                    log("✅ ${file.name} validated")
                }
            }
        }
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
