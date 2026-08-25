package com.royalenfield.ffmechanic.app.feature.wifi.domain

import com.royalenfield.ffmechanic.app.core.adb.AdbManager
import com.royalenfield.ffmechanic.app.core.adb.AdbManagerResult
import com.royalenfield.ffmechanic.app.feature.wifi.data.WifiChangeLogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

sealed class WifiStep {
    data class Log(val message: String, val color: String = "black") : WifiStep()
    data class Done(val success: Boolean) : WifiStep()
}

data class WifiChangeRecord(
    val vin: String,
    val ssid: String,
    val newPassword: String,
    val macAddress: String,
    val timestamp: String,
)

/**
 * Port of WifiUpdateWorker.run() from Wifi_Password_tracker.py: connect -> root -> pull XML ->
 * parse/modify Passphrase -> push XML back -> log CSV row -> delete local file -> reboot.
 *
 * Emits a Flow of steps so the ViewModel can render the log console live, same as the
 * log_signal / finished_signal pyqtSignals did.
 */
@Singleton
class WifiUpdateWorkflow @Inject constructor(
    private val adbManager: AdbManager,
    private val wifiChangeLog: WifiChangeLogRepository,
) {
    fun run(host: String, vin: String, newPassword: String, workDir: File): Flow<WifiStep> = flow {
        emit(WifiStep.Log("=== Starting Wi-Fi Password Update Workflow ===", "black"))

        val result = adbManager.connectAndUpdateWifi(
            host = host,
            localCacheDir = workDir,
            newPassword = newPassword,
            onLog = { emit(WifiStep.Log(it, "black")) },
        )

        if (result is AdbManagerResult.Failure) {
            emit(WifiStep.Log("Wi-Fi update failed: ${result.message}", "red"))
            emit(WifiStep.Done(false))
            return@flow
        }

        val success = result as AdbManagerResult.Success
        emit(WifiStep.Log("XML updated with new password successfully.", "green"))
        emit(WifiStep.Log("Extracted Info -> SSID: ${success.ssid} | MAC: ${success.macAddress}", "black"))

        emit(WifiStep.Log("Logging transaction to Wi-Fi change log...", "black"))
        wifiChangeLog.append(
            WifiChangeRecord(
                vin = vin,
                ssid = success.ssid,
                newPassword = newPassword,
                macAddress = success.macAddress,
                timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date()),
            )
        )
        emit(WifiStep.Log("Log entry recorded successfully.", "green"))

        emit(WifiStep.Log("=== Workflow Completed Successfully! ===", "green"))
        emit(WifiStep.Done(true))
    }
}
