package com.royalenfield.provisioning.feature.ota.data

import com.royalenfield.provisioning.core.adb.AdbClient
import com.royalenfield.provisioning.core.config.EnvironmentConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
data class OtaPackage(
    val id: String,
    val vehicleModel: String,
    val targetVersion: String,
    val sizeBytes: Long,
    val sizeDisplay: String,
    val sha256: String,
    val releaseDate: String,
    val notes: String,
    val minBatteryRequired: Int = 50,
    val minStorageRequiredMb: Int = 1000
)

class OtaRepository(
    private val httpClient: HttpClient,
    private val adbClient: AdbClient
) {
    suspend fun getAvailablePackages(): Result<List<OtaPackage>> = withContext(Dispatchers.IO) {
        try {
            val endpoint = "${EnvironmentConfig.ffBaseUrl}/api/v1/ota/packages"
            val response = httpClient.get(endpoint).body<List<OtaPackage>>()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkVehicleBattery(): Int = withContext(Dispatchers.IO) {
        val result = adbClient.runShell("dumpsys battery | grep level")
        when (result) {
            is com.royalenfield.provisioning.core.adb.AdbResult.Success -> {
                val levelStr = result.data.substringAfter("level:").trim()
                val parsed = levelStr.toIntOrNull()
                if (parsed != null) return@withContext parsed
                
                // Fallback to sysfs capacity
                val sysfsRes = adbClient.runShell("cat /sys/class/power_supply/battery/capacity")
                if (sysfsRes is com.royalenfield.provisioning.core.adb.AdbResult.Success) {
                    sysfsRes.data.trim().toIntOrNull() ?: -1
                } else {
                    -1
                }
            }
            is com.royalenfield.provisioning.core.adb.AdbResult.Failure -> -1
        }
    }

    suspend fun checkAvailableStorageMb(): Long = withContext(Dispatchers.IO) {
        val result = adbClient.runShell("df /data")
        when (result) {
            is com.royalenfield.provisioning.core.adb.AdbResult.Success -> {
                try {
                    val lines = result.data.trim().lines()
                    if (lines.size >= 2) {
                        val parts = lines[1].trim().split(Regex("\\s+"))
                        // df output: Filesystem 1K-blocks Used Available Use% Mounted on
                        if (parts.size >= 4) {
                            val availableKb = parts[3].toLongOrNull() ?: 0L
                            return@withContext availableKb / 1024
                        }
                    }
                    -1L
                } catch (e: Exception) {
                    -1L
                }
            }
            is com.royalenfield.provisioning.core.adb.AdbResult.Failure -> -1L
        }
    }

    suspend fun queryInstalledFirmware(): String = withContext(Dispatchers.IO) {
        val result = adbClient.runShell("getprop ro.build.display.id")
        when (result) {
            is com.royalenfield.provisioning.core.adb.AdbResult.Success -> {
                val displayId = result.data.trim()
                if (displayId.isNotEmpty()) return@withContext displayId
                
                val incremental = adbClient.runShell("getprop ro.build.version.incremental")
                if (incremental is com.royalenfield.provisioning.core.adb.AdbResult.Success && incremental.data.trim().isNotEmpty()) {
                    return@withContext incremental.data.trim()
                }

                val buildId = adbClient.runShell("getprop ro.build.id")
                if (buildId is com.royalenfield.provisioning.core.adb.AdbResult.Success && buildId.data.trim().isNotEmpty()) {
                    return@withContext buildId.data.trim()
                }
                "Unknown Version"
            }
            is com.royalenfield.provisioning.core.adb.AdbResult.Failure -> "Cluster Offline"
        }
    }

    suspend fun queryActiveSlot(): String = withContext(Dispatchers.IO) {
        val result = adbClient.runShell("getprop ro.boot.slot_suffix")
        when (result) {
            is com.royalenfield.provisioning.core.adb.AdbResult.Success -> {
                val suffix = result.data.trim().lowercase()
                when {
                    suffix.contains("b") -> "SLOT B"
                    suffix.contains("a") -> "SLOT A"
                    else -> {
                        // Query bootctl as fallback
                        val bootctlRes = adbClient.runShell("bootctl get-current-slot")
                        if (bootctlRes is com.royalenfield.provisioning.core.adb.AdbResult.Success) {
                            when (bootctlRes.data.trim()) {
                                "0" -> "SLOT A"
                                "1" -> "SLOT B"
                                else -> "SLOT A"
                            }
                        } else {
                            "SLOT A"
                        }
                    }
                }
            }
            is com.royalenfield.provisioning.core.adb.AdbResult.Failure -> "Offline"
        }
    }
}

