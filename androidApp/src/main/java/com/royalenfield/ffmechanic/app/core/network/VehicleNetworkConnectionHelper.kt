package com.royalenfield.ffmechanic.app.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages non-internet Wi-Fi connections for vehicle access points.
 *
 * Key responsibilities:
 * 1. Establish Wi-Fi connection without requiring internet (NET_CAPABILITY_INTERNET removed)
 * 2. Bind app process to the specified network so all socket operations route through it
 * 3. Handle network callbacks and state transitions
 * 4. Provide graceful disconnect and cleanup
 *
 * Usage:
 *   val helper = vehicleNetworkHelper
 *   val connected = helper.connect("RE_LXHD_250925", "password")
 *   if (connected) {
 *       // All ADB/socket operations now use 192.168.1.1 via this network
 *   }
 *   helper.disconnect()
 */
@Singleton
class VehicleNetworkConnectionHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var boundNetwork: Network? = null

    /**
     * Attempts to connect to the specified Wi-Fi network.
     *
     * @param ssid Wi-Fi network name (must start with RE_ and be 14 chars)
     * @param password Wi-Fi network password
     * @return true if connection initiated successfully (actual connection is async)
     */
    fun connect(ssid: String, password: String): Boolean {
        return try {
            Log.d(TAG, "Initiating connection to SSID=$ssid")
            disconnect() // Clean up any existing connection first

            val specifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                WifiNetworkSpecifier.Builder()
                    .setSsid(ssid)
                    .setWpa2Passphrase(password)
                    .build()
            } else {
                Log.w(TAG, "Android version < Q, WifiNetworkSpecifier not available")
                return false
            }

            // Build network request WITHOUT NET_CAPABILITY_INTERNET
            // This prevents Android from disconnecting when no internet is available
            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .setNetworkSpecifier(specifier)
                .build()

            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    Log.d(TAG, "Network available: $network")
                    _connectionState.value = ConnectionState.Available(network)
                    bindProcessToNetwork(network)
                }

                override fun onCapabilitiesChanged(
                    network: Network,
                    capabilities: NetworkCapabilities,
                ) {
                    super.onCapabilitiesChanged(network, capabilities)
                    Log.d(TAG, "Capabilities changed for $network: $capabilities")
                }

                override fun onLost(network: Network) {
                    super.onLost(network)
                    Log.d(TAG, "Network lost: $network")
                    if (boundNetwork == network) {
                        connectivityManager.bindProcessToNetwork(null)
                        boundNetwork = null
                    }
                    _connectionState.value = ConnectionState.Disconnected
                }

                override fun onUnavailable() {
                    super.onUnavailable()
                    Log.e(TAG, "Network unavailable")
                    _connectionState.value = ConnectionState.Error("Network unavailable")
                }
            }

            // Request the network
            networkCallback?.let {
                connectivityManager.requestNetwork(request, it)
                _connectionState.value = ConnectionState.Connecting
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initiate network connection", e)
            _connectionState.value = ConnectionState.Error(e.message ?: "Unknown error")
            false
        }
    }

    /**
     * Binds the application process to the specified network.
     * All socket operations will now route through this network exclusively.
     *
     * @param network The network to bind to
     */
    private fun bindProcessToNetwork(network: Network) {
        try {
            connectivityManager.bindProcessToNetwork(network)
            boundNetwork = network
            Log.d(TAG, "Process bound to network: $network")
            _connectionState.value = ConnectionState.Connected(network)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to bind process to network", e)
            _connectionState.value = ConnectionState.Error("Failed to bind process: ${e.message}")
        }
    }

    /**
     * Disconnects from the current network and unbinds the process.
     */
    fun disconnect() {
        try {
            // Unbind process from network
            if (boundNetwork != null) {
                connectivityManager.bindProcessToNetwork(null)
                boundNetwork = null
                Log.d(TAG, "Process unbound from network")
            }

            // Unregister callback
            networkCallback?.let {
                connectivityManager.unregisterNetworkCallback(it)
                networkCallback = null
                Log.d(TAG, "Network callback unregistered")
            }

            _connectionState.value = ConnectionState.Disconnected
            Log.d(TAG, "Disconnected from Wi-Fi network")
        } catch (e: Exception) {
            Log.e(TAG, "Error during disconnect", e)
        }
    }

    /**
     * Returns true if currently bound to a network.
     */
    fun isConnected(): Boolean {
        return boundNetwork != null && _connectionState.value is ConnectionState.Connected
    }

    companion object {
        private const val TAG = "VehicleNetworkHelper"
    }
}

/**
 * Represents the state of the vehicle network connection.
 */
sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    data class Available(val network: Network) : ConnectionState()
    data class Connected(val network: Network) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

