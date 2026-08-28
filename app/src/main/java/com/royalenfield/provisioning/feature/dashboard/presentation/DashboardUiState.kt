package com.royalenfield.provisioning.feature.dashboard.presentation

data class DashboardUiState(
    val isSetupStarted: Boolean = false,
    
    // Step 1: Wi-Fi Connection
    val ssidInput: String = "RE_3EJQ_250925",
    val passwordInput: String = "12345678",
    val ssidValidationError: String? = null,
    val isWifiConnecting: Boolean = false,
    val isWifiConnected: Boolean = false,
    val connectedSsid: String? = null,
    val isProcessBound: Boolean = false,
    val wifiErrorMessage: String? = null,

    // Step 2: ADB Connection
    val adbHostInput: String = "192.168.1.1",
    val adbPortInput: String = "5555",
    val isAdbConnecting: Boolean = false,
    val isAdbConnected: Boolean = false,
    val isAdbRooted: Boolean = false,
    val adbErrorMessage: String? = null,

    // Vehicle Quick Telemetry (Real-time telemetry updated via connection/daemon)
    val batteryVoltage: String = "--",
    val ecuStatus: String = "DISCONNECTED",
    val clusterBuild: String = "--",
    val storageSpace: String = "--"
)
