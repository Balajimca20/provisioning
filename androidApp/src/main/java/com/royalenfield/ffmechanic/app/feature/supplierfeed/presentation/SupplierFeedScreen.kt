package com.royalenfield.ffmechanic.app.feature.supplierfeed.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.royalenfield.ffmechanic.app.feature.supplierfeed.domain.FieldCategories

private val BgDark = Color(0xFF121824)
private val Cyan = Color(0xFF00E5FF)
private val FieldBg = Color(0xFF0D1A2A)
private val Border = Color(0xFF2A5A8A)

@Composable
fun SupplierFeedScreen(viewModel: SupplierFeedViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Top lookup & action bar
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("ICCID:", color = Cyan, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = state.iccidInput,
                onValueChange = viewModel::onIccidChanged,
                placeholder = { Text("Enter or paste 19/20-digit ICCID") },
                singleLine = true,
                modifier = Modifier.weight(2f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Cyan, unfocusedTextColor = Cyan,
                    focusedContainerColor = FieldBg, unfocusedContainerColor = FieldBg,
                    focusedBorderColor = Border, unfocusedBorderColor = Border,
                ),
            )
            Button(onClick = viewModel::fetchDevice, enabled = !state.isLoading) {
                Text(if (state.isLoading) "⏳ Fetching…" else "🔍 Fetch Device")
            }
            OutlinedButton(onClick = viewModel::clearForm) { Text("🧹 Clear Form") }
        }

        // Status banner
        val bannerText = when (val b = state.banner) {
            is BannerState.Idle -> "ℹ️ No device loaded. Enter an ICCID above and click 'Fetch Device'."
            is BannerState.NotFound -> "⚠️ Device not found in Cloud Registry."
            is BannerState.Loaded -> "✅ Loaded ICCID: ${b.iccid} | Model: ${b.model} | Status: ${b.status}"
        }
        Surface(color = FieldBg, shape = RoundedCornerShape(4.dp)) {
            Text(bannerText, color = Cyan, modifier = Modifier.padding(8.dp))
        }

        // Categorized fields, scrollable — mirrors the QScrollArea + QGridLayout per category
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FieldCategories.sections.forEach { (title, fields) ->
                Surface(
                    color = Color(0xFF0B121C),
                    shape = RoundedCornerShape(6.dp),
                ) {
                    Column(Modifier.padding(10.dp)) {
                        Text(title, color = Cyan, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        fields.chunked(2).forEach { pair ->
                            Row(horizontalArrangement = Arrangement.spacedBy(15.dp)) {
                                pair.forEach { field ->
                                    Column(Modifier.weight(1f)) {
                                        Text(field.label, color = Color(0xFF88A0C0), fontWeight = FontWeight.Bold)
                                        SelectionContainer {
                                            Text(
                                                state.device?.let { field.getter(it) } ?: "",
                                                color = Cyan,
                                                fontFamily = FontFamily.Monospace,
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                }
            }
        }

        // Log console
        Surface(color = Color(0xFF080D14), shape = RoundedCornerShape(4.dp)) {
            Column(
                Modifier
                    .heightIn(max = 90.dp)
                    .padding(6.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                state.logLines.forEach { (msg, color) ->
                    Text(msg, color = parseLogColor(color), fontFamily = FontFamily.Monospace, fontSize = androidx.compose.ui.unit.TextUnit.Unspecified)
                }
            }
        }
    }
}

private fun parseLogColor(name: String): Color = when (name) {
    "red" -> Color.Red
    "orange" -> Color(0xFFFFA726)
    "#4CAF50" -> Color(0xFF4CAF50)
    else -> Color(0xFF00E5FF)
}
