package com.royalenfield.provisioning.feature.wifi.domain

import com.royalenfield.provisioning.core.adb.AdbManager
import com.royalenfield.provisioning.core.adb.AdbManagerResult
import com.royalenfield.provisioning.feature.wifi.data.WifiChangeLogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

data class WorkflowProgress(
    val step: String,
    val percent: Int,
    val isComplete: Boolean = false,
    val error: String? = null
)

class WifiUpdateWorkflow(
    private val adbManager: AdbManager,
    private val logRepository: WifiChangeLogRepository
) {
    fun execute(
        oldSsid: String,
        newSsid: String,
        newPassphrase: String
    ): Flow<WorkflowProgress> = flow {
        emit(WorkflowProgress("Step 1/5: Verifying root access via ADB...", 15))

        val result = adbManager.updateSoftApCredentials(
            newSsid = newSsid,
            newPassphrase = newPassphrase,
            onLog = { }
        )

        when (result) {
            is AdbManagerResult.Success -> {
                emit(WorkflowProgress("Step 4/5: Syncing audit record to local ledger...", 85))
                logRepository.addLog(oldSsid, newSsid, "SUCCESS")
                emit(WorkflowProgress("Step 5/5: SoftAP XML credentials updated successfully!", 100, isComplete = true))
            }
            is AdbManagerResult.Failure -> {
                logRepository.addLog(oldSsid, newSsid, "FAILED: ${result.message}")
                emit(WorkflowProgress("Failed: ${result.message}", 0, isComplete = false, error = result.message))
            }
        }
    }
}
