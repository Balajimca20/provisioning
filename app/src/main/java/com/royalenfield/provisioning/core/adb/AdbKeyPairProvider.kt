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
        val cached = cachedKeyPair
        if (cached != null) return cached

        val keyFile = File(context.filesDir, "adbkey")
        val pubFile = File(context.filesDir, "adbkey.pub")

        val keyPair: AdbKeyPair = try {
            if (!keyFile.exists() || !pubFile.exists()) {
                AdbKeyPair.generate(keyFile, pubFile)
            }
            AdbKeyPair.read(keyFile, pubFile)
        } catch (e: Exception) {
            keyFile.delete()
            pubFile.delete()
            AdbKeyPair.generate(keyFile, pubFile)
            AdbKeyPair.read(keyFile, pubFile)
        }

        cachedKeyPair = keyPair
        return keyPair
    }
}
