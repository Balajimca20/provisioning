package com.royalenfield.provisioning.feature.provisioning.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.royalenfield.provisioning.core.adb.AdbClient
import com.royalenfield.provisioning.core.adb.AdbResult
import com.royalenfield.provisioning.core.network.VehicleNetworkConnectionHelper
import com.royalenfield.provisioning.core.validation.SsidValidator
import com.royalenfield.provisioning.feature.dashboard.presentation.CurrentSSIDAndPasswordDetails
import com.royalenfield.provisioning.feature.dashboard.presentation.CurrentSSIDAndPasswordDetails.adbHostInput
import com.royalenfield.provisioning.feature.dashboard.presentation.CurrentSSIDAndPasswordDetails.adbPortInput
import com.royalenfield.provisioning.feature.provisioning.data.repository.ProvisioningRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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
    private val networkHelper: VehicleNetworkConnectionHelper,
) : ViewModel() {

    private val _provisioningUiState = MutableStateFlow(ProvisioningStateModel())
    val provisioningUiState: StateFlow<ProvisioningStateModel> = _provisioningUiState

    private val _status = MutableStateFlow<ProvisioningStatus>(ProvisioningStatus.Idle)
    val status: StateFlow<ProvisioningStatus> = _status.asStateFlow()

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    private val _requestAdbDialog = MutableSharedFlow<Boolean>(0)
    val requestAdbDialog = _requestAdbDialog.asSharedFlow()

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
        when (
            val result = adbClient.runShell(
                "cat /mnt/vendor/persist/c2c/c2c_vehicle.json"
            )
        ) {
            is AdbResult.Success -> {
                val output = result.data
               // log("📡 Telemetry JSON: $output")
                Log.e("TAG", "monitorTelemetryState: $output")

                // Parse and extract system_state from C2C_Vehicle object
                try {
                    val json = Json { ignoreUnknownKeys = true; isLenient = true }
                    val root = json.parseToJsonElement(output).jsonObject
                    val c2cVehicle = root["C2C_Vehicle"]?.jsonObject

                    val systemState  = c2cVehicle?.get("system_state")?.jsonPrimitive?.content
                    val guid         = c2cVehicle?.get("guid")?.jsonPrimitive?.content
                    val pkgVersion   = c2cVehicle?.get("pkg_version")?.jsonPrimitive?.content
                    val programId    = c2cVehicle?.get("program_id")?.jsonPrimitive?.content
                    val gblUrlCloud  = c2cVehicle?.get("gbl_url_cloud")?.jsonPrimitive?.content
                    val systemName   = c2cVehicle?.get("systemName")?.jsonPrimitive?.content

                    log("📊 system_state  : $systemState")
                    log("🔑 guid          : $guid")
                    log("📦 pkg_version   : $pkgVersion")
                    log("🆔 program_id    : $programId")
                    log("🌐 gbl_url_cloud : $gblUrlCloud")
                    log("👤 systemName    : $systemName")

                    if (systemState == "PRE_REGIONAL_ACTIVE") {
                        log("🏆 Target State [PRE_REGIONAL_ACTIVE] Detected!")
                    } else {
                        log("⏳ Current system_state: $systemState — waiting for PRE_REGIONAL_ACTIVE")
                    }
                } catch (e: Exception) {
                    log("⚠️ Failed to parse telemetry JSON: ${e.message}")
                    Log.e("TAG", "monitorTelemetryState parse error", e)
                }
            }

            is AdbResult.Failure -> {
                log("State updated : ${result.message}")
            }
        }

        delay(2000)
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

                clearingSharedPrefs()
                // 8. Reboot



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

    suspend fun clearingSharedPrefs(){
        log("🧹 Clearing shared prefs...")
        adbClient.runShell("rm -rf /data/vendor/c2c/shared_prefs/*")
        adbClient.runShell("sync")
        log("❓ WAITING FOR REBOOT CONSENT...")
        log("❓ Prompting operator user for hardware reboot consent..")

        _requestAdbDialog.emit(true)
    }

    fun onSendRebootConsent(concern: Boolean) {
        viewModelScope.launch {
            if (concern){
                log("Consent received. Requesting device hardware reboot...")
                log("🔄 Rebooting device...")
                _status.value = ProvisioningStatus.Running("Rebooting device", 95)
                adbClient.runShell("reboot")

                log("✅ Reboot initiated. waiting for wifi reconnection to ${CurrentSSIDAndPasswordDetails.ssidInput}...")

                connectWifi()
            }
        }

    }

    fun connectWifi() {
        networkHelper.connect(
            ssid = CurrentSSIDAndPasswordDetails.ssidInput,
            passphrase = CurrentSSIDAndPasswordDetails.passwordInput
        )
        connectAdb()
    }

    fun connectAdb() {
        viewModelScope.launch {
            val port = adbPortInput.toIntOrNull() ?: 5555
            when (val res = adbClient.connect(adbHostInput, port)) {
                is AdbResult.Success -> {
                    log("✅ ADB connected")

                    // 9. Monitor telemetry
                    log("🛰️ Monitoring Telemetry JSON...")
                    adbClient.restartAsRoot()
                    monitorTelemetryState()

                }

                is AdbResult.Failure -> {
                    log("❌ ADB connection failed: ${res.message}")
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
