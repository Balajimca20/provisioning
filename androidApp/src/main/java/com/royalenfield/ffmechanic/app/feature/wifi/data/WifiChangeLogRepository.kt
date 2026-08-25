package com.royalenfield.ffmechanic.app.feature.wifi.data

import android.content.Context
import com.royalenfield.ffmechanic.app.feature.wifi.domain.WifiChangeRecord
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Port of _write_csv_log() from Wifi_Password_tracker.py. Appends one row per Wi-Fi change
 * to a CSV in app-private storage (Context.filesDir), same header order as the original:
 * VIN, WiFi SSID, New WiFi Password, WiFi MAC ID, Timestamp.
 *
 * NOTE: like the original tool, this logs the *plaintext* new password to disk. That was true
 * of the desktop version too — worth revisiting (e.g. write to an encrypted store, or log only
 * a hash) now that this can live on a portable device, but keeping parity for now per your call
 * to flag secrets handling rather than change behavior yet.
 */
@Singleton
class WifiChangeLogRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val csvFile: File
        get() = File(context.filesDir, "Wifi_Password_Tracker.csv")

    fun append(record: WifiChangeRecord) {
        val fileExists = csvFile.exists()
        csvFile.appendText(buildString {
            if (!fileExists) appendLine("VIN,WiFi SSID,New WiFi Password,WiFi MAC ID,Timestamp")
            appendLine(
                listOf(record.vin, record.ssid, record.newPassword, record.macAddress, record.timestamp)
                    .joinToString(",") { csvEscape(it) }
            )
        })
    }

    fun readAll(): List<WifiChangeRecord> {
        if (!csvFile.exists()) return emptyList()
        return csvFile.readLines().drop(1).mapNotNull { line ->
            val parts = line.split(",")
            if (parts.size < 5) return@mapNotNull null
            WifiChangeRecord(parts[0], parts[1], parts[2], parts[3], parts[4])
        }
    }

    private fun csvEscape(value: String): String =
        if (value.contains(",") || value.contains("\"")) "\"${value.replace("\"", "\"\"")}\"" else value
}
