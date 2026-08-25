package com.royalenfield.provisioning.feature.ota.presentation

import com.royalenfield.provisioning.feature.ota.data.OtaPackage

data class OtaUiState(
    val availablePackages: List<OtaPackage> = emptyList(),
    val selectedPackage: OtaPackage? = null,
    val currentInstalledVersion: String = "RE_HIM450_V2.1.0_REL",
    val pipelineStage: String = "IDLE", // IDLE, PRECHECK, DOWNLOADING, PUSHING, VERIFYING, FLASHING, AWAITING_REBOOT, COMPLETE, FAILED
    val progressPercent: Int = 0,
    val stageStatusText: String = "Ready to deploy firmware",
    val terminalLogs: List<String> = emptyList(),
    val errorMessage: String? = null
)
