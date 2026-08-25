package com.royalenfield.provisioning.core.adb

import android.util.Log
import dadb.Dadb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

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

    suspend fun push(localFile: File, remotePath: String): AdbResult<Boolean> =
        withContext(Dispatchers.IO) {
            val dadb = dadbInstance ?: return@withContext AdbResult.Failure("ADB not connected")
            try {
                Log.d(TAG, "Pushing ${localFile.absolutePath} -> $remotePath")
                dadb.push(localFile, remotePath)
                AdbResult.Success(true)
            } catch (e: Exception) {
                Log.e(TAG, "Push failed: ${e.message}", e)
                AdbResult.Failure(e.message ?: "Failed to push $remotePath", e)
            }
        }

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
