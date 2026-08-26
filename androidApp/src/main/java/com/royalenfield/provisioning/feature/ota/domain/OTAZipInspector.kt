package com.royalenfield.provisioning.feature.ota.domain

import java.io.File
import java.io.RandomAccessFile
import java.util.zip.ZipFile

data class OTAPayloadInfo(
    val payloadOffset: Long,
    val payloadSize: Long,
    val headers: String
)

object OTAZipInspector {
    /**
     * Extracts payload.bin offset, size, and payload_properties.txt straight from the ZIP container
     * without extracting the entire multi-gigabyte package to disk.
     */
    @Throws(Exception::class)
    fun inspect(file: File): OTAPayloadInfo {
        val zip = ZipFile(file)
        
        // 1. Extract payload_properties.txt headers
        val propsEntry = zip.getEntry("payload_properties.txt")
            ?: throw IllegalStateException("payload_properties.txt not found in ZIP archive")
        val headers = zip.getInputStream(propsEntry).bufferedReader().use { it.readText() }
            .replace("\r\n", " ")
            .replace("\n", " ")
            .trim()

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
                    val entryName = String(nameBytes)

                    if (entryName == "payload.bin") {
                        dataOffset = currentPos + 30 + nameLen + extraLen
                        break
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
        } finally {
            raf.close()
            zip.close()
        }

        if (dataOffset == -1L) {
            throw IllegalStateException("Unable to locate Local File Header data offset for payload.bin")
        }

        return OTAPayloadInfo(
            payloadOffset = dataOffset,
            payloadSize = payloadSize,
            headers = headers
        )
    }
}
