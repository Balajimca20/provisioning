package com.royalenfield.provisioning.feature.wifitracker.presentation

import com.royalenfield.provisioning.feature.wifitracker.data.WifiLogRecord

data class ConsoleLogItem(
    val timestamp: String,
    val message: String,
    val colorHex: String = "#22C55E" // default green
)

data class WifiTrackerUiState(
    // VIN Scanner & Input
    val vinInput: String = "",
    val vinError: String? = null,

    // Password Generation
    val generatedPassword: String = "",
    val isPasswordGenerated: Boolean = false,

    // Target Wi-Fi & MAC Details
    val targetSsid: String = "RE_LXHD_250925",
    val targetMacId: String = "02:00:00:44:55:66",

    // Execution & Progress State
    val isChangingPassword: Boolean = false,
    val progressPercent: Int = 0,
    val currentStep: String? = null,
    val updateSuccess: Boolean = false,
    val errorMessage: String? = null,

    // Execution Console Logs
    val consoleLogs: List<ConsoleLogItem> = listOf(
        ConsoleLogItem("11:37:15", "Application initialized. Ready for VIN entry.", "#808080")
    ),

    // Dialogs
    val showSuccessDialog: Boolean = false,
    val showErrorDialog: Boolean = false,
    val dialogMessage: String = "",

    // Transaction Log Bottom Sheet
    val isLogBottomSheetOpen: Boolean = false,
    val logSearchQuery: String = "",
    val transactionLogs: List<WifiLogRecord> = emptyList(),

    // Legacy fields for backward compatibility
    val currentSsid: String = "RE_LXHD_250925",
    val currentSecurity: String = "WPA2-PSK",
    val currentBand: String = "5 GHz (Channel 36)",
    val newSsidInput: String = "RE_LXHD_250925",
    val newPassphraseInput: String = "",
    val validationError: String? = null,
    val isUpdating: Boolean = false,
    val rawXmlContent: String = "",
    val isReadingXml: Boolean = false,
    val auditLogs: List<WifiLogRecord> = emptyList(),
    val executionLogs: List<String> = listOf(
        "[11:37:15] Application initialized. Ready for VIN entry."
    )
)
