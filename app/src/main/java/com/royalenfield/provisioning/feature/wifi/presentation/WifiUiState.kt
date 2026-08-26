package com.royalenfield.provisioning.feature.wifi.presentation

import com.royalenfield.provisioning.feature.wifi.data.WifiLogRecord

data class WifiUiState(
    val currentSsid: String = "RE_LXHD_250925",
    val currentSecurity: String = "WPA2-PSK",
    val currentBand: String = "5 GHz (Channel 36)",
    val newSsidInput: String = "RE_HIM4_992102",
    val newPassphraseInput: String = "RoyalEnfield@2025",
    val validationError: String? = null,
    val isUpdating: Boolean = false,
    val currentStep: String? = null,
    val progressPercent: Int = 0,
    val updateSuccess: Boolean = false,
    val errorMessage: String? = null,
    val rawXmlContent: String = "",
    val isReadingXml: Boolean = false,
    val auditLogs: List<WifiLogRecord> = emptyList()
)
