package com.royalenfield.provisioning.feature.supplierfeed.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

// Dark Theme Color Palette
private val DarkBg = Color(0xFF1018)
private val CardBg = Color(0xFF09121D)
private val BorderColor = Color(0xFF12314E)
private val AccentCyan = Color(0xFF00E5FF)
private val LabelColor = Color(0xFF8DA4BE)
private val InputBg = Color(0xFF0B1726)

@Composable
fun SupplierFeedScreen(viewModel: SupplierFeedViewModel = viewModel()) {

    val uiState = viewModel.deviceData.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Search Action Bar

        Text("ICCID:", color = AccentCyan, fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = uiState.value.iccid,
            onValueChange = { viewModel.onIccidChanged(it) },
            placeholder = { Text("Enter or paste 19/20-digit ICCID", color = LabelColor) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.Black,
                unfocusedContainerColor = Color.Black,
                focusedBorderColor = AccentCyan,
                unfocusedBorderColor = BorderColor,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = { viewModel.fetchIccidViaDadb() },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0C3552)),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text("Read ICCID from Device", color = AccentCyan)
        }


        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Button(
                onClick = { viewModel.fetchSupplierFeedResponse() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0C3552)),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text("🔍 Fetch Device", color = AccentCyan)
            }

            Button(
                onClick = { viewModel.clearForm() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222930)),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text("🧹 Clear Form", color = Color.LightGray)
            }
        }

        // Status Indicator Message
        if (uiState.value.iccid.isNotEmpty()) {
            Text(
                text = "ℹ No device loaded. Enter an ICCID above and click 'Fetch Device'.",
                color = LabelColor,
                fontSize = 12.sp
            )
        }

        // Section Cards
        SectionGroup(title = "📦 Core System _Hardware") {
            Grid3Layout(
                listOf(
                    "ICCID:" to uiState.value.iccid, "EUID:" to uiState.value.euid, "System ID:" to uiState.value.systemId,
                    "Status:" to uiState.value.status, "Model:" to uiState.value.model, "IMEI Primary:" to uiState.value.imeiPrimary,
                    "IMEI Secondary:" to uiState.value.imeiSecondary, "Serial No:" to uiState.value.serialNo, "Part No:" to uiState.value.partNo,
                    "HW Version:" to uiState.value.hwVersion, "Vendor Code:" to uiState.value.vendorCode, "Category:" to uiState.value.category
                )
            )
        }

        SectionGroup(title = "🔐 Security _Key Vault") {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(modifier = Modifier.weight(1f)) { DetailField("Admin Key:", uiState.value.adminKey) }
                Box(modifier = Modifier.weight(1f)) { DetailField("User Key:", uiState.value.userKey) }
            }
        }

        SectionGroup(title = "📊 SOM Wireless (Wi-Fi _Bluetooth)") {
            Grid3Layout(
                listOf(
                    "Wi-Fi MAC (2.4GHz):" to uiState.value.wifiMac24, "Wi-Fi SSID (2.4GHz):" to uiState.value.wifiSsid24, "Wi-Fi Passphrase (2.4GHz):" to uiState.value.wifiPass24,
                    "Wi-Fi MAC (5GHz):" to uiState.value.wifiMac5, "Wi-Fi SSID (5GHz):" to uiState.value.wifiSsid5, "Wi-Fi Passphrase (5GHz):" to uiState.value.wifiPass5,
                    "SOM BT MAC:" to uiState.value.somBtMac, "SOM BT Connection Name:" to uiState.value.somBtName, "SOM BT Passphrase:" to uiState.value.somBtPass,
                    "SOM BLE MAC:" to uiState.value.somBleMac, "SOM BLE Passphrase:" to uiState.value.somBlePass, "" to ""
                )
            )
        }

        SectionGroup(title = "📡 BCM Wireless (Bluetooth _BLE)") {
            Grid3Layout(
                listOf(
                    "BCM 1st BLE MAC:" to uiState.value.bcm1stBleMac, "BCM 1st BLE Name:" to uiState.value.bcm1stBleName, "BCM 1st BLE Passphrase:" to uiState.value.bcm1stBlePass,
                    "BCM 2nd BT MAC:" to uiState.value.bcm2ndBtMac, "BCM 2nd BT Name:" to uiState.value.bcm2ndBtName, "BCM 2nd BT Passphrase:" to uiState.value.bcm2ndBtPass
                )
            )
        }

        SectionGroup(title = "📲 Cellular Modem _eSIM Profile") {
            Grid3Layout(
                listOf(
                    "eSIM IMSI:" to uiState.value.esimImsi, "eSIM MSISDN:" to uiState.value.esimMsisdn, "eSIM APN:" to uiState.value.esimApn,
                    "eSIM Part No:" to uiState.value.esimPartNo, "eSIM Vendor Code:" to uiState.value.esimVendorCode, "GSM CREG:" to uiState.value.gsmCreg,
                    "Shipment Invoice:" to uiState.value.shipmentInvoice, "" to "", "" to ""
                )
            )
        }

        SectionGroup(title = "🏭 Software, Firmware _Lifecycle Metadata") {
            Grid3Layout(
                listOf(
                    "SOM Make:" to uiState.value.somMake, "SOM SW Version:" to uiState.value.somSwVersion, "Firmware Version:" to uiState.value.firmwareVersion,
                    "Config Version:" to uiState.value.configVersion, "Manufacturing Date:" to uiState.value.manufacturingDate, "Created By:" to uiState.value.createdBy,
                    "Created Time:" to uiState.value.createdTime, "Updated By:" to uiState.value.updatedBy, "Updated Time:" to uiState.value.updatedTime
                )
            )
        }
    }
}

@Composable
fun SectionGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(6.dp))
            .border(1.dp, BorderColor, RoundedCornerShape(6.dp))
            .padding(16.dp)
    ) {
        Text(title, color = AccentCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 12.dp))
        content()
    }
}

@Composable
fun Grid3Layout(items: List<Pair<String, String>>) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach {  item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
            ) {
                DetailField(label = item.first, value = item.second)
            }
        }
    }
}

@Composable
fun DetailField(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            color = LabelColor,
            fontSize = 11.sp,
            modifier = Modifier.width(115.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = InputBg,
                unfocusedContainerColor = InputBg,
                focusedBorderColor = BorderColor,
                unfocusedBorderColor = BorderColor,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )
    }
}
