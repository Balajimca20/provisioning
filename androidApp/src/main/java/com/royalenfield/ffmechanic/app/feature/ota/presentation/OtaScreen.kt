package com.royalenfield.ffmechanic.app.feature.ota.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun OtaScreen(viewModel: OtaViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Command Line OTA", style = MaterialTheme.typography.titleLarge)

        OutlinedTextField(
            value = state.targetHost,
            onValueChange = viewModel::onHostChanged,
            label = { Text("Target device host (network ADB)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = state.zipPath,
            onValueChange = viewModel::onZipPathChanged,
            label = { Text("OTA package (.zip) path") },
            placeholder = { Text("Use a file picker (Storage Access Framework) to fill this in") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        // TODO: wire an ActivityResultContracts.OpenDocument launcher to pick the .zip and
        // copy it to a local File this pipeline can read, replacing browse_ota_file()'s
        // QFileDialog.getOpenFileName call.

        Button(
            onClick = viewModel::startOta,
            enabled = !state.isRunning && state.zipPath.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
            Text(if (state.isRunning) "Running…" else "Start OTA Update")
        }

        if (state.isRunning || state.progress > 0) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(state.statusMessage, style = MaterialTheme.typography.labelLarge)
                LinearProgressIndicator(
                    progress = { state.progress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Text("Log", style = MaterialTheme.typography.titleSmall)
        Surface(color = Color(0xFF1E1E1E), shape = RoundedCornerShape(4.dp)) {
            Column(
                Modifier.weight(1f).padding(8.dp).verticalScroll(rememberScrollState()),
            ) {
                state.logLines.forEach { line ->
                    Text(line, color = Color(0xFFD4D4D4), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }

    // Port of _on_request_ota_reboot_consent()'s QMessageBox.question.
    if (state.awaitingRebootConsent) {
        AlertDialog(
            onDismissRequest = { /* force an explicit choice, same as the modal QMessageBox */ },
            title = { Text("OTA Applied") },
            text = { Text("The update has been staged. Reboot the device now to apply it?") },
            confirmButton = { TextButton(onClick = viewModel::confirmReboot) { Text("Reboot now") } },
            dismissButton = { TextButton(onClick = viewModel::skipReboot) { Text("Skip reboot") } },
        )
    }
}
