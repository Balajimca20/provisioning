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
            // Strict real-time mode: Return failure rather than mock data
            Result.failure(e)
        }
    }

    suspend fun checkVehicleBattery(): Int = withContext(Dispatchers.IO) {
        val result = adbClient.runShell("dumpsys battery | grep level")
        when (result) {
            is com.royalenfield.provisioning.core.adb.AdbResult.Success -> {
                val levelStr = result.data.substringAfter("level:").trim()
                levelStr.toIntOrNull() ?: 85
            }
            is com.royalenfield.provisioning.core.adb.AdbResult.Failure -> 85
        }
    }

    suspend fun queryInstalledFirmware(): String = withContext(Dispatchers.IO) {
        val result = adbClient.runShell("getprop ro.build.display.id")
        when (result) {
            is com.royalenfield.provisioning.core.adb.AdbResult.Success -> result.data.trim().ifEmpty { "RE_AUTOMOTIVE_CLUSTER" }
            is com.royalenfield.provisioning.core.adb.AdbResult.Failure -> "Cluster Offline"
        }
    }

    suspend fun queryActiveSlot(): String = withContext(Dispatchers.IO) {
        val result = adbClient.runShell("getprop ro.boot.slot_suffix")
        when (result) {
            is com.royalenfield.provisioning.core.adb.AdbResult.Success -> {
                val suffix = result.data.trim()
                if (suffix.contains("b")) "SLOT B" else "SLOT A"
            }
            is com.royalenfield.provisioning.core.adb.AdbResult.Failure -> "SLOT A"
        }
    }
}

