package com.royalenfield.provisioning.core.adb

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

sealed class AdbManagerResult {
    // macAddress added: WifiUpdateWorkflow.kt reads result.macAddress, which didn't exist on
    // this type before — that was a compile-blocking mismatch, not just a missing feature.
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

    /**
     * Reads the device's Wi-Fi hardware MAC directly from sysfs — matches the Python
     * reference's fallback (`cat /sys/class/net/wlan1/address`) for when the SoftAP XML
     * doesn't carry a usable MAC entry. Previously called from WifiTrackerViewModel but never
     * defined here.
     */
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

    /** Unchanged — left exactly as before. */
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

            AdbManagerResult.Success(ssid = newSsid, message = "Successfully updated SoftAP configuration")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating SoftAP credentials: ${e.message}", e)
            AdbManagerResult.Failure(e.message ?: "Unknown error during SoftAP update")
        }
    }

    /**
     * Wi-Fi Tracker workflow: matches the Python `WifiUpdateWorker.run()` step-by-step —
     * verify root, pull XML, update ONLY the passphrase (SSID is left untouched, unlike
     * updateSoftApCredentials above), resolve MAC, push, chmod/chown, reboot.
     *
     * Previously missing entirely — WifiUpdateWorkflow.kt called this and it didn't exist.
     *
     * Reports progress two ways, both live/real-time rather than only before/after:
     *  - onLog: a human-readable console line after every step (feeds the terminal-style log)
     *  - onStepProgress: a (label, percent) pair after every step (feeds the progress bar)
     */
    suspend fun executeWifiPasswordUpdate(
        vin: String,
        newPassword: String,
        onLog: (String) -> Unit = {},
        onStepProgress: (stepLabel: String, percent: Int) -> Unit = { _, _ -> }
    ): AdbManagerResult = withContext(Dispatchers.IO) {
        try {
            onLog("=== Starting Wi-Fi Password Update Workflow (VIN: $vin) ===")

            onStepProgress("Step 1/6: Verifying root access...", 10)
            onLog("Verifying ADB root privileges...")
            val hasRoot = verifyRootAccess()
            if (!hasRoot) {
                onLog("Error: Vehicle device does not have su 0 root permission")
                return@withContext AdbManagerResult.Failure("Vehicle device does not have su 0 root permission")
            }

            onStepProgress("Step 2/6: Pulling remote SoftAP XML config...", 28)
            onLog("Pulling remote SoftAP XML config...")
            val localXmlFile = File(context.cacheDir, "wifi_tracker_softap_${System.currentTimeMillis()}.xml")
            when (val pullRes = adbClient.pull(REMOTE_SOFTAP_XML, localXmlFile)) {
                is AdbResult.Failure -> {
                    onLog("[ADB Error]: Failed to pull XML: ${pullRes.message}")
                    return@withContext AdbManagerResult.Failure("Failed to pull XML: ${pullRes.message}")
                }
                is AdbResult.Success -> onLog("Pull complete.")
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
            when (val pushRes = adbClient.push(localXmlFile, REMOTE_SOFTAP_XML)) {
                is AdbResult.Failure -> {
                    onLog("[ADB Error]: Failed to push XML: ${pushRes.message}")
                    return@withContext AdbManagerResult.Failure("Failed to push XML: ${pushRes.message}")
                }
                is AdbResult.Success -> onLog("Push complete.")
            }

            onStepProgress("Step 6/6: Setting permissions & rebooting device...", 88)
            onLog("Setting permissions (chmod 600)...")
            adbClient.runShell("su 0 chmod 600 $REMOTE_SOFTAP_XML")
            adbClient.runShell("su 0 chown wifi:wifi $REMOTE_SOFTAP_XML")

            localXmlFile.delete()
            onLog("SoftAP XML successfully written! Rebooting target device via 'su 0 reboot'...")
            val rebootRes = adbClient.runShell("su 0 reboot")
            if (rebootRes is AdbResult.Failure) {
                // Non-fatal: the XML write already succeeded; a dropped connection during/after
                // reboot is the expected outcome here, not a real failure.
                onLog("Note: reboot command reported '${rebootRes.message}' (device may already be restarting).")
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

    suspend fun rebootDevice(): AdbResult<String> {
        return adbClient.runShell("su 0 reboot")
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

    /**
     * Updates ONLY the passphrase, leaving SSID and everything else untouched — matches the
     * Python reference, which only ever rewrites `<string name="Passphrase">`.
     */
    private fun replacePassphraseOnly(xml: String, newPass: String): String {
        val passphraseRegex = Regex("(<string name=\"Passphrase\">)(.*?)(</string>)")
        return if (passphraseRegex.containsMatchIn(xml)) {
            passphraseRegex.replace(xml) { match ->
                "${match.groupValues[1]}$newPass${match.groupValues[3]}"
            }
        } else {
            // Passphrase tag missing entirely — append one, matching the Python fallback.
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