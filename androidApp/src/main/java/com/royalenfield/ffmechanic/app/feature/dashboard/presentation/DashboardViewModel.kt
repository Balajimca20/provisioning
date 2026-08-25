package com.royalenfield.ffmechanic.app.feature.dashboard.presentation

import android.content.Context
import android.net.wifi.SupplicantState
import android.net.wifi.WifiManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.royalenfield.ffmechanic.app.core.adb.AdbClient
import com.royalenfield.ffmechanic.app.core.adb.AdbResult
import com.royalenfield.ffmechanic.app.core.network.VehicleNetworkConnectionHelper
import com.royalenfield.ffmechanic.app.core.validation.SsidValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val deviceHost: String = "192.168.1.1",
    val wifiSsid: String = "",
    val wifiPassword: String = "",
    val isWifiConnected: Boolean = false,
    val isConnectingWifi: Boolean = false,
    val wifiStatus: String = "Not Connected",
    val wifiValidationError: String? = null,
    val isAdbConnected: Boolean = false,
    val isConnectingAdb: Boolean = false,
    val adbStatus: String = "Not connected",
) {
    val canOpenFeatures: Boolean get() = isWifiConnected && isAdbConnected
    val canConnectWifi: Boolean get() = !isConnectingWifi && 
        wifiSsid.isNotBlank() && 
        wifiPassword.isNotBlank() &&
        wifiValidationError == null
    val isSsidValid: Boolean get() = SsidValidator.isValidVehicleSsid(wifiSsid)
}

private data class WifiConnectionSnapshot(
    val isAssociated: Boolean,
    val isVehicleConnected: Boolean,
    val ssid: String,
    val status: String,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val adbClient: AdbClient,
    private val networkHelper: VehicleNetworkConnectionHelper,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val appPrefs by lazy {
        context.getSharedPreferences("ffm_prefs", Context.MODE_PRIVATE)
    }

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    /** Fires once when the Connect button is pressed and the phone isn't yet on Wi-Fi,
     *  telling the screen to open Android Wi-Fi settings so the user can connect manually. */
    private val _openWifiSettingsEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val openWifiSettingsEvent: SharedFlow<Unit> = _openWifiSettingsEvent.asSharedFlow()

    private var wifiPollJob: Job? = null

    init {
        refreshWifiStatus()
    }

    fun onHostChanged(host: String) {
        _uiState.update { it.copy(deviceHost = host) }
    }

    fun onWifiPasswordChanged(password: String) {
        _uiState.update { it.copy(wifiPassword = password) }
    }

    fun onWifiSsidChanged(ssid: String) {
        val validationError = SsidValidator.getValidationError(ssid)
        _uiState.update { it.copy(wifiSsid = ssid, wifiValidationError = validationError) }
    }

    fun refreshWifiStatus() {
        val snapshot = resolveWifiConnection()
        persistVehicleWifiSession(snapshot.isVehicleConnected, snapshot.ssid)
        _uiState.update { state ->
            state.copy(
                isWifiConnected = snapshot.isVehicleConnected,
                wifiSsid = if (snapshot.isVehicleConnected) snapshot.ssid else state.wifiSsid,
                wifiStatus = snapshot.status,
                isAdbConnected = if (snapshot.isVehicleConnected) state.isAdbConnected else false,
                adbStatus = if (snapshot.isVehicleConnected) state.adbStatus else "Wi-Fi disconnected",
            )
        }
    }

    fun connectWifi() {
        val state = _uiState.value
        if (state.wifiSsid.isBlank() || state.wifiPassword.isBlank() || state.isConnectingWifi) return
        if (!state.isSsidValid) {
            _uiState.update { it.copy(wifiStatus = "Invalid SSID format (must be RE_XXXX_XXXXXX)") }
            return
        }

        val currentConnection = resolveWifiConnection()
        if (currentConnection.isVehicleConnected) {
            // Already on Wi-Fi — initiate network helper binding and accept immediately
            val cleanedSsid = currentConnection.ssid
            if (SsidValidator.isValidVehicleSsid(cleanedSsid)) {
                if (networkHelper.connect(cleanedSsid, state.wifiPassword)) {
                    _uiState.update {
                        it.copy(
                            isWifiConnected = true,
                            wifiStatus = "Connected to $cleanedSsid",
                        )
                    }
                    persistVehicleWifiSession(true, cleanedSsid)
                    return
                }
            }
        }

        // Not connected — attempt to connect using NetworkHelper
        _uiState.update {
            it.copy(
                isConnectingWifi = true,
                wifiStatus = "Connecting to ${state.wifiSsid}...",
            )
        }

        val cleanedSsid = SsidValidator.sanitize(state.wifiSsid)
        if (!networkHelper.connect(cleanedSsid, state.wifiPassword)) {
            _uiState.update {
                it.copy(
                    isConnectingWifi = false,
                    wifiStatus = "Failed to initiate connection",
                )
            }
            return
        }

        // Poll for connection completion
        startWifiPolling(cleanedSsid)
    }

    fun cancelWifiConnect() {
        wifiPollJob?.cancel()
        _uiState.update { it.copy(isConnectingWifi = false, wifiStatus = "Cancelled") }
    }

    fun disconnectWifi() {
        wifiPollJob?.cancel()
        networkHelper.disconnect()
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiManager.disconnect()
        persistVehicleWifiSession(false, "")
        _uiState.update {
            it.copy(
                isConnectingWifi = false,
                isWifiConnected = false,
                wifiStatus = "Not Connected",
                isAdbConnected = false,
                adbStatus = "Wi-Fi disconnected",
            )
        }
    }

    /** Polls every second for up to 30 s until Wi-Fi association is detected. */
    private fun startWifiPolling(targetSsid: String) {
        wifiPollJob?.cancel()
        wifiPollJob = viewModelScope.launch {
            repeat(30) {
                delay(1_000L)
                val snapshot = resolveWifiConnection()
                if (snapshot.isVehicleConnected && snapshot.ssid.equals(targetSsid, ignoreCase = true)) {
                    persistVehicleWifiSession(true, snapshot.ssid)
                    _uiState.update { s ->
                        s.copy(
                            isConnectingWifi = false,
                            isWifiConnected = true,
                            wifiStatus = snapshot.status,
                            wifiSsid = snapshot.ssid,
                        )
                    }
                    return@launch
                }

                if (snapshot.isAssociated && !snapshot.isVehicleConnected) {
                    _uiState.update {
                        it.copy(
                            isWifiConnected = false,
                            wifiStatus = snapshot.status,
                        )
                    }
                }
            }
            // Timed out.
            _uiState.update {
                it.copy(
                    isConnectingWifi = false,
                    wifiStatus = if (it.wifiStatus == "Invalid Vehicle SSID") it.wifiStatus else "Not Connected",
                )
            }
        }
    }

    fun connectAdb() {
        val state = _uiState.value
        if (!state.isWifiConnected) {
            _uiState.update { it.copy(adbStatus = "Connect device to Wi-Fi first") }
            return
        }
        if (state.deviceHost.isBlank() || state.isConnectingAdb) return

        _uiState.update { it.copy(isConnectingAdb = true, adbStatus = "Connecting ADB...") }
        viewModelScope.launch {
            when (val result = adbClient.connect(state.deviceHost.trim())) {
                is AdbResult.Success -> _uiState.update {
                    it.copy(
                        isConnectingAdb = false,
                        isAdbConnected = true,
                        adbStatus = "ADB connected to ${state.deviceHost.trim()}",
                    )
                }
                is AdbResult.Failure -> _uiState.update {
                    it.copy(
                        isConnectingAdb = false,
                        isAdbConnected = false,
                        adbStatus = result.message,
                    )
                }
            }
        }
    }

    /**
     * Detects Wi-Fi association using SupplicantState — works for hotspots with no internet
     * (e.g. vehicle APs at 192.168.1.1) where ConnectivityManager.activeNetwork would be null.
     * Also works when networkId == -1 (e.g. some OEM ROMs for suggestion-managed networks).
     */
    @Suppress("DEPRECATION")
    private fun resolveWifiConnection(): WifiConnectionSnapshot {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (!wifiManager.isWifiEnabled) {
            return WifiConnectionSnapshot(
                isAssociated = false,
                isVehicleConnected = false,
                ssid = "",
                status = "Wi-Fi disabled",
            )
        }
        val info = wifiManager.connectionInfo ?: return WifiConnectionSnapshot(
            isAssociated = false,
            isVehicleConnected = false,
            ssid = "",
            status = "Not Connected",
        )

        // COMPLETED = 4-way handshake done and associated; also accept ASSOCIATED as "connecting".
        // This works regardless of internet availability or networkId value.
        val associated = info.supplicantState == SupplicantState.COMPLETED ||
            info.supplicantState == SupplicantState.FOUR_WAY_HANDSHAKE ||
            info.supplicantState == SupplicantState.GROUP_HANDSHAKE ||
            (info.networkId != -1 && info.ipAddress != 0)

        if (!associated) {
            return WifiConnectionSnapshot(
                isAssociated = false,
                isVehicleConnected = false,
                ssid = "",
                status = "Not Connected",
            )
        }

        val ssid = SsidValidator.sanitize(info.ssid)
        if (ssid.isBlank() || ssid.equals("<unknown ssid>", ignoreCase = true)) {
            return WifiConnectionSnapshot(
                isAssociated = true,
                isVehicleConnected = false,
                ssid = "",
                status = "Invalid Vehicle SSID",
            )
        }

        val isVehicleSsid = SsidValidator.isValidVehicleSsid(ssid)
        return WifiConnectionSnapshot(
            isAssociated = true,
            isVehicleConnected = isVehicleSsid,
            ssid = ssid,
            status = if (isVehicleSsid) "Connected to $ssid" else "Invalid Vehicle SSID",
        )
    }

    private fun persistVehicleWifiSession(isConnected: Boolean, ssid: String) {
        val isVehicleWifi = isConnected && SsidValidator.isValidVehicleSsid(ssid)
        appPrefs.edit()
            .putBoolean("vehicle_wifi_connected", isVehicleWifi)
            .putString("vehicle_wifi_ssid", if (isVehicleWifi) ssid else "")
            .apply()
    }
}
