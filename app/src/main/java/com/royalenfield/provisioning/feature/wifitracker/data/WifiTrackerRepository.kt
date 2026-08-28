package com.royalenfield.provisioning.feature.wifitracker.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Serializable
data class WifiLogRecord(
    val vin: String = "",
    val wifiSsid: String = "",
    val newWifiPassword: String = "",
    val wifiMacId: String = "",
    val timestamp: String = "",
    val status: String = "SUCCESS",
    val oldSsid: String = "",
    val newSsid: String = "",
    val operator: String = "Field Mechanic",
    val vehicleModel: String = "Hunter 350 / Himalayan 450"
)

class WifiTrackerRepository(
    private val context: Context
) {
    private val jsonLogFile = File(context.filesDir, "wifi_change_audit.json")
    private val csvLogFile = File(context.filesDir, "Wifi_Password_Tracker.csv")
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    suspend fun getLogs(): List<WifiLogRecord> = withContext(Dispatchers.IO) {
        if (!jsonLogFile.exists()) {
            // Seed initial records if both files are empty
            seedInitialLogIfEmpty()
        }
        try {
            val content = jsonLogFile.readText()
            json.decodeFromString<List<WifiLogRecord>>(content)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun seedInitialLogIfEmpty() {
        val initialRecord = WifiLogRecord(
            vin = "ME3HUNTER350N20250",
            wifiSsid = "RE_LXHD_250925",
            newWifiPassword = "Royal@2025!",
            wifiMacId = "02:00:00:44:55:66",
            timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
            status = "SUCCESS"
        )
        try {
            jsonLogFile.writeText(json.encodeToString(listOf(initialRecord)))
            writeRecordToCsv(initialRecord)
        } catch (e: Exception) {
            // Ignore
        }
    }

    suspend fun addLog(
        vin: String,
        wifiSsid: String,
        newWifiPassword: String,
        wifiMacId: String,
        status: String = "SUCCESS"
    ): WifiLogRecord = withContext(Dispatchers.IO) {
        val currentLogs = getLogs().toMutableList()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val timestamp = dateFormat.format(Date())

        val newEntry = WifiLogRecord(
            vin = vin,
            wifiSsid = wifiSsid,
            newWifiPassword = newWifiPassword,
            wifiMacId = wifiMacId,
            timestamp = timestamp,
            status = status,
            oldSsid = wifiSsid,
            newSsid = wifiSsid
        )

        currentLogs.add(0, newEntry)
        try {
            jsonLogFile.writeText(json.encodeToString(currentLogs))
        } catch (e: Exception) {
            android.util.Log.e("WifiLogRepo", "Failed to write audit log JSON: ${e.message}")
        }

        // Also append directly to Wifi_Password_Tracker.csv matching Python reference
        writeRecordToCsv(newEntry)

        newEntry
    }

    private fun writeRecordToCsv(record: WifiLogRecord) {
        try {
            val fileExists = csvLogFile.exists() && csvLogFile.length() > 0
            val headers = "VIN,WiFi SSID,New WiFi Password,WiFi MAC ID,Timestamp\n"
            val row = "\"${record.vin}\",\"${record.wifiSsid}\",\"${record.newWifiPassword}\",\"${record.wifiMacId}\",\"${record.timestamp}\"\n"

            if (!fileExists) {
                csvLogFile.writeText(headers + row)
            } else {
                csvLogFile.appendText(row)
            }
        } catch (e: Exception) {
            android.util.Log.e("WifiLogRepo", "Failed to write CSV: ${e.message}")
        }
    }

    suspend fun exportLogsToCsv(): File = withContext(Dispatchers.IO) {
        val logs = getLogs()
        val exportFile = File(context.cacheDir, "Wifi_Password_Tracker.csv")
        val csvHeader = "VIN,WiFi SSID,New WiFi Password,WiFi MAC ID,Timestamp\n"
        val csvContent = buildString {
            append(csvHeader)
            logs.forEach { log ->
                append("\"${log.vin}\",\"${log.wifiSsid}\",\"${log.newWifiPassword}\",\"${log.wifiMacId}\",\"${log.timestamp}\"\n")
            }
        }
        exportFile.writeText(csvContent)
        exportFile
    }
}
