package com.royalenfield.ffmechanic.app.feature.ota.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.royalenfield.ffmechanic.app.feature.ota.domain.OtaPipeline
import com.royalenfield.ffmechanic.app.feature.ota.domain.OtaStep
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class OtaUiState(
    val targetHost: String = "192.168.1.1",
    val zipPath: String = "",
    val isRunning: Boolean = false,
    val progress: Int = 0,
    val statusMessage: String = "",
    val awaitingRebootConsent: Boolean = false,
    val logLines: List<String> = emptyList(),
)

@HiltViewModel
class OtaViewModel @Inject constructor(
    private val pipeline: OtaPipeline,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OtaUiState())
    val uiState: StateFlow<OtaUiState> = _uiState.asStateFlow()

    fun onHostChanged(host: String) = _uiState.update { it.copy(targetHost = host) }
    fun onZipPathChanged(path: String) = _uiState.update { it.copy(zipPath = path) }

    /** Port of handle_ota_action() -> execute_ota_pipeline(). */
    fun startOta() {
        val state = _uiState.value
        if (state.zipPath.isBlank() || state.isRunning) return

        _uiState.update { it.copy(isRunning = true, progress = 0, awaitingRebootConsent = false, logLines = emptyList()) }

        viewModelScope.launch {
            pipeline.run(state.targetHost, File(state.zipPath)).collect { step ->
                when (step) {
                    is OtaStep.Log -> _uiState.update { it.copy(logLines = it.logLines + step.message) }
                    is OtaStep.Progress -> _uiState.update { it.copy(progress = step.percent, statusMessage = step.statusMessage) }
                    is OtaStep.AwaitingRebootConsent -> _uiState.update { it.copy(awaitingRebootConsent = true) }
                    is OtaStep.Done -> _uiState.update {
                        it.copy(isRunning = false, logLines = it.logLines + step.message)
                    }
                }
            }
        }
    }

    /** User tapped "Reboot now" on the consent prompt — port of _on_request_ota_reboot_consent(Yes). */
    fun confirmReboot() {
        _uiState.update { it.copy(awaitingRebootConsent = false) }
        viewModelScope.launch {
            val result = pipeline.confirmReboot()
            _uiState.update { it.copy(isRunning = false, logLines = it.logLines + (result as OtaStep.Done).message) }
        }
    }

    /** User tapped "Skip reboot" — port of the No branch. */
    fun skipReboot() {
        val result = pipeline.skipReboot()
        _uiState.update {
            it.copy(
                awaitingRebootConsent = false,
                isRunning = false,
                logLines = it.logLines + (result as OtaStep.Done).message,
            )
        }
    }
}
