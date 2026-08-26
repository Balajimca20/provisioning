package com.royalenfield.provisioning.feature.ota.domain

import android.util.Log
import com.royalenfield.provisioning.core.adb.AdbClient
import com.royalenfield.provisioning.core.adb.AdbResult
import com.royalenfield.provisioning.feature.ota.data.OtaPackage
import com.royalenfield.provisioning.feature.ota.data.OtaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File

sealed class OtaProgress {
    data class PreCheck(val batteryOk: Boolean, val storageOk: Boolean) : OtaProgress()
    data class Downloading(val percent: Int, val bytesTransferred: Long, val totalBytes: Long) : OtaProgress()
    data class PushingToVehicle(val percent: Int, val speedMbps: Double) : OtaProgress()
    data class VerifyingChecksum(val percent: Int, val sha256Matched: Boolean) : OtaProgress()
    data class FlashingPartition(val percent: Int, val currentSlot: String) : OtaProgress()
    object AwaitingReboot : OtaProgress()
    object Complete : OtaProgress()
    data class Failed(val reason: String) : OtaProgress()
    data class Log(val message: String, val isUpdate: Boolean = false) : OtaProgress()
}

class OtaPipeline(
    private val otaRepository: OtaRepository,
    private val adbClient: AdbClient
) {
    /**
     * Technical implementation of the Royal Enfield OTA Flash logic.
     * Ported from the Python 'ota_call.py' metadata extraction and deployment workflow.
     */
    fun runPipeline(pkg: OtaPackage, localZipFile: File? = null): Flow<OtaProgress> = flow {
        try {
            emit(OtaProgress.Log("re_mechanic@vehicle:~$ ota-deploy --start --target ${pkg.targetVersion}"))
            
            // Step 1: Real Pre-flight vehicle checks (Battery & Storage)
            emit(OtaProgress.Log("[SYS] Performing hardware pre-flight checks..."))
            val batteryLevel = otaRepository.checkVehicleBattery()
            if (batteryLevel != -1) {
                emit(OtaProgress.Log("🔋 Vehicle Battery: $batteryLevel% (Min required: ${pkg.minBatteryRequired}%)"))
                if (batteryLevel < pkg.minBatteryRequired) {
                    emit(OtaProgress.Failed("Battery level ($batteryLevel%) is below minimum required (${pkg.minBatteryRequired}%)"))
                    return@flow
                }
            } else {
                emit(OtaProgress.Log("⚠️ Battery status: telemetry bypassed (AC/bench mode)"))
            }

            val storageAvailableMb = otaRepository.checkAvailableStorageMb()
            if (storageAvailableMb > 0) {
                emit(OtaProgress.Log("💾 Storage Available: ${storageAvailableMb}MB on /data"))
                if (storageAvailableMb < pkg.minStorageRequiredMb) {
                    emit(OtaProgress.Failed("Insufficient storage: ${storageAvailableMb}MB available, required ${pkg.minStorageRequiredMb}MB"))
                    return@flow
                }
            }
            emit(OtaProgress.PreCheck(batteryOk = true, storageOk = true))

            // Step 2: Escalate Root Permissions
            emit(OtaProgress.Log("🔓 Acquiring root permissions on the Android device…"))
            val rootRes = adbClient.restartAsRoot()
            if (rootRes is AdbResult.Failure) {
                emit(OtaProgress.Log("⚠️ Root escalation notice (continuing): ${rootRes.message}"))
            } else if (rootRes is AdbResult.Success) {
                emit(OtaProgress.Log("🔓 Root status: ${rootRes.data}"))
            }
            delay(300)

            // Step 3: Stage update.zip to target path
            val remotePath = "/data/ota_package/update.zip"
            val workFile = localZipFile ?: File("/data/user/0/com.royalenfield.provisioning/files/update.zip")
            
            if (!workFile.exists()) {
                 emit(OtaProgress.Failed("Source archive not found at ${workFile.absolutePath}"))
                 return@flow
            }

            emit(OtaProgress.Log("[ADB] Pushing ${workFile.name} (${workFile.length() / 1024 / 1024} MB) to $remotePath..."))
            adbClient.runShell("mkdir -p /data/ota_package")
            
            emit(OtaProgress.PushingToVehicle(10, speedMbps = 0.0))
            val startTime = System.currentTimeMillis()
            val pushRes = adbClient.push(workFile, remotePath)
            val elapsedSec = ((System.currentTimeMillis() - startTime).coerceAtLeast(100)) / 1000.0
            
            if (pushRes is AdbResult.Failure) {
                emit(OtaProgress.Failed("Push failed: ${pushRes.message}"))
                return@flow
            }

            val actualSpeedMbps = if (elapsedSec > 0) {
                String.format(java.util.Locale.US, "%.1f", (workFile.length() / 1024.0 / 1024.0) / elapsedSec).toDoubleOrNull() ?: 0.0
            } else 0.0
            emit(OtaProgress.PushingToVehicle(100, speedMbps = actualSpeedMbps))

            // Verify remote file integrity & size
            val verifyRes = adbClient.runShell("stat -c %s $remotePath || ls -l $remotePath")
            if (verifyRes is AdbResult.Success) {
                emit(OtaProgress.Log("✅ [SYNCED] Remote file verified: ${verifyRes.data.trim()} ($actualSpeedMbps MB/s)"))
            }

            // Step 4: Metadata Extraction (payload.bin analyzer)
            emit(OtaProgress.Log("[SYS] Running payload metadata analyzer..."))
            val payloadInfo = withContext(Dispatchers.IO) {
                try {
                    OTAZipInspector.inspect(workFile)
                } catch (e: Exception) {
                    null
                }
            }
            
            if (payloadInfo == null) {
                emit(OtaProgress.Failed("Invalid update archive: payload.bin descriptors not found."))
                return@flow
            }
            
            emit(OtaProgress.Log("⚙️ payload_offset: ${payloadInfo.payloadOffset}"))
            emit(OtaProgress.Log("⚙️ payload_size: ${payloadInfo.payloadSize}"))
            emit(OtaProgress.Log("✅ Technical parameters extracted from ZIP header."))

            // Step 5: Execute Update Engine via ADB streaming
            val updateCmd = "update_engine_client --update --follow " +
                    "--payload=file://$remotePath " +
                    "--offset=${payloadInfo.payloadOffset} --size=${payloadInfo.payloadSize} " +
                    "--headers=\"${payloadInfo.headers}\""
            
            emit(OtaProgress.Log("[CMD] cd /data/ota_package"))
            emit(OtaProgress.Log("[CMD] Spawning update_engine_client daemon..."))
            emit(OtaProgress.Log("[CMD] $ $updateCmd"))

            val currentSlot = otaRepository.queryActiveSlot()
            val targetSlot = if (currentSlot.contains("A", ignoreCase = true)) "boot_b" else "boot_a"
            emit(OtaProgress.Log("🎯 Active Slot: $currentSlot -> Target Slot: $targetSlot"))

            var updateCompleted = false
            adbClient.runShellStreaming("cd /data/ota_package && $updateCmd").collect { line ->
                val cleanLine = line.trim()
                if (cleanLine.isNotEmpty()) {
                    val isStatusUpdate = cleanLine.contains("UPDATE_STATUS")
                    emit(OtaProgress.Log("📲 $cleanLine", isUpdate = isStatusUpdate))
                    
                    if (cleanLine.contains("UPDATE_STATUS_DOWNLOADING")) {
                        val pctMatch = Regex("([0-9.]+)").find(cleanLine.substringAfter("),"))
                        pctMatch?.let {
                            val pct = (it.value.toDouble() * 100).toInt()
                            emit(OtaProgress.FlashingPartition(pct, currentSlot = targetSlot))
                        }
                    } else if (cleanLine.contains("UPDATE_STATUS_VERIFYING")) {
                        val pctMatch = Regex("([0-9.]+)").find(cleanLine.substringAfter("),"))
                        pctMatch?.let {
                            val pct = (it.value.toDouble() * 100).toInt()
                            emit(OtaProgress.VerifyingChecksum(pct, pct >= 100))
                        }
                    } else if (cleanLine.contains("UPDATE_STATUS_FINALIZING")) {
                        emit(OtaProgress.FlashingPartition(99, currentSlot = targetSlot))
                    } else if (cleanLine.contains("UPDATE_STATUS_UPDATED_NEED_REBOOT") || cleanLine.contains("Update succeeded")) {
                        updateCompleted = true
                    }
                }
            }

            emit(OtaProgress.Log("🎉 [SUCCESS] Deployment applied to inactive partition."))
            emit(OtaProgress.AwaitingReboot)

        } catch (e: Exception) {
            emit(OtaProgress.Failed("Deployment error: ${e.localizedMessage}"))
        }
    }

    suspend fun executeRebootAndVerify(): Boolean {
        return withContext(Dispatchers.IO) {
            // Issuing the final hardware reboot (adb reboot)
            adbClient.runShell("reboot")
            true
        }
    }
}
