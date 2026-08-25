package com.royalenfield.provisioning.core.adb

import android.content.Context
import dadb.AdbKeyPair
import java.io.File

class AdbKeyPairProvider(
    private val context: Context
) {
    private var cachedKeyPair: AdbKeyPair? = null

    @Synchronized
    fun getKeyPair(): AdbKeyPair {
        cachedKeyPair?.let { return it }

        val keyFile = File(context.filesDir, "adbkey")
        val pubFile = File(context.filesDir, "adbkey.pub")

        val keyPair = if (keyFile.exists() && pubFile.exists()) {
            try {
                AdbKeyPair.read(keyFile, pubFile)
            } catch (e: Exception) {
                keyFile.delete()
                pubFile.delete()
                AdbKeyPair.generate(keyFile, pubFile)
            }
        } else {
            AdbKeyPair.generate(keyFile, pubFile)
        }

        cachedKeyPair = keyPair as AdbKeyPair?
        return keyPair
    }
}
