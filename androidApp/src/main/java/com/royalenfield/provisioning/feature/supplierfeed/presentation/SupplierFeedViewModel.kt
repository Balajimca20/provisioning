package com.royalenfield.provisioning.feature.supplierfeed.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.royalenfield.provisioning.feature.supplierfeed.domain.FetchTelemetryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SupplierFeedViewModel(
    private val fetchTelemetryUseCase: FetchTelemetryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SupplierFeedUiState())
    val uiState: StateFlow<SupplierFeedUiState> = _uiState.asStateFlow()

    init {
        updateQueryPreview(_uiState.value.serialNumberInput)
        fetchTelemetry()
    }

    fun onSerialNumberChanged(serial: String) {
        _uiState.update { it.copy(serialNumberInput = serial) }
        updateQueryPreview(serial)
    }

    private fun updateQueryPreview(serial: String) {
        val q = """
            query GetDeviceTelemetry {
              getDevice(serialNumber: "$serial") {
                vin
                model
                ecuHardwareRev
                tcuImei
                firmwareVersion
                batteryVoltage
                odometerKm
                canBusHealth
              }
            }
        """.trimIndent()
        _uiState.update { it.copy(rawGraphQLQuery = q) }
    }

    fun fetchTelemetry() {
        val serial = _uiState.value.serialNumberInput.trim()
        if (serial.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val result = fetchTelemetryUseCase(serial)
            result.onSuccess { data ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        telemetry = data,
                        errorMessage = null
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = err.message ?: "Failed to query GraphQL telemetry"
                    )
                }
            }
        }
    }
}
