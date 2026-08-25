package com.royalenfield.provisioning.feature.supplierfeed.presentation

import com.royalenfield.provisioning.feature.supplierfeed.data.DeviceTelemetryData

data class SupplierFeedUiState(
    val serialNumberInput: String = "RE-HIM-2026-9042",
    val isLoading: Boolean = false,
    val telemetry: DeviceTelemetryData? = null,
    val errorMessage: String? = null,
    val rawGraphQLQuery: String = ""
)
