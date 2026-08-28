package com.royalenfield.provisioning.core.adb

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.File
import java.io.StringReader
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

sealed class AdbManagerResult {
    data class Success(
        val ssid: String,
        val macAddress: String,
        val newPassword: String,
        val message: String
    ) : AdbManagerResult()
    data class Failure(val message: String) : AdbManagerResult()
}

class AdbManager(
    private val context: Context,
    private val adbClient: AdbClient
) {
    companion object {
        private const val TAG = "AdbManager"
        const val REMOTE_SOFTAP_APEX_XML =
            "/data/misc/apexdata/com.android.wifi/WifiConfigStoreSoftAp.xml"
        const val REMOTE_SOFTAP_LEGACY_XML =
            "/data/misc/wifi/WifiConfigStoreSoftAp.xml"
    }

    suspend fun verifyRootAccess(): Boolean = withContext(Dispatchers.IO) {
        when (val result = adbClient.runShell("su 0 id || whoami || id -u")) {
            is AdbResult.Success -> {
                val out = result.data.trim()
                out.contains("uid=0(root)") || out.contains("root") || out == "0"
            }
            is AdbResult.Failure -> false
        }
    }

    suspend fun readSoftApXml(): Result<String> = withContext(Dispatchers.IO) {
        val tempFile = File(context.cacheDir, "temp_softap_read.xml")
        val targetPath = resolveSoftApRemotePath()
        when (val pullResult = adbClient.pull(targetPath, tempFile)) {
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

    private suspend fun resolveSoftApRemotePath(): String = withContext(Dispatchers.IO) {
        val checkApex = adbClient.runShell("ls $REMOTE_SOFTAP_APEX_XML")
        if (checkApex is AdbResult.Success && !checkApex.data.contains("No such file")) {
            REMOTE_SOFTAP_APEX_XML
        } else {
            val checkLegacy = adbClient.runShell("ls $REMOTE_SOFTAP_LEGACY_XML")
            if (checkLegacy is AdbResult.Success && !checkLegacy.data.contains("No such file")) {
                REMOTE_SOFTAP_LEGACY_XML
            } else {
                REMOTE_SOFTAP_APEX_XML
            }
        }
    }

    suspend fun getHardwareMacAddress(onLog: (String) -> Unit = {}): String = withContext(Dispatchers.IO) {
        try {
            val res1 = adbClient.runShell("cat /sys/class/net/wlan1/address")
            if (res1 is AdbResult.Success && res1.data.trim().isNotEmpty() && !res1.data.contains("No such")) {
                val mac = res1.data.trim().uppercase()
                if (mac.contains(":")) return@withContext mac
            }

            val res0 = adbClient.runShell("cat /sys/class/net/wlan0/address")
            if (res0 is AdbResult.Success && res0.data.trim().isNotEmpty() && !res0.data.contains("No such")) {
                val mac = res0.data.trim().uppercase()
                if (mac.contains(":")) return@withContext mac
            }

            val ipLink = adbClient.runShell("ip link show wlan1 || ip link show wlan0")
            if (ipLink is AdbResult.Success) {
                val macRegex = Regex("([0-9a-fA-F]{2}:[0-9a-fA-F]{2}:[0-9a-fA-F]{2}:[0-9a-fA-F]{2}:[0-9a-fA-F]{2}:[0-9a-fA-F]{2})")
                val match = macRegex.find(ipLink.data)
                if (match != null) {
                    return@withContext match.value.uppercase()
                }
            }
            "N/A"
        } catch (e: Exception) {
            onLog("Failed to read sysfs MAC address: ${e.message}")
            "N/A"
        }
    }

    /**
     * Executes the complete Python WifiUpdateWorker pipeline:
     * 0. Ensure ADB connected (default 192.168.1.1:5555)
     * 1. Escalate privileges with `adb root`
     * 2. Pull WifiConfigStoreSoftAp.xml
     * 3. Parse XML & modify Passphrase / extract SSID & MAC (or query /sys/class/net/wlan1/address)
     * 4. Push updated XML back to device
     * 5. Set proper file permissions (chmod 600 / chown wifi:wifi)
     * 6. Delete local XML file
     * 7. Reboot target device via `adb reboot`
     */
    suspend fun executeWifiPasswordUpdate(
        vin: String,
        newPassword: String,
        onLog: (String) -> Unit = {}
    ): AdbManagerResult = withContext(Dispatchers.IO) {
        val xmlFilename = "WifiConfigStoreSoftAp.xml"
        val localXmlFile = File(context.cacheDir, xmlFilename)

        try {
            onLog("=== Starting Wi-Fi Password Update Workflow ===")

            // Step 0: Ensure ADB is connected
            if (!adbClient.isConnected) {
                onLog("adb connecting to 192.168.1.1:5555...")
                when (val connectRes = adbClient.connect("192.168.1.1", 5555)) {
                    is AdbResult.Failure -> {
                        onLog("Notice: Direct connect to 192.168.1.1: ${connectRes.message}. Checking existing daemon...")
                    }
                    is AdbResult.Success -> {
                        onLog("adb connected successfully.")
                    }
                }
            }

            // Step 1: Escalating privileges with adb root
            onLog("Escalating privileges with 'adb root'...")
            val rootRes = adbClient.restartAsRoot()
            if (rootRes is AdbResult.Failure) {
                onLog("Root restart notice: ${rootRes.message}")
                val hasRoot = verifyRootAccess()
                if (!hasRoot) {
                    onLog("Warning: adbd root check failed, attempting su elevation...")
                } else {
                    onLog("Privileges verified as root.")
                }
            } else {
                onLog("Privileges escalated: ${(rootRes as AdbResult.Success).data}")
            }

            // Step 2: Pull configuration XML
            val remoteTargetPath = resolveSoftApRemotePath()
            onLog("Pulling $remoteTargetPath...")
            when (val pullRes = adbClient.pull(remoteTargetPath, localXmlFile)) {
                is AdbResult.Failure -> {
                    // Fallback to cat
                    val catRes = adbClient.runShell("cat $remoteTargetPath")
                    if (catRes is AdbResult.Success && catRes.data.contains("<WifiConfigStoreSoftAp")) {
                        localXmlFile.writeText(catRes.data)
                    } else {
                        val errMsg = "Error: Local XML file '$xmlFilename' not found after pull (${pullRes.message})."
                        onLog(errMsg)
                        return@withContext AdbManagerResult.Failure(errMsg)
                    }
                }
                is AdbResult.Success -> {}
            }

            if (!localXmlFile.exists() || localXmlFile.length() == 0L) {
                val errMsg = "Error: Local XML file '$xmlFilename' not found after pull."
                onLog(errMsg)
                return@withContext AdbManagerResult.Failure(errMsg)
            }

            // Step 3: XML Parsing & Modification (Python ElementTree equivalent)
            onLog("Parsing '$xmlFilename'...")
            val xmlContent = localXmlFile.readText()

            var extractedSsid = "N/A"
            var extractedMac = "N/A"
            val modifiedXml = updateXmlTree(
                originalXml = xmlContent,
                newPassword = newPassword,
                onExtract = { ssid, mac ->
                    extractedSsid = ssid
                    extractedMac = mac
                },
                onLog = onLog
            )

            // If MAC not found in XML, query /sys/class/net/wlan1/address directly
            if (extractedMac == "N/A" || extractedMac == "Unknown" || extractedMac.isBlank()) {
                onLog("MAC not found in XML. Querying /sys/class/net/wlan1/address directly...")
                extractedMac = getHardwareMacAddress(onLog)
            }

            localXmlFile.writeText(modifiedXml)
            onLog("XML updated with new password successfully.")
            onLog("Extracted Info -> SSID: $extractedSsid | MAC: $extractedMac")

            // Step 4: Push updated XML back
            val pushRemoteDir = remoteTargetPath.substringBeforeLast('/') + "/"
            onLog("Pushing updated $xmlFilename to $pushRemoteDir...")
            when (val pushRes = adbClient.pushFile(localXmlFile, remoteTargetPath)) {
                is AdbResult.Failure -> {
                    // Fallback using echo/cat with root
                    onLog("Push fallback: writing XML content via shell...")
                    val escapedXml = modifiedXml.replace("'", "'\\''")
                    val writeRes = adbClient.runShell("printf '%s' '$escapedXml' > $remoteTargetPath")
                    if (writeRes is AdbResult.Failure) {
                        return@withContext AdbManagerResult.Failure("Push failed: ${pushRes.message}")
                    }
                }
                is AdbResult.Success -> {}
            }

            // Set file permissions
            adbClient.runShell("chmod 600 $remoteTargetPath || true")
            adbClient.runShell("chown wifi:wifi $remoteTargetPath || chown 1010:1010 $remoteTargetPath || true")

            // Step 6: Delete the local XML file before rebooting
            deleteLocalXmlFile(localXmlFile, xmlFilename, onLog)

            // Step 7: Reboot target device via adb reboot
            onLog("Rebooting target device via 'adb reboot'...")
            adbClient.reboot()

            onLog("=== Workflow Completed Successfully! ===")
            AdbManagerResult.Success(
                ssid = extractedSsid,
                macAddress = extractedMac,
                newPassword = newPassword,
                message = "Wi-Fi Password updated successfully! Target device rebooting..."
            )
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected Execution Error: ${e.message}", e)
            val err = "Unexpected Execution Error: ${e.message}"
            onLog(err)
            deleteLocalXmlFile(localXmlFile, xmlFilename, onLog)
            AdbManagerResult.Failure(err)
        }
    }

    private fun deleteLocalXmlFile(file: File, xmlFilename: String, onLog: (String) -> Unit) {
        try {
            if (file.exists()) {
                file.delete()
                onLog("Deleted local file '$xmlFilename' successfully.")
            } else {
                onLog("File '$xmlFilename' does not exist locally to delete.")
            }
        } catch (e: Exception) {
            onLog("Failed to delete local XML file: ${e.message}")
        }
    }

    private fun updateXmlTree(
        originalXml: String,
        newPassword: String,
        onExtract: (ssid: String, mac: String) -> Unit,
        onLog: (String) -> Unit
    ): String {
        try {
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = false
            val builder = factory.newDocumentBuilder()
            val doc: Document = builder.parse(InputSource(StringReader(originalXml)))
            doc.documentElement.normalize()

            var passphraseUpdated = false
            var ssid = "N/A"
            var macAddress = "N/A"

            val stringNodes = doc.getElementsByTagName("string")
            for (i in 0 until stringNodes.length) {
                val node = stringNodes.item(i) as? Element ?: continue
                val nameAttr = node.getAttribute("name")
                when {
                    nameAttr.equals("Passphrase", ignoreCase = true) -> {
                        node.setTextContent(newPassword)
                        passphraseUpdated = true
                    }
                    nameAttr.equals("PreSharedKey", ignoreCase = true) -> {
                        // Standard Android format with quotes
                        node.setTextContent("\"$newPassword\"")
                        passphraseUpdated = true
                    }
                    nameAttr.equals("SSID", ignoreCase = true) || nameAttr.equals("Ssid", ignoreCase = true) -> {
                        val rawSsid = (node.textContent ?: "").replace("\"", "").replace("&quot;", "").trim()
                        if (rawSsid.isNotEmpty()) ssid = rawSsid
                    }
                    nameAttr.equals("MacAddress", ignoreCase = true) ||
                    nameAttr.equals("MACAddress", ignoreCase = true) ||
                    nameAttr.equals("Mac", ignoreCase = true) -> {
                        val rawMac = (node.textContent ?: "").replace("\"", "").trim()
                        if (rawMac.isNotEmpty()) macAddress = rawMac
                    }
                }
            }

            if (!passphraseUpdated) {
                onLog("Warning: <string name=\"Passphrase\"> tag not found. Appending standard node.")
                val newPassElem = doc.createElement("string")
                newPassElem.setAttribute("name", "Passphrase")
                newPassElem.setTextContent(newPassword)
                doc.documentElement.appendChild(newPassElem)
            }

            onExtract(ssid, macAddress)

            val transformer = TransformerFactory.newInstance().newTransformer()
            transformer.setOutputProperty(OutputKeys.INDENT, "yes")
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8")
            val writer = StringWriter()
            transformer.transform(DOMSource(doc), StreamResult(writer))
            return writer.toString()
        } catch (e: Exception) {
            onLog("DOM parse notice (${e.message}), running regex substitution...")
            return fallbackRegexUpdate(originalXml, newPassword, onExtract)
        }
    }

    private fun fallbackRegexUpdate(
        xml: String,
        newPassword: String,
        onExtract: (ssid: String, mac: String) -> Unit
    ): String {
        var updated = xml
        var ssid = "N/A"
        var mac = "N/A"

        val ssidMatch = Regex("<string name=\"(?:SSID|Ssid)\">(?:&quot;)?(.*?)(?:&quot;)?</string>").find(xml)
        if (ssidMatch != null) {
            ssid = ssidMatch.groupValues[1].replace("\"", "").trim()
        }

        val macMatch = Regex("<string name=\"(?:MacAddress|MACAddress|Mac)\">(.*?)</string>").find(xml)
        if (macMatch != null) {
            mac = macMatch.groupValues[1].replace("\"", "").trim()
        }

        val passPattern = Regex("<string name=\"Passphrase\">.*?</string>")
        if (passPattern.containsMatchIn(updated)) {
            updated = passPattern.replace(updated, "<string name=\"Passphrase\">$newPassword</string>")
        } else {
            val pskPattern = Regex("<string name=\"PreSharedKey\">&quot;.*?&quot;</string>")
            if (pskPattern.containsMatchIn(updated)) {
                updated = pskPattern.replace(updated, "<string name=\"PreSharedKey\">&quot;$newPassword&quot;</string>")
            } else {
                updated = updated.replace("</WifiConfigStoreSoftAp>", "  <string name=\"Passphrase\">$newPassword</string>\n</WifiConfigStoreSoftAp>")
            }
        }

        onExtract(ssid, mac)
        return updated
    }

    suspend fun updateSoftApCredentials(
        newSsid: String,
        newPassphrase: String,
        onLog: (String) -> Unit = {}
    ): AdbManagerResult = withContext(Dispatchers.IO) {
        executeWifiPasswordUpdate(
            vin = "N/A",
            newPassword = newPassphrase,
            onLog = onLog
        )
    }

    suspend fun rebootDevice(): AdbResult<String> {
        return adbClient.reboot()
    }
}
