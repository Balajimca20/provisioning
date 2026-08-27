package com.royalenfield.provisioning.core.adb

import android.util.Log
import dadb.AdbShellPacket
import dadb.Dadb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
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

    // Remembered so restartAsRoot() can reconnect to the same target after adbd restarts —
    // the root restart always kills the current TCP connection.
    private var connectedHost: String? = null
    private var connectedPort: Int = 5555

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
                connectedHost = host
                connectedPort = port
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

        // Make sure a cancelled/closed collector actually tears down the underlying shell
        // session and reader thread instead of leaking them — previously this was a no-op.
        awaitClose {
            try {
                session.close()
            } catch (e: Exception) {
                // Ignore — closing an already-finished session is fine.
            }
            readerThread.interrupt()
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

    /**
     * Restarts adbd as root on the device (equivalent to `adb root`), using dadb's own root
     * primitive rather than a plain shell command — there is no shell binary called "root" to
     * invoke.
     *
     * Critically: requesting the restart drops the current TCP connection out from under us
     * (adbd tears the socket down as it relaunches with different privileges), so the existing
     * `dadbInstance` is dead the instant `dadb.root()` returns — reusing it (e.g. for an
     * immediate `whoami` check) fails with "Broken pipe". A fresh connection has to be
     * established once the daemon comes back up, mirroring the retry/reconnect loop the iOS
     * client uses around its own `root:` service call.
     */
    suspend fun restartAsRoot(
        reconnectAttempts: Int = 6,
        reconnectDelayMs: Long = 1500
    ): AdbResult<String> = withContext(Dispatchers.IO) {
        val dadb = dadbInstance ?: return@withContext AdbResult.Failure("ADB not connected")
        val host = connectedHost
        val port = connectedPort
        if (host == null) return@withContext AdbResult.Failure("No known host to reconnect to after root restart")

        Log.d(TAG, "Requesting adbd restart as root...")
        try {
            dadb.root()
        } catch (e: Exception) {
            // Some dadb versions throw here because the socket closes mid-call — that's the
            // expected shape of this operation, not necessarily a real failure, so we don't
            // bail out yet and instead try to reconnect below.
            Log.d(TAG, "dadb.root() threw during restart (often expected mid-restart): ${e.message}")
        }

        // The old connection is dead either way — drop it and reconnect rather than reusing it.
        try { dadb.close() } catch (e: Exception) { /* ignore */ }
        dadbInstance = null

        var lastFailureMessage: String? = null
        repeat(reconnectAttempts) { attempt ->
            if (dadbInstance != null) return@repeat // already succeeded on a prior iteration
            delay(reconnectDelayMs)
            Log.d(TAG, "Reconnect attempt ${attempt + 1}/$reconnectAttempts after root restart...")
            when (val reconnectRes = connect(host, port)) {
                is AdbResult.Success -> {
                    val whoamiRes = runShell("whoami || id -u")
                    val isRoot = whoamiRes is AdbResult.Success &&
                            (whoamiRes.data.contains("root") || whoamiRes.data.trim() == "0")
                    if (!isRoot) {
                        lastFailureMessage = "reconnected but shell still reports non-root " +
                                "(${(whoamiRes as? AdbResult.Success)?.data?.trim() ?: (whoamiRes as? AdbResult.Failure)?.message})"
                        // Undo the connection so the next loop iteration reconnects cleanly.
                        disconnect()
                    }
                }
                is AdbResult.Failure -> {
                    lastFailureMessage = reconnectRes.message
                }
            }
        }

        if (dadbInstance != null && isConnected) {
            AdbResult.Success("adbd is running as root")
        } else {
            AdbResult.Failure(
                "Root escalation failed: could not confirm root after $reconnectAttempts " +
                        "reconnect attempts" + (lastFailureMessage?.let { " ($it)" } ?: "")
            )
        }
    }

    suspend fun reboot(): AdbResult<String> = runShell("reboot")

    /**
     * Pushes a local file to the device via dadb's sync protocol. Unlike the previous
     * implementation, this now returns Failure whenever the transfer did not actually
     * complete — there is no shell `cp`/`cat` fallback, because localFile.absolutePath is a
     * path on *this* app's device, not on the remote ADB target; a shell command referencing
     * it can never succeed and was previously masking real transfer failures as Success.
     */
    suspend fun pushFile(
        localFile: File,
        remotePath: String,
        onProgress: ((sent: Long, total: Long) -> Unit)? = null
    ): AdbResult<Boolean> = withContext(Dispatchers.IO) {
        val dadb = dadbInstance ?: return@withContext AdbResult.Failure("ADB not connected")
        val totalBytes = localFile.length()
        onProgress?.invoke(0L, totalBytes)

        try {
            Log.d(TAG, "Pushing ${localFile.absolutePath} -> $remotePath ($totalBytes bytes)")
            dadb.push(localFile, remotePath)

            // Verify the remote file actually landed at the expected size instead of trusting
            // a non-throwing push() call alone.
            val verifyRes = runShell("stat -c %s '$remotePath' 2>/dev/null || wc -c < '$remotePath'")
            val remoteSize = (verifyRes as? AdbResult.Success)?.data?.trim()?.toLongOrNull()
            if (remoteSize != null && remoteSize != totalBytes) {
                return@withContext AdbResult.Failure(
                    "Push verification failed: remote size $remoteSize != local size $totalBytes"
                )
            }

            onProgress?.invoke(totalBytes, totalBytes)
            AdbResult.Success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Push error: ${e.message}", e)
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