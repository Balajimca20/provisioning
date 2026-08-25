package com.royalenfield.provisioning.feature.wifi.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.royalenfield.provisioning.core.adb.AdbManager
import com.royalenfield.provisioning.core.validation.SsidValidator
import com.royalenfield.provisioning.feature.wifi.data.WifiChangeLogRepository
import com.royalenfield.provisioning.feature.wifi.domain.WifiUpdateWorkflow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WifiViewModel(
    private val workflow: WifiUpdateWorkflow,
    private val logRepository: WifiChangeLogRepository,
    private val adbManager: AdbManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(WifiUiState())
    val uiState: StateFlow<WifiUiState> = _uiState.asStateFlow()

    init {
        validateSsid(_uiState.value.newSsidInput)
        loadAuditLogs()
        loadRawXml()
    }

    fun onSsidChanged(newSsid: String) {
        val sanitized = SsidValidator.sanitize(newSsid)
        _uiState.update { it.copy(newSsidInput = sanitized) }
        validateSsid(sanitized)
    }

    fun onPassphraseChanged(newPass: String) {
        _uiState.update { it.copy(newPassphraseInput = newPass) }
    }

    private fun validateSsid(ssid: String) {
        val error = SsidValidator.getValidationError(ssid)
        _uiState.update { it.copy(validationError = error) }
    }

    fun loadAuditLogs() {
        viewModelScope.launch {
            val logs = logRepository.getLogs()
            _uiState.update { it.copy(auditLogs = logs) }
        }
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
                        rawXmlContent = "<!-- SoftAP XML: Run ADB read or connect device to inspect -->\n<WifiConfigStoreSoftAp>\n  <SoftApConfiguration>\n    <string name=\"SSID\">&quot;${_uiState.value.currentSsid}&quot;</string>\n    <int name=\"SecurityType\" value=\"1\" />\n    <boolean name=\"Hidden\" value=\"false\" />\n  </SoftApConfiguration>\n</WifiConfigStoreSoftAp>",
                        isReadingXml = false
                    )
                }
            }
        }
    }

    fun executeWorkflow() {
        val state = _uiState.value
        val validation = SsidValidator.getValidationError(state.newSsidInput)
        if (validation != null) {
            _uiState.update { it.copy(validationError = validation) }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isUpdating = true,
                    updateSuccess = false,
                    errorMessage = null,
                    progressPercent = 0
                )
            }

            workflow.execute(
                oldSsid = state.currentSsid,
                newSsid = state.newSsidInput,
                newPassphrase = state.newPassphraseInput
            ).collect { progress ->
                _uiState.update {
                    it.copy(
                        currentStep = progress.step,
                        progressPercent = progress.percent,
                        updateSuccess = progress.isComplete,
                        isUpdating = !progress.isComplete && progress.error == null,
                        errorMessage = progress.error,
                        currentSsid = if (progress.isComplete) state.newSsidInput else it.currentSsid
                    )
                }
            }

            loadAuditLogs()
            loadRawXml()
        }
    }
}
