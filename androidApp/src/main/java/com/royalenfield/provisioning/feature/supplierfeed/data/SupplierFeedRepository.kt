package com.royalenfield.provisioning.feature.supplierfeed.data

import android.annotation.SuppressLint
import com.royalenfield.provisioning.core.network.GraphQLClient
import com.royalenfield.provisioning.core.network.GraphQLRequest
import io.ktor.client.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class DeviceTelemetryData(
    val serialNumber: String,
    val model: String,
    val vin: String,
    val ecuHardwareRev: String,
    val tcuImei: String,
    val firmwareVersion: String,
    val batteryHealth: String,
    val batteryVoltage: Double,
    val engineHours: Double,
    val odometerKm: Double,
    val absStatus: String,
    val dtcCodes: List<String>,
    val canBusHealth: String
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class GetDeviceQueryResult(
    val getDevice: DeviceTelemetryData? = null
)

class SupplierFeedRepository(
    private val graphQLClient: GraphQLClient,
    private val httpClient: HttpClient
) {
    suspend fun fetchDeviceTelemetry(serialNumber: String): Result<DeviceTelemetryData> =
        withContext(Dispatchers.IO) {
            val query = """
                query GetDeviceTelemetry(${'$'}serial: String!) {
                  getDevice(serialNumber: ${'$'}serial) {
                    serialNumber
                    model
                    vin
                    ecuHardwareRev
                    tcuImei
                    firmwareVersion
                    batteryHealth
                    batteryVoltage
                    engineHours
                    odometerKm
                    absStatus
                    dtcCodes
                    canBusHealth
                  }
                }
            """.trimIndent()

            val variables = buildJsonObject {
                put("serial", serialNumber)
            }

            val request = GraphQLRequest(query = query, variables = variables)
            val result = graphQLClient.execute<GetDeviceQueryResult>(request)

            result.fold(
                onSuccess = { response ->
                    if (response.getDevice != null) {
                        Result.success(response.getDevice)
                    } else {
                        Result.failure(Exception("No real-time diagnostic profile found for serial number: $serialNumber in supplier registry"))
                    }
                },
                onFailure = { error ->
                    Result.failure(Exception("Live GraphQL supplier gateway query failed: ${error.message}", error))
                }
            )
        }
}
