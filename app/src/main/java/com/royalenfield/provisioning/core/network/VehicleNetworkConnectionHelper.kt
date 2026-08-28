package com.royalenfield.provisioning.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class VehicleNetworkState {
    object Disconnected : VehicleNetworkState()
    data class Connecting(val ssid: String) : VehicleNetworkState()
    data class Connected(val ssid: String, val isBound: Boolean) : VehicleNetworkState()
    data class Failed(val error: String) : VehicleNetworkState()
}

class VehicleNetworkConnectionHelper(
    private val context: Context
) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _networkState = MutableStateFlow<VehicleNetworkState>(VehicleNetworkState.Disconnected)
    val networkState: StateFlow<VehicleNetworkState> = _networkState.asStateFlow()

    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var boundNetwork: Network? = null

    fun connect(ssid: String, passphrase: String) {
        disconnect()

        _networkState.value = VehicleNetworkState.Connecting(ssid)
        Log.d(TAG, "Requesting non-internet Wi-Fi connection to $ssid")

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                Log.d(TAG, "Network available: $network, binding process...")

                val bound = connectivityManager.bindProcessToNetwork(network)
                boundNetwork = network
                _networkState.value = VehicleNetworkState.Connected(ssid = ssid, isBound = bound)
                Log.d(TAG, "Process bound to vehicle network: $bound")
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                Log.w(TAG, "Network connection lost: $network")
                if (boundNetwork == network) {
                    connectivityManager.bindProcessToNetwork(null)
                    boundNetwork = null
                    _networkState.value = VehicleNetworkState.Disconnected
                }
            }

            override fun onUnavailable() {
                super.onUnavailable()
                Log.e(TAG, "Vehicle network unavailable or user cancelled")
                _networkState.value = VehicleNetworkState.Failed("Vehicle Wi-Fi connection timed out or cancelled")
            }
        }

        networkCallback = callback

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val specifier = WifiNetworkSpecifier.Builder()
                .setSsid(ssid)
                .setWpa2Passphrase(passphrase)
                .build()

            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .setNetworkSpecifier(specifier)
                .build()

            connectivityManager.requestNetwork(request, callback)
        } else {
            // Pre-Android 10 fallback
            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build()
            connectivityManager.requestNetwork(request, callback)
        }
    }

    fun disconnect() {
        try {
            networkCallback?.let {
                connectivityManager.unregisterNetworkCallback(it)
            }
        } catch (e: IllegalArgumentException) {
            // Handle case where callback was already unregistered or never successfully registered
            Log.w(TAG, "NetworkCallback was not registered: ${e.message}")
        } catch (e: Exception) {
            Log.w(TAG, "Error unregistering network callback: ${e.message}")
        } finally {
            networkCallback = null
            connectivityManager.bindProcessToNetwork(null)
            boundNetwork = null
            _networkState.value = VehicleNetworkState.Disconnected
        }
    }

    companion object {
        private const val TAG = "VehicleNetworkHelper"
    }
}
