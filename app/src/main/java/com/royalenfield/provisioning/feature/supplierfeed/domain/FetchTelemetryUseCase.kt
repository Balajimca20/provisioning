package com.royalenfield.provisioning.feature.supplierfeed.domain

import com.royalenfield.provisioning.feature.supplierfeed.data.DeviceTelemetryData
import com.royalenfield.provisioning.feature.supplierfeed.data.SupplierFeedRepository

class FetchTelemetryUseCase(
    private val supplierFeedRepository: SupplierFeedRepository
) {
//    suspend operator fun invoke(serialNumber: String): Result<DeviceTelemetryData> {
//        return supplierFeedRepository.fetchDeviceTelemetry(serialNumber)
//    }
}
