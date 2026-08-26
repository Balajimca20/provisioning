package com.royalenfield.provisioning.feature.provisioning.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse

// ProvisioningRepository.kt
class ProvisioningRepository(private val httpClient: HttpClient) {

    suspend fun registerVehicleMetadata(
        vin: String,
        modelCode: String,
        modelDesc: String,
        region: String,
        country: String
    ): Result<Boolean> = runCatching {
        val ts = (System.currentTimeMillis() / 1000).toString()
        val payload = """
            [{
                "vin": "$vin",
                "mcu": "MCU$ts",
                "bin": "BIN$ts",
                "vcu": "IDVINREGBYTOOL",
                "model": "FLYING FLEA C6",
                "region": "$region",
                "country": "$country",
                "modelDescription": "$modelDesc",
                "plant": "Bangalore Plant 3",
                "assemblyLine": "Line A",
                "shift": "Morning",
                "modelCode": "$modelCode",
                "manufacturingDate": "2026-03-23T12:20:00:2345"
            }]
        """.trimIndent()

        val response: HttpResponse = httpClient.post("http://cbp-global.royalenfield.com/device-registration-service/v1/vehicle-metadata") {
            header("Content-Type", "application/json")
            header("api-key", "IcnygozYsM9SzImCmIn-zpIgvQRu6c3UGcb8Xcp4-hY")
            header("x-requestor", "EV")
            setBody(payload)
        }

        response.status.value in 200..202
    }
}