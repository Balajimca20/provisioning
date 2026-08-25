package com.royalenfield.ffmechanic.app.core.adb

import android.content.Context
import dadb.AdbKeyPair
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdbKeyPairProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    @Volatile
    private var cached: AdbKeyPair? = null

    @Synchronized
    fun getOrCreate(): AdbKeyPair {
        cached?.let { return it }

        val keyDir = File(context.filesDir, "adbkeys")
        if (!keyDir.exists()) {
            keyDir.mkdirs()
        }

        val privateKeyFile = File(keyDir, "adbkey")
        val publicKeyFile = File(keyDir, "adbkey.pub")

        if (!privateKeyFile.exists() || !publicKeyFile.exists()) {
            AdbKeyPair.generate(privateKeyFile, publicKeyFile)
        }

        return AdbKeyPair.read(privateKeyFile, publicKeyFile).also { cached = it }
    }
}

