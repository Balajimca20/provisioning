package com.royalenfield.ffmechanic.app.feature.wifi.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun WifiScreen(viewModel: WifiViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Automotive Wi-Fi Password Manager", style = MaterialTheme.typography.titleLarge)

        // Vehicle identification
        ElevatedCard {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Vehicle Identification", style = MaterialTheme.typography.titleSmall)
                OutlinedTextField(
                    value = state.vin,
                    onValueChange = viewModel::onVinChanged,
                    label = { Text("VIN Scanner Input") },
                    placeholder = { Text("Enter or scan 17-character VIN") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.targetHost,
                    onValueChange = viewModel::onHostChanged,
                    label = { Text("Target device host (network ADB)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // Password generation
        ElevatedCard {
            Row(
                Modifier.padding(12.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(onClick = viewModel::generatePassword) { Text("Generate New Password") }
                OutlinedTextField(
                    value = state.generatedPassword,
                    onValueChange = {},
                    readOnly = true,
                    placeholder = { Text("Click to generate (8 chars)") },
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace),
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // Main action
        Button(
            onClick = viewModel::startWifiChangeProcess,
            enabled = !state.isRunning,
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
            Text(if (state.isRunning) "Running…" else "Change the WIFI password")
        }

        // Log console
        Text("Execution Log", style = MaterialTheme.typography.titleSmall)
        Surface(color = Color(0xFF1E1E1E), shape = RoundedCornerShape(4.dp)) {
            Column(
                Modifier
                    .heightIn(min = 150.dp, max = 300.dp)
                    .padding(8.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                state.logLines.forEach { (msg, color) ->
                    Text(msg, color = logColor(color), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

private fun logColor(name: String): Color = when (name) {
    "red" -> Color(0xFFF44747)
    "green" -> Color(0xFF4EC9B0)
    "orange" -> Color(0xFFCE9178)
    "gray" -> Color(0xFF808080)
    else -> Color(0xFFD4D4D4)
}
