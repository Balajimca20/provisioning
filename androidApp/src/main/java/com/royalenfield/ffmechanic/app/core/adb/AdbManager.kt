package com.royalenfield.ffmechanic.app.core.adb

import dadb.Dadb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

sealed class AdbManagerResult {
    data class Success(val ssid: String, val macAddress: String) : AdbManagerResult()
    data class Failure(val message: String) : AdbManagerResult()
}

@Singleton
class AdbManager @Inject constructor(
    private val keyPairProvider: AdbKeyPairProvider,
) {

    private data class ParsedXml(val updatedXml: String, val ssid: String, val macAddress: String)

    /**
     * Connects to target device, runs root checks, and executes pull/edit/push workflow.
     */
    suspend fun connectAndUpdateWifi(
        host: String = "192.168.1.1",
        port: Int = 5555,
        localCacheDir: File,
        newPassword: String,
        onLog: (suspend (String) -> Unit)? = null,
    ): AdbManagerResult = withContext(Dispatchers.IO) {
        var dadb: Dadb? = null
        try {
            onLog?.invoke("adb connecting to $host:$port...")
            dadb = Dadb.create(host, port, keyPairProvider.getOrCreate())

            onLog?.invoke("Escalating privileges with 'su 0 id'...")
            val rootCheck = dadb.shell("su 0 id")
            if (!rootCheck.output.contains("uid=0")) {
                return@withContext AdbManagerResult.Failure("Failed to obtain root permissions on target device.")
            }

            val remotePath = "/data/misc/apexdata/com.android.wifi/WifiConfigStoreSoftAp.xml"
            val localXml = File(localCacheDir, "WifiConfigStoreSoftAp.xml")

            onLog?.invoke("Pulling WifiConfigStoreSoftAp.xml...")
            dadb.pull(localXml, remotePath)
            if (!localXml.exists()) {
                return@withContext AdbManagerResult.Failure("Pulled XML file is missing locally.")
            }

            onLog?.invoke("Parsing XML and updating passphrase...")
            val parsed = parseAndUpdatePassphrase(localXml, newPassword)
            localXml.writeText(parsed.updatedXml)

            val mac = if (parsed.macAddress.equals("N/A", ignoreCase = true) || parsed.macAddress.isBlank()) {
                dadb.shell("cat /sys/class/net/wlan1/address").output.trim().uppercase().ifBlank { "N/A" }
            } else {
                parsed.macAddress
            }

            onLog?.invoke("Pushing updated WifiConfigStoreSoftAp.xml...")
            dadb.push(localXml, remotePath)

            onLog?.invoke("Setting XML permissions and rebooting target...")
            dadb.shell("chmod 600 $remotePath")
            dadb.shell("reboot")

            if (localXml.exists()) {
                localXml.delete()
            }

            AdbManagerResult.Success(
                ssid = parsed.ssid,
                macAddress = mac,
            )
        } catch (e: Exception) {
            AdbManagerResult.Failure(e.message ?: "Unknown ADB error")
        } finally {
            dadb?.close()
        }
    }

    private fun parseAndUpdatePassphrase(xmlFile: File, newPassword: String): ParsedXml {
        var ssid = "N/A"
        var mac = "N/A"
        var passphraseUpdated = false

        val original = xmlFile.readText()
        val nameRegex = Regex("""<string name=\"([^\"]+)\">([^<]*)</string>""")
        val sb = StringBuffer()
        val matcher = nameRegex.toPattern().matcher(original)
        while (matcher.find()) {
            val name = matcher.group(1)
            val value = matcher.group(2)
            when (name) {
                "Passphrase" -> {
                    passphraseUpdated = true
                    matcher.appendReplacement(sb, """<string name=\"Passphrase\">$newPassword</string>""")
                }
                "SSID", "Ssid" -> ssid = value ?: "N/A"
                "MacAddress", "MACAddress", "Mac" -> mac = value ?: "N/A"
            }
        }
        matcher.appendTail(sb)

        var updated = sb.toString()
        if (!passphraseUpdated) {
            updated = updated.replace(
                "</map>",
                "    <string name=\"Passphrase\">$newPassword</string>\n</map>",
            )
        }

        return ParsedXml(updated, ssid, mac)
    }
}

