package com.royalenfield.provisioning.feature.dashboard.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.royalenfield.provisioning.core.adb.AdbClient
import com.royalenfield.provisioning.core.adb.AdbManager
import com.royalenfield.provisioning.core.adb.AdbResult
import com.royalenfield.provisioning.core.network.VehicleNetworkConnectionHelper
import com.royalenfield.provisioning.core.network.VehicleNetworkState
import com.royalenfield.provisioning.core.validation.SsidValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val networkHelper: VehicleNetworkConnectionHelper,
    private val adbClient: AdbClient,
    private val adbManager: AdbManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        // Initial SSID validation
        validateSsid(_uiState.value.ssidInput)

        // Observe network state flow
        viewModelScope.launch {
            networkHelper.networkState.collect { state ->
                when (state) {
                    is VehicleNetworkState.Connecting -> {
                        _uiState.update {
                            it.copy(
                                isWifiConnecting = true,
                                wifiErrorMessage = null
                            )
                        }
                    }
                    is VehicleNetworkState.Connected -> {
                        _uiState.update {
                            it.copy(
                                isWifiConnecting = false,
                                isWifiConnected = true,
                                connectedSsid = state.ssid,
                                isProcessBound = state.isBound,
                                wifiErrorMessage = null
                            )
                        }
                    }
                    is VehicleNetworkState.Disconnected -> {
                        _uiState.update {
                            it.copy(
                                isWifiConnecting = false,
                                isWifiConnected = false,
                                connectedSsid = null,
                                isProcessBound = false
                            )
                        }
                    }
                    is VehicleNetworkState.Failed -> {
                        _uiState.update {
                            it.copy(
                                isWifiConnecting = false,
                                isWifiConnected = false,
                                wifiErrorMessage = state.error
                            )
                        }
                    }
                }
            }
        }
    }

    fun startSetup() {
        _uiState.update { it.copy(isSetupStarted = true) }
    }

    fun onSsidChanged(newSsid: String) {
        val sanitized = SsidValidator.sanitize(newSsid)
        _uiState.update { it.copy(ssidInput = sanitized) }
        validateSsid(sanitized)
    }

    fun onPasswordChanged(newPassword: String) {
        _uiState.update { it.copy(passwordInput = newPassword) }
    }

    private fun validateSsid(ssid: String) {
        val error = SsidValidator.getValidationError(ssid)
        _uiState.update { it.copy(ssidValidationError = error) }
    }

    fun connectWifi() {
        val currentState = _uiState.value
        val error = SsidValidator.getValidationError(currentState.ssidInput)
        if (error != null) {
            _uiState.update { it.copy(ssidValidationError = error) }
            return
        }

        networkHelper.connect(
            ssid = currentState.ssidInput,
            passphrase = currentState.passwordInput
        )
    }

    fun disconnectWifi() {
        networkHelper.disconnect()
        disconnectAdb()
    }

    fun onAdbHostChanged(host: String) {
        _uiState.update { it.copy(adbHostInput = host) }
    }

    fun onAdbPortChanged(port: String) {
        _uiState.update { it.copy(adbPortInput = port) }
    }

    fun connectAdb() {
        val currentState = _uiState.value
        val port = currentState.adbPortInput.toIntOrNull() ?: 5555

        viewModelScope.launch {
            _uiState.update { it.copy(isAdbConnecting = true, adbErrorMessage = null) }

            when (val res = adbClient.connect(currentState.adbHostInput, port)) {
                is AdbResult.Success -> {
                    val isRooted = adbManager.verifyRootAccess()
                    _uiState.update {
                        it.copy(
                            isAdbConnecting = false,
                            isAdbConnected = true,
                            isAdbRooted = isRooted,
                            adbErrorMessage = null
                        )
                    }
                }
                is AdbResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isAdbConnecting = false,
                            isAdbConnected = false,
                            adbErrorMessage = res.message
                        )
                    }
                }
            }
        }
    }

    fun disconnectAdb() {
        adbClient.disconnect()
        _uiState.update {
            it.copy(
                isAdbConnected = false,
                isAdbRooted = false
            )
        }
    }

    fun rebootVehicle() {
        viewModelScope.launch {
            adbManager.rebootDevice()
        }
    }
}
