package com.royalenfield.ffmechanic.app.feature.wifi.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.royalenfield.ffmechanic.app.feature.wifi.domain.WifiStep
import com.royalenfield.ffmechanic.app.feature.wifi.domain.WifiUpdateWorkflow
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.security.SecureRandom
import javax.inject.Inject

data class WifiUiState(
    val vin: String = "",
    val generatedPassword: String = "",
    val targetHost: String = "192.168.1.1", // matches the hardcoded `adb connect 192.168.1.1`
    val isRunning: Boolean = false,
    val logLines: List<Pair<String, String>> = emptyList(),
) {
    val isVinValid: Boolean get() = vin.length == 17
}

@HiltViewModel
class WifiViewModel @Inject constructor(
    private val workflow: WifiUpdateWorkflow,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WifiUiState())
    val uiState: StateFlow<WifiUiState> = _uiState.asStateFlow()

    /** Port of handle_vin_input_parsing(): strip non-alphanumeric, uppercase, cap at 17. */
    fun onVinChanged(raw: String) {
        val cleaned = raw.filter { it.isLetterOrDigit() }.take(17).uppercase()
        _uiState.update { it.copy(vin = cleaned) }
    }

    fun onHostChanged(host: String) {
        _uiState.update { it.copy(targetHost = host) }
    }

    /** Port of generate_password(): 8 chars from letters + digits + a fixed special-char set. */
    fun generatePassword() {
        val specialChars = "!@#$%&[]{}"
        val alphabet = ('a'..'z') + ('A'..'Z') + ('0'..'9') + specialChars.toList()
        val random = SecureRandom()
        val pwd = (1..8).map { alphabet[random.nextInt(alphabet.size)] }.joinToString("")
        _uiState.update { it.copy(generatedPassword = pwd) }
        log("New 8-character password generated: $pwd")
    }

    /** Port of start_wifi_change_process() + on_process_finished(). */
    fun startWifiChangeProcess() {
        val state = _uiState.value
        if (!state.isVinValid) {
            log("Validation Failed: VIN length is not 17 characters.", "red")
            return
        }
        if (state.generatedPassword.isBlank()) {
            log("Validation Failed: Password field is empty.", "red")
            return
        }
        if (state.isRunning) return

        _uiState.update { it.copy(isRunning = true) }

        viewModelScope.launch {
            var success = false
            workflow.run(state.targetHost, state.vin, state.generatedPassword, context.cacheDir).collect { step ->
                when (step) {
                    is WifiStep.Log -> log(step.message, step.color)
                    is WifiStep.Done -> success = step.success
                }
            }
            _uiState.update { it.copy(isRunning = false) }
            log(if (success) "Wi-Fi password updated successfully! Target device rebooting..." else
                "Wi-Fi configuration change failed. Review the log above for details.", if (success) "green" else "red")
        }
    }

    private fun log(message: String, color: String = "black") {
        _uiState.update { it.copy(logLines = it.logLines + (message to color)) }
    }
}
