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

    // Target Wi-Fi & MAC Details (Live from device / ADB)
    val targetSsid: String = "",
    val targetMacId: String = "",

    // Execution & Progress State
    val isChangingPassword: Boolean = false,
    val progressPercent: Int = 0,
    val currentStep: String? = null,
    val updateSuccess: Boolean = false,
    val errorMessage: String? = null,

    // Execution Console Logs
    val consoleLogs: List<ConsoleLogItem> = emptyList(),

    // Dialogs
    val showSuccessDialog: Boolean = false,
    val showErrorDialog: Boolean = false,
    val dialogMessage: String = "",

    // Transaction Log Bottom Sheet
    val isLogBottomSheetOpen: Boolean = false,
    val logSearchQuery: String = "",
    val transactionLogs: List<WifiLogRecord> = emptyList(),

    // Legacy fields for backward compatibility
    val currentSsid: String = "",
    val currentSecurity: String = "",
    val currentBand: String = "",
    val newSsidInput: String = "",
    val newPassphraseInput: String = "",
    val validationError: String? = null,
    val isUpdating: Boolean = false,
    val rawXmlContent: String = "",
    val isReadingXml: Boolean = false,
    val auditLogs: List<WifiLogRecord> = emptyList(),
    val executionLogs: List<String> = emptyList()
)
