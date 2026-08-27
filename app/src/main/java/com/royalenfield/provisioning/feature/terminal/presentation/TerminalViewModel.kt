package com.royalenfield.provisioning.feature.terminal.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.royalenfield.provisioning.core.adb.AdbClient
import com.royalenfield.provisioning.core.adb.AdbResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TerminalUiState(
    val commandInput: String = "su 0 id",
    val isExecuting: Boolean = false,
    val logs: List<String> = listOf(
        "RE FF Mechanic ADB Shell v1.0",
        "Type 'su 0 id' or preset commands below."
    )
)

class TerminalViewModel(
    private val adbClient: AdbClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(TerminalUiState())
    val uiState: StateFlow<TerminalUiState> = _uiState.asStateFlow()

    fun onCommandChanged(cmd: String) {
        _uiState.update { it.copy(commandInput = cmd) }
    }

    fun executeCommand(cmd: String = _uiState.value.commandInput) {
        val trimmed = cmd.trim()
        if (trimmed.isEmpty()) return

        viewModelScope.launch {
            addLog("$ $trimmed")
            _uiState.update { it.copy(isExecuting = true) }

            when (val res = adbClient.runShell(trimmed)) {
                is AdbResult.Success -> {
                    val lines = res.data.lines().filter { it.isNotBlank() }
                    lines.forEach { addLog(it) }
                    _uiState.update { it.copy(isExecuting = false, commandInput = "") }
                }
                is AdbResult.Failure -> {
                    addLog("ERR: ${res.message}")
                    _uiState.update { it.copy(isExecuting = false) }
                }
            }
        }
    }

    fun clearLogs() {
        _uiState.update { it.copy(logs = emptyList()) }
    }

    private fun addLog(line: String) {
        _uiState.update {
            val updated = it.logs.toMutableList()
            updated.add(line)
            it.copy(logs = updated)
        }
    }
}
