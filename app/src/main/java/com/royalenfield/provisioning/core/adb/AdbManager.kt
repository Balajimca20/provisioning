package com.royalenfield.provisioning.core.adb

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.io.IOException
import java.net.SocketException
import java.util.Locale

sealed class AdbManagerResult {
    data class Success(val ssid: String, val macAddress: String = "N/A", val message: String) : AdbManagerResult()
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
        when (adbClient.restartAsRoot()) {
            is AdbResult.Success -> true
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

    suspend fun getHardwareMacAddress(): String = withContext(Dispatchers.IO) {
        when (val result = adbClient.runShell("cat /sys/class/net/wlan1/address")) {
            is AdbResult.Success -> {
                val mac = result.data.trim().uppercase(Locale.US)
                mac.ifEmpty { "N/A" }
            }
            is AdbResult.Failure -> {
                Log.w(TAG, "Failed to read sysfs MAC address: ${result.message}")
                "N/A"
            }
        }
    }

    suspend fun updateSoftApCredentials(
        newSsid: String,
        newPassphrase: String,
        onLog: (String) -> Unit = {}
    ): AdbManagerResult = withContext(Dispatchers.IO) {
        try {
            onLog("Verifying ADB root privileges...")
            val rootRes = adbClient.restartAsRoot()
            if (rootRes is AdbResult.Failure) {
                return@withContext AdbManagerResult.Failure("Vehicle device does not have ADB root permission: ${rootRes.message}")
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
            adbClient.runShell("chmod 600 $REMOTE_SOFTAP_XML")
            adbClient.runShell("chown wifi:wifi $REMOTE_SOFTAP_XML")

            onLog("SoftAP XML successfully written! Device reboot will apply new credentials.")
            localXmlFile.delete()

            AdbManagerResult.Success(ssid = newSsid, message = "Successfully updated SoftAP configuration")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating SoftAP credentials: ${e.message}", e)
            AdbManagerResult.Failure(e.message ?: "Unknown error during SoftAP update")
        }
    }

    suspend fun executeWifiPasswordUpdate(
        vin: String,
        newPassword: String,
        onLog: (String) -> Unit = {},
        onStepProgress: (stepLabel: String, percent: Int) -> Unit = { _, _ -> }
    ): AdbManagerResult = withContext(Dispatchers.IO) {
        try {
            onLog("=== Starting Wi-Fi Password Update Workflow (VIN: $vin) ===")

            onStepProgress("Step 1/6: Acquiring root permissions on the device...", 10)
            onLog("Acquiring root permissions on the Android device...")
            val rootResult = adbClient.restartAsRoot()
            if (rootResult is AdbResult.Failure) {
                onLog("Error: Root escalation failed: ${rootResult.message}")
                return@withContext AdbManagerResult.Failure("Root escalation failed: ${rootResult.message}")
            }
            onLog("Root confirmed: ${(rootResult as AdbResult.Success).data}")

            onStepProgress("Step 2/6: Pulling remote SoftAP XML config...", 28)
            onLog("Pulling remote SoftAP XML config...")
            val localXmlFile = File(context.cacheDir, "wifi_tracker_softap_${System.currentTimeMillis()}.xml")
            try {
                withRetry("Pull $REMOTE_SOFTAP_XML", onLog) {
                    val pullRes = adbClient.pull(REMOTE_SOFTAP_XML, localXmlFile)
                    if (pullRes is AdbResult.Failure) throw IllegalStateException(pullRes.message)
                }
                onLog("Pull complete.")
            } catch (e: Exception) {
                onLog("[ADB Error]: Failed to pull XML: ${e.message}")
                return@withContext AdbManagerResult.Failure("Failed to pull XML: ${e.message}")
            }

            onStepProgress("Step 3/6: Parsing XML & updating passphrase...", 45)
            onLog("Parsing 'WifiConfigStoreSoftAp.xml'...")
            val originalContent = try {
                localXmlFile.readText()
            } catch (e: Exception) {
                onLog("Error: Local XML file not found after pull.")
                return@withContext AdbManagerResult.Failure("Local XML file not found after pull: ${e.message}")
            }

            val ssid = extractXmlStringValue(originalContent, "SSID", "Ssid") ?: "N/A"
            var macAddress = extractXmlStringValue(originalContent, "MacAddress", "MACAddress", "Mac") ?: "N/A"

            val modifiedContent = replacePassphraseOnly(originalContent, newPassword)
            localXmlFile.writeText(modifiedContent)
            onLog("XML updated with new password successfully.")

            onStepProgress("Step 4/6: Resolving Wi-Fi MAC address...", 55)
            if (macAddress.isBlank() || macAddress.equals("N/A", ignoreCase = true) || macAddress.equals("Unknown", ignoreCase = true)) {
                onLog("MAC not found in XML. Querying /sys/class/net/wlan1/address directly...")
                macAddress = getHardwareMacAddress()
            }
            onLog("Extracted Info -> SSID: $ssid | MAC: $macAddress")

            onStepProgress("Step 5/6: Pushing updated SoftAP XML to vehicle...", 70)
            onLog("Pushing updated SoftAP XML to vehicle partition...")
            try {
                withRetry("Push $REMOTE_SOFTAP_XML", onLog) {
                    val pushRes = adbClient.push(localXmlFile, REMOTE_SOFTAP_XML)
                    if (pushRes is AdbResult.Failure) throw IllegalStateException(pushRes.message)
                }
                onLog("Push complete.")
            } catch (e: Exception) {
                onLog("[ADB Error]: Failed to push XML: ${e.message}")
                return@withContext AdbManagerResult.Failure("Failed to push XML: ${e.message}")
            }

            onStepProgress("Step 6/6: Setting permissions & rebooting device...", 88)
            onLog("Setting permissions (chmod 600)...")
            adbClient.runShell("chmod 600 $REMOTE_SOFTAP_XML")
            adbClient.runShell("chown wifi:wifi $REMOTE_SOFTAP_XML")

            localXmlFile.delete()
            onLog("SoftAP XML successfully written! Rebooting target device...")
            val rebootRes = adbClient.reboot()
            if (rebootRes) {
                onLog("Note: reboot command reported '${rebootRes}' (device may already be restarting).")
            }

            AdbManagerResult.Success(
                ssid = ssid,
                macAddress = macAddress,
                message = "Successfully updated Wi-Fi passphrase"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error during Wi-Fi password update: ${e.message}", e)
            onLog("Unexpected Execution Error: ${e.message}")
            AdbManagerResult.Failure(e.message ?: "Unknown error during Wi-Fi password update")
        }
    }

    private suspend fun <T> withRetry(
        description: String,
        onLog: (String) -> Unit,
        timeoutMs: Long = 30_000,
        attempts: Int = 3,
        operation: suspend () -> T
    ): T {
        var lastError: Exception = IllegalStateException("$description failed")
        for (attempt in 1..attempts) {
            try {
                return withTimeout(timeoutMs) { operation() }
            } catch (e: Exception) {
                lastError = e
                onLog("⚠️ $description attempt $attempt failed: ${e.message}")
                if (attempt < attempts) {
                    ensureConnected(onLog)
                }
            }
        }
        throw lastError
    }

    private suspend fun ensureConnected(onLog: (String) -> Unit) {
        val responsive = try {
            withTimeout(5_000) {
                val res = adbClient.runShell("echo online")
                (res as? AdbResult.Success)?.data?.lowercase(Locale.US)?.contains("online") == true
            }
        } catch (e: Exception) {
            false
        }
        if (responsive) return

        onLog("🔗 Reconnecting ADB…")
        for (attempt in 1..3) {
            val reconnectRes = adbClient.reconnect()
            if (reconnectRes is AdbResult.Success) {
                val rootRes = adbClient.restartAsRoot()
                if (rootRes is AdbResult.Success) {
                    onLog("✅ Reconnected.")
                    return
                }
            }
            onLog("⚠️ Reconnect attempt $attempt failed.")
            delay(1500)
        }
        onLog("❌ Could not reconnect to the device.")
    }

    suspend fun rebootDevice(): AdbResult<String> {
        return try {
            adbClient.reboot()
            // Successfully sent reboot command
            AdbResult.Success("Reboot command sent successfully. Device is restarting.")
        } catch (e: SocketException) {
            // Expected behavior: Target device closed the socket immediately upon rebooting
            AdbResult.Success("Reboot command acknowledged. Device connection closed.")
        } catch (e: IOException) {
            // Handle secondary I/O drops caused by sudden shutdown
            AdbResult.Success("Reboot initiated; connection dropped as expected.")
        } catch (e: Exception) {
            // Unexpected failures (e.g., ADB permission denied, invalid state)
            AdbResult.Failure(e.message ?: "Failed to execute reboot command")
        }
    }

    private fun extractXmlStringValue(xml: String, vararg names: String): String? {
        for (name in names) {
            val regex = Regex("<string name=\"$name\">&quot;?(.*?)&quot;?</string>")
            val match = regex.find(xml)
            if (match != null) {
                val value = match.groupValues[1].trim()
                if (value.isNotEmpty()) return value
            }
        }
        return null
    }

    private fun replaceSoftApValues(xml: String, newSsid: String, newPass: String): String {
        var updated = xml
        val ssidRegex = Regex("<string name=\"SSID\">&quot;.*?&quot;</string>")
        val passRegex = Regex("<string name=\"PreSharedKey\">&quot;.*?&quot;</string>")

        updated = ssidRegex.replace(updated, "<string name=\"SSID\">&quot;$newSsid&quot;</string>")
        updated = passRegex.replace(updated, "<string name=\"PreSharedKey\">&quot;$newPass&quot;</string>")
        return updated
    }

    private fun replacePassphraseOnly(xml: String, newPass: String): String {
        val passphraseRegex = Regex("(<string name=\"Passphrase\">)(.*?)(</string>)")
        return if (passphraseRegex.containsMatchIn(xml)) {
            passphraseRegex.replace(xml) { match ->
                "${match.groupValues[1]}$newPass${match.groupValues[3]}"
            }
        } else {
            if (xml.contains("</WifiConfigStoreSoftAp>")) {
                xml.replace(
                    "</WifiConfigStoreSoftAp>",
                    "  <string name=\"Passphrase\">$newPass</string>\n</WifiConfigStoreSoftAp>"
                )
            } else {
                xml + "\n<string name=\"Passphrase\">$newPass</string>"
            }
        }
    }
}