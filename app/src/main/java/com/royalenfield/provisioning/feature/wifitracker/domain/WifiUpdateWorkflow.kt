package com.royalenfield.provisioning.feature.wifitracker.domain

import com.royalenfield.provisioning.core.adb.AdbManager
import com.royalenfield.provisioning.core.adb.AdbManagerResult
import com.royalenfield.provisioning.feature.wifitracker.data.WifiTrackerRepository
import com.royalenfield.provisioning.feature.wifitracker.data.WifiLogRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow

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
    /**
     * Uses channelFlow (not a plain `flow { }`) so WorkflowProgress can be emitted from inside
     * the onStepProgress callback as AdbManager.executeWifiPasswordUpdate runs — previously
     * this only emitted once at the very start and once after the whole operation returned,
     * so the UI necessarily jumped 15% -> 85% -> 100% with nothing in between no matter how
     * long the real work took.
     */
    fun execute(
        vin: String,
        newPassword: String,
        onLogMessage: (String) -> Unit
    ): Flow<WorkflowProgress> = channelFlow {
        send(WorkflowProgress("Step 0/7: Connecting to ADB & verifying root access...", 5))

        val result = adbManager.executeWifiPasswordUpdate(
            vin = vin,
            newPassword = newPassword,
            onLog = onLogMessage,
            onStepProgress = { label, percent ->
                // Non-suspending, thread-safe — safe to call from within the withContext(IO)
                // block executeWifiPasswordUpdate runs on.
                trySend(WorkflowProgress(label, percent))
            }
        )

        when (result) {
            is AdbManagerResult.Success -> {
                send(WorkflowProgress("Step 7/7 (a): Logging transaction to 'Wifi_Password_Tracker.csv'...", 92))
                val savedRecord = logRepository.addLog(
                    vin = vin,
                    wifiSsid = result.ssid,
                    newWifiPassword = newPassword,
                    wifiMacId = result.macAddress,
                    status = "SUCCESS"
                )

                send(
                    WorkflowProgress(
                        step = "Step 7/7 (b): === Workflow Completed Successfully! ===",
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
                send(
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