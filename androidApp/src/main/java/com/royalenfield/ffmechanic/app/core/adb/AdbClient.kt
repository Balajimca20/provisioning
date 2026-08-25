package com.royalenfield.ffmechanic.app.core.adb

import dadb.Dadb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

sealed class AdbResult {
    data class Success(val stdout: String = "") : AdbResult()
    data class Failure(val message: String) : AdbResult()
}


@Singleton
class AdbClient @Inject constructor(
    private val keyPairProvider: AdbKeyPairProvider,
) {

    private var dadb: Dadb? = null
    private var connectedHost: String? = null
    private var connectedPort: Int? = null

    suspend fun connect(host: String, port: Int = 5555): AdbResult = withContext(Dispatchers.IO) {
        try {
            // If already connected to this host:port, reuse the connection
            if (connectedHost == host && connectedPort == port && isSocketValid()) {
                return@withContext AdbResult.Success("Already connected to $host:$port")
            }

            // Close old connection if switching hosts
            disconnect()

            // Create a direct TCP connection to the remote ADB endpoint.
            dadb = Dadb.create(host, port, keyPairProvider.getOrCreate())
            
            connectedHost = host
            connectedPort = port

            val testResult = shell("echo ok")
            return@withContext if (testResult.contains("ok", ignoreCase = true) || testResult.isNotBlank()) {
                AdbResult.Success("Connected to $host:$port")
            } else {
                AdbResult.Failure("adb connect failed: empty shell response")
            }
        } catch (e: Exception) {
            disconnect()
            AdbResult.Failure("adb connect failed: ${e.message}")
        }
    }

    /**
     * Mirrors `adb root` (step 1).
     */
    suspend fun root(): AdbResult = withContext(Dispatchers.IO) {
        try {
            checkConnected()
            val output = shell("su 0 id")
            if (output.contains("uid=0")) {
                AdbResult.Success(output.trim())
            } else {
                AdbResult.Failure("adb root failed: root permissions not granted")
            }
        } catch (e: Exception) {
            AdbResult.Failure("adb root failed: ${e.message}")
        }
    }

    /**
     * Mirrors `adb pull <remotePath>` into a local File (step 2).
     */
    suspend fun pull(remotePath: String, localFile: File): AdbResult = withContext(Dispatchers.IO) {
        try {
            checkConnected()
            dadb!!.pull(localFile, remotePath)
            AdbResult.Success()
        } catch (e: Exception) {
            AdbResult.Failure("adb pull failed: ${e.message}")
        }
    }

    /**
     * Mirrors `adb push <local> <remote>` (used both for the XML config and the OTA zip).
     */
    suspend fun push(
        localFile: File,
        remotePath: String,
        onProgress: ((bytesTransferred: Long, totalBytes: Long) -> Unit)? = null,
    ): AdbResult = withContext(Dispatchers.IO) {
        try {
            checkConnected()
            val totalBytes = localFile.length().coerceAtLeast(1L)
            dadb!!.push(localFile, remotePath)
            onProgress?.invoke(totalBytes, totalBytes)
            AdbResult.Success()
        } catch (e: Exception) {
            AdbResult.Failure("adb push failed: ${e.message}")
        }
    }

    /**
     * Mirrors `adb shell <cmd>` returning captured stdout.
     */
    suspend fun runShell(command: String): AdbResult = withContext(Dispatchers.IO) {
        try {
            checkConnected()
            val output = shell(command)
            AdbResult.Success(output.trim())
        } catch (e: Exception) {
            AdbResult.Failure(e.message ?: "adb shell failed")
        }
    }

    /**
     * Streaming shell for the OTA engine command.
     */
    fun shellStream(command: String): Flow<String> = flow {
        checkConnected()
        val output = shell(command)
        output.lineSequence().forEach { emit(it) }
    }

    /**
     * Mirrors `adb reboot` (final step of both the Wi-Fi and OTA workflows).
     */
    suspend fun reboot(): AdbResult = withContext(Dispatchers.IO) {
        try {
            checkConnected()
            shell("reboot")
            AdbResult.Success()
        } catch (e: Exception) {
            AdbResult.Failure("adb reboot failed: ${e.message}")
        }
    }

    fun disconnect() {
        try {
            dadb?.close()
        } catch (e: Exception) {
            // Ignore
        }
        dadb = null
        connectedHost = null
        connectedPort = null
    }

    private fun shell(command: String): String {
        val result = dadb!!.shell(command)
        return result.output
    }

    private fun isSocketValid(): Boolean {
        return dadb != null
    }

    private fun checkConnected() {
        if (!isSocketValid()) {
            throw IllegalStateException("Not connected — call connect(host) first")
        }
    }
}


















