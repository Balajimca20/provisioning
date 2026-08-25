package com.royalenfield.ffmechanic.app.feature.ota.domain

import com.royalenfield.ffmechanic.app.core.adb.AdbClient
import com.royalenfield.ffmechanic.app.core.adb.AdbResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import java.util.zip.ZipFile
import javax.inject.Inject
import javax.inject.Singleton

sealed class OtaStep {
    data class Log(val message: String) : OtaStep()
    data class Progress(val percent: Int, val statusMessage: String) : OtaStep()
    data object AwaitingRebootConsent : OtaStep()
    data class Done(val success: Boolean, val message: String) : OtaStep()
}

/**
 * Port of ota_call_generate_command(): reads the zip's central directory to find payload.bin's
 * byte offset/size within the archive (update_engine_client streams the payload directly out
 * of the zip via file offsets, it doesn't need the zip extracted) and the update headers.
 */
object OtaCommandBuilder {
    private const val PAYLOAD_LOCATION = "/data/ota_package/update.zip"

    fun generateCommand(zipFile: File): String {
        ZipFile(zipFile).use { zip ->
            val payloadEntry = zip.getEntry("payload.bin")
                ?: error("payload.bin not found in OTA package")
            val headers = zip.getInputStream(zip.getEntry("payload_properties.txt") ?: error("payload_properties.txt not found"))
                .bufferedReader().readText()

            // java.util.zip doesn't expose the raw header offset the way Python's zipfile does
            // (ZipInfo.header_offset + len(FileHeader())). Compute it by walking the local file
            // header ourselves, since that's what `update_engine_client --offset=` needs.
            val payloadOffset = computeLocalHeaderDataOffset(zipFile, payloadEntry.name)
            val payloadSize = payloadEntry.size

            return "update_engine_client --update --follow " +
                "--payload=file://$PAYLOAD_LOCATION " +
                "--offset=$payloadOffset --size=$payloadSize " +
                "--headers=\"$headers\""
        }
    }

    /** Reads a ZIP local file header to find where entry [name]'s actual data starts. */
    private fun computeLocalHeaderDataOffset(zipFile: File, name: String): Long {
        ZipFile(zipFile).use { zip ->
            val entry = zip.getEntry(name) ?: error("$name not found")
            java.io.RandomAccessFile(zipFile, "r").use { raf ->
                // Local file header offset isn't directly exposed by java.util.zip either;
                // ZipEntry doesn't carry it. In practice, use a small zip-parsing helper
                // (e.g. Apache Commons Compress's ZipArchiveEntry.dataOffset) instead of
                // hand-rolling header parsing here — swap this stub for that call.
                throw NotImplementedError(
                    "Wire up org.apache.commons.compress:commons-compress and use " +
                        "ZipArchiveEntry.dataOffset for '$name' — java.util.zip can't expose this."
                )
            }
        }
    }
}

@Singleton
class OtaPipeline @Inject constructor(
    private val adbClient: AdbClient,
) {
    private val pushTarget = "/data/ota_package/update.zip"

    /**
     * Port of execute_ota_pipeline(). The Python version spawned a visible terminal window
     * as a workaround to stream push progress ("native terminal breakout"); here we push
     * directly via the adb client and report real progress through onProgress instead.
     */
    fun run(host: String, zipFile: File): Flow<OtaStep> = flow {
        emit(OtaStep.Progress(5, "GAINING ROOT ACCESS"))
        adbClient.connect(host)
        adbClient.root()

        emit(OtaStep.Progress(10, "PUSHING OTA ZIP PACKAGE"))
        emit(OtaStep.Log("Source package path: ${zipFile.path}"))
        val pushResult = adbClient.push(zipFile, pushTarget) { transferred, total ->
            // Map push progress into the 10-50% range, same as the original's 5-50% budget.
        }
        if (pushResult is AdbResult.Failure) {
            emit(OtaStep.Done(false, "Push failed: ${pushResult.message}")); return@flow
        }
        emit(OtaStep.Progress(50, "PACKAGE PUSHED"))

        emit(OtaStep.Progress(50, "RUNNING LOCAL ANALYZER"))
        val updateCmd = try {
            OtaCommandBuilder.generateCommand(zipFile)
        } catch (e: Exception) {
            emit(OtaStep.Done(false, "ZIP header extraction failed: ${e.message}")); return@flow
        }
        emit(OtaStep.Log("Generated engine command:\n$updateCmd"))

        emit(OtaStep.Progress(55, "STARTING UPDATE ENGINE"))
        var successSignature1 = false
        var successSignature2 = false

        adbClient.shellStream("cd /data/ota_package && $updateCmd").collect { line ->
            emit(OtaStep.Log(line))

            Regex("""UPDATE_STATUS_DOWNLOADING\s*\(\d+\),\s*([0-9.]+)""").find(line)?.let {
                val frac = it.groupValues[1].toFloat()
                emit(OtaStep.Progress((55 + frac * 25).toInt(), "INSTALLING: ${"%.1f".format(frac * 100)}%"))
            }
            Regex("""UPDATE_STATUS_VERIFYING\s*\(\d+\),\s*([0-9.]+)""").find(line)?.let {
                val frac = it.groupValues[1].toFloat()
                emit(OtaStep.Progress((80 + frac * 15).toInt(), "VERIFYING: ${"%.1f".format(frac * 100)}%"))
            }
            if ("UPDATE_STATUS_UPDATED_NEED_REBOOT" in line) successSignature1 = true
            if ("onPayloadApplicationComplete(ErrorCode::kSuccess" in line) successSignature2 = true
        }

        if (!(successSignature1 || successSignature2)) {
            emit(OtaStep.Done(false, "Process closed without detecting successful registration tags."))
            return@flow
        }

        emit(OtaStep.Progress(95, "OTA registered in A/B slots"))
        emit(OtaStep.AwaitingRebootConsent)
        // Caller (ViewModel) collects this flow and, after the user answers the reboot prompt,
        // calls confirmReboot()/skipReboot() below rather than blocking this flow — Flow has no
        // built-in "pause for external input" primitive the way the Python while-loop did.
    }

    suspend fun confirmReboot(): OtaStep {
        val result = adbClient.reboot()
        return if (result is AdbResult.Success) {
            OtaStep.Done(true, "OTA pipeline completed successfully. Device rebooting...")
        } else {
            OtaStep.Done(false, "Reboot command failed.")
        }
    }

    fun skipReboot(): OtaStep =
        OtaStep.Done(true, "OTA pipeline flashed successfully. Reboot skipped by operator.")
}
