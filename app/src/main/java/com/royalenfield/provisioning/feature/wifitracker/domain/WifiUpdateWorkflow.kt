package com.royalenfield.provisioning.feature.wifitracker.domain

import com.royalenfield.provisioning.core.adb.AdbManager
import com.royalenfield.provisioning.core.adb.AdbManagerResult
import com.royalenfield.provisioning.feature.wifitracker.data.WifiTrackerRepository
import com.royalenfield.provisioning.feature.wifitracker.data.WifiLogRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

data class WorkflowProgress(
    val step: String,
    val percent: Int,
    val isComplete: Boolean = false,
    val error: String? = null,
    val ssid: String = "",
    val macAddress: String = "",
    val logRecord: WifiLogRecord? = null
)

class WifiUpdateWorkflow(
    private val adbManager: AdbManager,
    private val logRepository: WifiTrackerRepository
) {
    fun execute(
        vin: String,
        newPassword: String,
        onLogMessage: (String) -> Unit
    ): Flow<WorkflowProgress> = flow {
        emit(WorkflowProgress("Step 1/6: Connecting to ADB & verifying root access...", 15))

        val result = adbManager.executeWifiPasswordUpdate(
            vin = vin,
            newPassword = newPassword,
            onLog = onLogMessage
        )

        when (result) {
            is AdbManagerResult.Success -> {
                emit(WorkflowProgress("Step 5/6: Logging transaction to 'Wifi_Password_Tracker.csv'...", 85))
                val savedRecord = logRepository.addLog(
                    vin = vin,
                    wifiSsid = result.ssid,
                    newWifiPassword = newPassword,
                    wifiMacId = result.macAddress,
                    status = "SUCCESS"
                )

                emit(
                    WorkflowProgress(
                        step = "Step 6/6: === Workflow Completed Successfully! ===",
                        percent = 100,
                        isComplete = true,
                        ssid = result.ssid,
                        macAddress = result.macAddress,
                        logRecord = savedRecord
                    )
                )
            }
            is AdbManagerResult.Failure -> {
                // Also log failed attempt
                logRepository.addLog(
                    vin = vin,
                    wifiSsid = "Unknown",
                    newWifiPassword = newPassword,
                    wifiMacId = "Unknown",
                    status = "FAILED"
                )
                emit(
                    WorkflowProgress(
                        step = "Failed: ${result.message}",
                        percent = 0,
                        isComplete = false,
                        error = result.message
                    )
                )
            }
        }
    }
}
