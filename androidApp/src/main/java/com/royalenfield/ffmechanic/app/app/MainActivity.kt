package com.royalenfield.ffmechanic.app.app

import android.app.Application
import android.content.Context
import android.net.wifi.SupplicantState
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.royalenfield.ffmechanic.app.app.navigation.AppNavigation
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@HiltAndroidApp
class FfMechanicApplication : Application(), DefaultLifecycleObserver {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var pendingDisconnectJob: Job? = null

    override fun onCreate() {
        super<Application>.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        Log.i("FfMechanicApplication", "Process observer registered")
    }

    override fun onStart(owner: LifecycleOwner) {
        Log.i("FfMechanicApplication", "onStart: cancel pending disconnect")
        pendingDisconnectJob?.cancel()
    }

    override fun onStop(owner: LifecycleOwner) {
        // Delay a little so quick app-switching does not instantly drop Wi-Fi.
        Log.i("FfMechanicApplication", "onStop: scheduling disconnect in 3s")
        pendingDisconnectJob?.cancel()
        pendingDisconnectJob = appScope.launch {
            delay(3_000L)
            Log.i("FfMechanicApplication", "onStop delayed job: invoking disconnectVehicleWifiIfNeeded")
            disconnectVehicleWifiIfNeeded(applicationContext)
        }
    }
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("FfMechanicApplication", "MainActivity onCreate")
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation()
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Keep a direct best-effort fallback in Activity lifecycle as well.
        Log.i("FfMechanicApplication", "MainActivity onStop: invoking disconnectVehicleWifiIfNeeded")
        disconnectVehicleWifiIfNeeded(applicationContext)
    }

    override fun onDestroy() {
        // If finishing (not rotation/config-change), make one final best-effort disconnect.
        if (isFinishing && !isChangingConfigurations) {
            Log.i("FfMechanicApplication", "MainActivity onDestroy finishing: invoking disconnectVehicleWifiIfNeeded")
            disconnectVehicleWifiIfNeeded(applicationContext)
        }
        super.onDestroy()
    }
}

private fun disconnectVehicleWifiIfNeeded(context: Context) {
    Log.i("disconnectVehicleWifiIfNeeded", "Start")
    val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return
    val info = wifiManager.connectionInfo ?: return
    val appPrefs = context.getSharedPreferences("ffm_prefs", Context.MODE_PRIVATE)

    val currentSsid = info.ssid?.removePrefix("\"")?.removeSuffix("\"").orEmpty()
    val knownVehicleSession = appPrefs.getBoolean("vehicle_wifi_connected", false)
    val isVehicleWifi = currentSsid.startsWith("RE_", ignoreCase = true) ||
        (currentSsid.isBlank() || currentSsid == "<unknown ssid>") && knownVehicleSession

    // Allow more transient states to avoid missing disconnect during lifecycle transitions.
    val isWifiAssociated = info.supplicantState == SupplicantState.COMPLETED ||
        info.supplicantState == SupplicantState.FOUR_WAY_HANDSHAKE ||
        info.supplicantState == SupplicantState.GROUP_HANDSHAKE

    Log.i(
        "disconnectVehicleWifiIfNeeded",
        "SSID='$currentSsid' startsWithRE=${currentSsid.startsWith("RE_", ignoreCase = true)} knownVehicleSession=$knownVehicleSession associated=$isWifiAssociated state=${info.supplicantState}",
    )

    if (isVehicleWifi && isWifiAssociated) {
        val disconnected = wifiManager.disconnect()
        Log.i("disconnectVehicleWifiIfNeeded", "disconnect() called for RE_ network, result=$disconnected")
        appPrefs.edit()
            .putBoolean("vehicle_wifi_connected", false)
            .putString("vehicle_wifi_ssid", "")
            .apply()
    } else {
        Log.i("disconnectVehicleWifiIfNeeded", "Skipped disconnect: not vehicle wifi or not associated")
    }
}
