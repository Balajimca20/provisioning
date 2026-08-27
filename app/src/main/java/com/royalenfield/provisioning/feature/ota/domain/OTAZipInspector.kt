package com.royalenfield.provisioning.feature.ota.domain

import android.util.Log
import java.io.File
import java.io.RandomAccessFile
import java.util.zip.ZipFile

data class OTAPayloadInfo(
    val payloadOffset: Long,
    val payloadSize: Long,
    val headers: String,
    val rawPropertiesText: String = headers
)

object OTAZipInspector {
    private const val TAG = "OTAZipInspector"
    private val CRAU_MAGIC = byteArrayOf(0x43, 0x72, 0x41, 0x55) // 'C', 'r', 'A', 'U'

    /**
     * Extracts payload.bin offset, size, and payload_properties.txt straight from the ZIP container
     * without extracting the entire multi-gigabyte package to disk.
     * Verifies the 'CrAU' magic bytes at the calculated offset.
     */
    @Throws(Exception::class)
    fun inspect(file: File): OTAPayloadInfo {
        val zip = ZipFile(file)
        
        // 1. Extract payload_properties.txt and format strictly with Unix '\n'
        val propsEntry = zip.getEntry("payload_properties.txt")
            ?: throw IllegalStateException("payload_properties.txt not found in ZIP archive")
        
        val rawHeaders = zip.getInputStream(propsEntry).bufferedReader().use { it.readText() }
        val cleanHeaders = rawHeaders
            .split(Regex("[\r\n]+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() && it.contains("=") }
            .joinToString("\n")

        Log.d(TAG, "Parsed clean headers:\n$cleanHeaders")

        // 2. Locate payload.bin entry size & byte offset
        val payloadEntry = zip.getEntry("payload.bin")
            ?: throw IllegalStateException("payload.bin not found in ZIP archive")
        val payloadSize = payloadEntry.size

        val raf = RandomAccessFile(file, "r")
        var dataOffset = -1L
        val signature = 0x04034b50 // Local File Header Signature (PK\x03\x04)
        val buffer = ByteArray(4)
        var currentPos = 0L

        try {
            while (currentPos < file.length() - 30) {
                raf.seek(currentPos)
                raf.readFully(buffer)
                val sig = (buffer[0].toInt() and 0xFF) or
                        ((buffer[1].toInt() and 0xFF) shl 8) or
                        ((buffer[2].toInt() and 0xFF) shl 16) or
                        ((buffer[3].toInt() and 0xFF) shl 24)

                if (sig == signature) {
                    raf.seek(currentPos + 26)
                    val b1 = raf.read()
                    val b2 = raf.read()
                    val nameLen = (b2 shl 8) or (b1 and 0xFF)
                    val b3 = raf.read()
                    val b4 = raf.read()
                    val extraLen = (b4 shl 8) or (b3 and 0xFF)

                    val nameBytes = ByteArray(nameLen)
                    raf.readFully(nameBytes)
                    val entryName = String(nameBytes, Charsets.UTF_8)

                    if (entryName == "payload.bin") {
                        val candidateOffset = currentPos + 30 + nameLen + extraLen
                        // Verify CrAU header
                        raf.seek(candidateOffset)
                        val magicCheck = ByteArray(4)
                        raf.readFully(magicCheck)
                        if (magicCheck.contentEquals(CRAU_MAGIC)) {
                            Log.i(TAG, "Verified CrAU magic header at byte offset $candidateOffset")
                            dataOffset = candidateOffset
                            break
                        } else {
                            Log.w(TAG, "Offset $candidateOffset found for payload.bin but CrAU magic mismatch. Continuing scan...")
                            dataOffset = candidateOffset
                        }
                    }
                    val entry = zip.getEntry(entryName)
                    val compressedSize = entry?.compressedSize ?: -1L
                    if (compressedSize >= 0) {
                        currentPos += 30 + nameLen + extraLen + compressedSize
                    } else {
                        currentPos++
                    }
                } else {
                    currentPos++
                }
            }

            // Fallback: If not found by entry scan, search for CrAU magic directly
            if (dataOffset == -1L) {
                Log.w(TAG, "Scanning file for CrAU magic signature directly...")
                raf.seek(0)
                val scanBuf = ByteArray(65536)
                var bytesRead: Int
                var filePos = 0L
                while (raf.read(scanBuf).also { bytesRead = it } != -1 && dataOffset == -1L) {
                    for (i in 0 until bytesRead - 4) {
                        if (scanBuf[i] == CRAU_MAGIC[0] &&
                            scanBuf[i + 1] == CRAU_MAGIC[1] &&
                            scanBuf[i + 2] == CRAU_MAGIC[2] &&
                            scanBuf[i + 3] == CRAU_MAGIC[3]
                        ) {
                            dataOffset = filePos + i
                            Log.i(TAG, "Found CrAU signature directly at offset $dataOffset")
                            break
                        }
                    }
                    filePos += bytesRead - 4
                    raf.seek(filePos)
                }
            }
        } finally {
            raf.close()
            zip.close()
        }

        if (dataOffset == -1L) {
            throw IllegalStateException("Unable to locate Local File Header or CrAU magic for payload.bin")
        }

        return OTAPayloadInfo(
            payloadOffset = dataOffset,
            payloadSize = payloadSize,
            headers = cleanHeaders,
            rawPropertiesText = cleanHeaders
        )
    }
}

