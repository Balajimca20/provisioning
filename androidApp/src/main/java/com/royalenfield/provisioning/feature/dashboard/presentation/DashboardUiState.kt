package com.royalenfield.provisioning.feature.dashboard.presentation

data class DashboardUiState(
    val isSetupStarted: Boolean = false,
    
    // Step 1: Wi-Fi Connection
    val ssidInput: String = "RE_LXHD_250925",
    val passwordInput: String = "RoyalEnfield@2025",
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

    // Vehicle Quick Telemetry
    val batteryVoltage: String = "12.8V",
    val ecuStatus: String = "LINK ACTIVE",
    val clusterBuild: String = "RE_HIM450_V2.1.0_REL",
    val storageSpace: String = "14.2 GB Free"
)
