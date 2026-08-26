package com.royalenfield.provisioning.core.adb

import android.util.Log
import dadb.AdbShellPacket
import dadb.Dadb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.charset.StandardCharsets

sealed class AdbResult<out T> {
    data class Success<out T>(val data: T) : AdbResult<T>()
    data class Failure(val message: String, val throwable: Throwable? = null) : AdbResult<Nothing>()
}

class AdbClient(
    private val keyPairProvider: AdbKeyPairProvider
) {
    private var dadbInstance: Dadb? = null

    val isConnected: Boolean
        get() = dadbInstance != null

    suspend fun connect(host: String = "192.168.1.1", port: Int = 5555): AdbResult<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                disconnect()
                Log.d(TAG, "Connecting to ADB daemon at $host:$port...")
                val keyPair = keyPairProvider.getKeyPair()
                val dadb = Dadb.create(host, port, keyPair)
                dadbInstance = dadb
                Log.d(TAG, "ADB Connection established")
                AdbResult.Success(true)
            } catch (e: Exception) {
                Log.e(TAG, "ADB Connection failed: ${e.message}", e)
                AdbResult.Failure(e.message ?: "Failed to connect to ADB at $host:$port", e)
            }
        }

    suspend fun runShell(command: String): AdbResult<String> = withContext(Dispatchers.IO) {
        val dadb = dadbInstance ?: return@withContext AdbResult.Failure("ADB not connected")
        try {
            Log.d(TAG, "Executing shell: $command")
            val response = dadb.shell(command)
            if (response.exitCode == 0) {
                AdbResult.Success(response.output)
            } else {
                AdbResult.Failure("Command exited with code ${response.exitCode}: ${response.errorOutput}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Shell execution error: ${e.message}", e)
            AdbResult.Failure(e.message ?: "Shell command execution failed", e)
        }
    }

    fun runShellStreaming(command: String): Flow<String> = callbackFlow {
        val dadb = dadbInstance
        if (dadb == null) {
            trySend("ERROR: ADB not connected")
            close()
            return@callbackFlow
        }

        val session = withContext(Dispatchers.IO) {
            dadb.openShell(command)
        }

        val readerThread = Thread {
            try {
                loop@ while (true) {
                    when (val packet = session.read()) {
                        is AdbShellPacket.StdOut -> {
                            emitLines("", packet.payload) { trySend(it) }
                        }
                        is AdbShellPacket.StdError -> {
                            emitLines("ERR: ", packet.payload) { trySend(it) }
                        }
                        is AdbShellPacket.Exit -> {
                            break@loop
                        }
                    }
                }
            } catch (e: Exception) {
                trySend("STREAM_ERROR: ${e.message}")
            } finally {
                try {
                    session.close()
                } catch (e: Exception) {
                    // Ignore
                }
                close()
            }
        }
        readerThread.start()

        awaitClose {
            // Thread finishes when session closes or command ends
        }
    }

    private inline fun emitLines(prefix: String, payload: ByteArray, send: (String) -> Unit) {
        String(payload, StandardCharsets.UTF_8).split("\n").forEach { line ->
            if (line.isNotBlank()) send("$prefix${line.trim()}")
        }
    }

    suspend fun pull(remotePath: String, localFile: File): AdbResult<File> =
        withContext(Dispatchers.IO) {
            val dadb = dadbInstance ?: return@withContext AdbResult.Failure("ADB not connected")
            try {
                Log.d(TAG, "Pulling $remotePath -> ${localFile.absolutePath}")
                dadb.pull(localFile, remotePath)
                AdbResult.Success(localFile)
            } catch (e: Exception) {
                Log.e(TAG, "Pull failed: ${e.message}", e)
                AdbResult.Failure(e.message ?: "Failed to pull $remotePath", e)
            }
        }

    suspend fun restartAsRoot(): AdbResult<String> = withContext(Dispatchers.IO) {
        val dadb = dadbInstance ?: return@withContext AdbResult.Failure("ADB not connected")
        try {
            Log.d(TAG, "Escalating root permissions...")
            val rootRes = runShell("root")
            if (rootRes is AdbResult.Success) {
                return@withContext rootRes
            }
            // Check if already root via whoami or id -u
            val whoamiRes = runShell("whoami || id -u")
            if (whoamiRes is AdbResult.Success && (whoamiRes.data.contains("root") || whoamiRes.data.trim() == "0")) {
                return@withContext AdbResult.Success("adbd is running as root")
            }
            // Non-blocking escalation warning
            AdbResult.Success("Root escalation attempted (${(rootRes as? AdbResult.Failure)?.message ?: "status ok"})")
        } catch (e: Exception) {
            Log.w(TAG, "Root escalation error (non-fatal): ${e.message}")
            AdbResult.Success("Root escalation skipped: ${e.message}")
        }
    }

    suspend fun reboot(): AdbResult<String> = runShell("reboot")

    suspend fun pushFile(
        localFile: File,
        remotePath: String,
        onProgress: ((sent: Long, total: Long) -> Unit)? = null
    ): AdbResult<Boolean> = withContext(Dispatchers.IO) {
        val dadb = dadbInstance ?: return@withContext AdbResult.Failure("ADB not connected")
        try {
            Log.d(TAG, "Pushing ${localFile.absolutePath} -> $remotePath")
            val totalBytes = localFile.length()
            onProgress?.invoke(0L, totalBytes)
            dadb.push(localFile, remotePath)
            onProgress?.invoke(totalBytes, totalBytes)
            AdbResult.Success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Push failed: ${e.message}", e)
            AdbResult.Failure(e.message ?: "Failed to push $remotePath", e)
        }
    }

    suspend fun push(localFile: File, remotePath: String): AdbResult<Boolean> =
        pushFile(localFile, remotePath, null)

    fun disconnect() {
        try {
            dadbInstance?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing ADB connection: ${e.message}")
        } finally {
            dadbInstance = null
        }
    }

    companion object {
        private const val TAG = "AdbClient"
    }
}
