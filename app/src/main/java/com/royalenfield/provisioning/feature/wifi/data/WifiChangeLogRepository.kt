package com.royalenfield.provisioning.feature.wifi.data

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
    val timestamp: String,
    val oldSsid: String,
    val newSsid: String,
    val status: String,
    val operator: String = "Field Mechanic",
    val vehicleModel: String = "Himalayan 450"
)

class WifiChangeLogRepository(
    private val context: Context
) {
    private val logFile = File(context.filesDir, "wifi_change_audit.json")
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    suspend fun getLogs(): List<WifiLogRecord> = withContext(Dispatchers.IO) {
        if (!logFile.exists()) return@withContext emptyList()
        try {
            val content = logFile.readText()
            json.decodeFromString<List<WifiLogRecord>>(content)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addLog(oldSsid: String, newSsid: String, status: String) = withContext(Dispatchers.IO) {
        val currentLogs = getLogs().toMutableList()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val newEntry = WifiLogRecord(
            timestamp = dateFormat.format(Date()),
            oldSsid = oldSsid,
            newSsid = newSsid,
            status = status
        )
        currentLogs.add(0, newEntry)
        try {
            logFile.writeText(json.encodeToString(currentLogs))
        } catch (e: Exception) {
            android.util.Log.e("WifiLogRepo", "Failed to write audit log: ${e.message}")
        }
    }
}
