package com.royalenfield.provisioning.core.adb

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

sealed class AdbManagerResult {
    data class Success(val ssid: String, val message: String) : AdbManagerResult()
    data class Failure(val message: String) : AdbManagerResult()
}

class AdbManager(
    private val context: Context,
    private val adbClient: AdbClient
) {
    companion object {
        private const val TAG = "AdbManager"
        const val REMOTE_SOFTAP_XML =
            "/data/misc/apexdata/com.android.wifi/WifiConfigStoreSoftAp.xml"
    }

    suspend fun verifyRootAccess(): Boolean = withContext(Dispatchers.IO) {
        when (val result = adbClient.runShell("su 0 id")) {
            is AdbResult.Success -> result.data.contains("uid=0(root)")
            is AdbResult.Failure -> false
        }
    }

    suspend fun readSoftApXml(): Result<String> = withContext(Dispatchers.IO) {
        val tempFile = File(context.cacheDir, "temp_softap_read.xml")
        when (val pullResult = adbClient.pull(REMOTE_SOFTAP_XML, tempFile)) {
            is AdbResult.Success -> {
                try {
                    val content = tempFile.readText()
                    tempFile.delete()
                    Result.success(content)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
            is AdbResult.Failure -> Result.failure(Exception(pullResult.message))
        }
    }

    suspend fun updateSoftApCredentials(
        newSsid: String,
        newPassphrase: String,
        onLog: (String) -> Unit = {}
    ): AdbManagerResult = withContext(Dispatchers.IO) {
        try {
            onLog("Verifying ADB root privileges...")
            val hasRoot = verifyRootAccess()
            if (!hasRoot) {
                return@withContext AdbManagerResult.Failure("Vehicle device does not have su 0 root permission")
            }

            onLog("Pulling remote SoftAP XML config...")
            val localXmlFile = File(context.cacheDir, "WifiConfigStoreSoftAp.xml")
            when (val pullRes = adbClient.pull(REMOTE_SOFTAP_XML, localXmlFile)) {
                is AdbResult.Failure -> return@withContext AdbManagerResult.Failure("Failed to pull XML: ${pullRes.message}")
                is AdbResult.Success -> {}
            }

            onLog("Modifying SSID to $newSsid and updating Passphrase...")
            val originalContent = localXmlFile.readText()
            val modifiedContent = replaceSoftApValues(originalContent, newSsid, newPassphrase)
            localXmlFile.writeText(modifiedContent)

            onLog("Pushing updated SoftAP XML to vehicle partition...")
            when (val pushRes = adbClient.push(localXmlFile, REMOTE_SOFTAP_XML)) {
                is AdbResult.Failure -> return@withContext AdbManagerResult.Failure("Failed to push XML: ${pushRes.message}")
                is AdbResult.Success -> {}
            }

            onLog("Setting permissions (chmod 600)...")
            adbClient.runShell("su 0 chmod 600 $REMOTE_SOFTAP_XML")
            adbClient.runShell("su 0 chown wifi:wifi $REMOTE_SOFTAP_XML")

            onLog("SoftAP XML successfully written! Device reboot will apply new credentials.")
            localXmlFile.delete()

            AdbManagerResult.Success(newSsid, "Successfully updated SoftAP configuration")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating SoftAP credentials: ${e.message}", e)
            AdbManagerResult.Failure(e.message ?: "Unknown error during SoftAP update")
        }
    }

    suspend fun rebootDevice(): AdbResult<String> {
        return adbClient.runShell("su 0 reboot")
    }

    private fun replaceSoftApValues(xml: String, newSsid: String, newPass: String): String {
        var updated = xml
        val ssidRegex = Regex("<string name=\"SSID\">&quot;.*?&quot;</string>")
        val passRegex = Regex("<string name=\"PreSharedKey\">&quot;.*?&quot;</string>")

        updated = ssidRegex.replace(updated, "<string name=\"SSID\">&quot;$newSsid&quot;</string>")
        updated = passRegex.replace(updated, "<string name=\"PreSharedKey\">&quot;$newPass&quot;</string>")
        return updated
    }
}
