package com.royalenfield.provisioning.feature.supplierfeed.data

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.royalenfield.provisioning.core.network.GraphQLError
import com.royalenfield.provisioning.feature.supplierfeed.presentation.GraphQLClient
import io.ktor.client.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
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
data class GraphQLResponse(
    val data: DeviceDataWrapper? = null,
    val errors: List<GraphQLError>? = null
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class DeviceDataWrapper(
    val getDevice: SupplierFeedApiResponse? = null
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class SupplierFeedApiResponse(
    @SerialName("iccid")
    val iccid: String? = null,
    @SerialName("euid")
    val euid: String? = null,
    @SerialName("system_id")
    val systemId: String? = null,
    @SerialName("status")
    val status: String? = null,
    @SerialName("model")
    val model: String? = null,
    @SerialName("imei_primary")
    val imeiPrimary: String? = null,
    @SerialName("imei_secondary")
    val imeiSecondary: String? = null,
    @SerialName("serial_no")
    val serialNo: String? = null,
    @SerialName("part_no")
    val partNo: String? = null,
    @SerialName("hw_version")
    val hwVersion: String? = null,
    @SerialName("vendor_code")
    val vendorCode: Int? = null,      // API returns Int e.g. 13346
    @SerialName("category")
    val category: String? = null,

    // Security Keys
    @SerialName("admin_key")
    val adminKey: String? = null,
    @SerialName("user_key")
    val userKey: String? = null,

    // SOM Wireless
    @SerialName("som_wifi_mac_2_4ghz")
    val wifiMac24: String? = null,
    @SerialName("som_wifi_ssid_connection_2_5ghz")
    val wifiSsid24: String? = null,
    @SerialName("som_wifi_pass_phrase_2_5ghz")
    val wifiPass24: String? = null,
    @SerialName("som_wifi_mac_5ghz")
    val wifiMac5: String? = null,
    @SerialName("som_wifi_ssid_connection_5ghz")
    val wifiSsid5: String? = null,
    @SerialName("som_wifi_pass_phrase_5ghz") val wifiPass5: String? = null,
    @SerialName("som_bt_mac_id") val somBtMac: String? = null,
    @SerialName("som_bt_connection_name") val somBtName: String? = null,
    @SerialName("som_bt_pass_phrase") val somBtPass: String? = null,
    @SerialName("som_ble_mac_id") val somBleMac: String? = null,
    @SerialName("som_ble_pass_phrase") val somBlePass: String? = null,

    // BCM Wireless
    @SerialName("bcm_1st_ble_mac_id") val bcm1stBleMac: String? = null,
    @SerialName("bcm_1st_ble_connection_name") val bcm1stBleName: String? = null,
    @SerialName("bcm_1st_ble_pass_phrase") val bcm1stBlePass: String? = null,
    @SerialName("bcm_2nd_bt_mac_id") val bcm2ndBtMac: String? = null,
    @SerialName("bcm_2nd_bt_connection_name") val bcm2ndBtName: String? = null,
    @SerialName("bcm_2nd_bt_pass_phrase") val bcm2ndBtPass: String? = null,

    // eSIM Profile
    @SerialName("esim_imsi") val esimImsi: String? = null,
    @SerialName("esim_msisdn") val esimMsisdn: String? = null,
    @SerialName("esim_apn") val esimApn: String? = null,
    @SerialName("esim_part_no") val esimPartNo: String? = null,
    @SerialName("esim_vendor_code") val esimVendorCode: String? = null,
    @SerialName("gsm_creg") val gsmCreg: String? = null,
    @SerialName("shipment_invoice") val shipmentInvoice: String? = null,

    // Metadata
    @SerialName("som_make") val somMake: String? = null,
    @SerialName("som_sw_version") val somSwVersion: String? = null,
    @SerialName("firmware_version") val firmwareVersion: Int? = null,  // API returns Int e.g. 16
    @SerialName("config_version") val configVersion: String? = null,
    @SerialName("manufacturing_date") val manufacturingDate: String? = null,
    @SerialName("created_by") val createdBy: String? = null,
    @SerialName("created_time") val createdTime: String? = null,
    @SerialName("updated_by") val updatedBy: String? = null,
    @SerialName("updated_time") val updatedTime: String? = null
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class GetDeviceQueryResult(
    val getDevice: DeviceTelemetryData? = null
)

class SupplierFeedRepository(
    private val httpClient: HttpClient,
    private val context: Context
) {
    // Holds the live cellular Network reference — populated by startCellularMonitor()
    @Volatile private var cellularNetwork: Network? = null
    private var cellularCallback: ConnectivityManager.NetworkCallback? = null

    /** Call once from ViewModel init{} to start watching for cellular availability early. */
    fun startCellularMonitor() {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .build()
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                cellularNetwork = network
                Log.d("SupplierFeedRepo", "Cellular network ready: $network")
            }
            override fun onLost(network: Network) {
                if (cellularNetwork == network) cellularNetwork = null
                Log.w("SupplierFeedRepo", "Cellular network lost")
            }
        }
        cellularCallback = cb
        cm.requestNetwork(request, cb)
    }

    /** Call from ViewModel onCleared() to release the callback. */
    fun stopCellularMonitor() {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        cellularCallback?.let {
            try { cm.unregisterNetworkCallback(it) } catch (_: Exception) {}
        }
        cellularCallback = null
        cellularNetwork = null
    }


    suspend fun fetchSupplierFeed(iccid: String): Result<SupplierFeedApiResponse> =
        withContext(Dispatchers.IO) {
            requestAndBindCellularNetwork(context)

            val getDeviceQuery = """
                query GetDevice(${'$'}iccid: String!) {
                    getDevice(iccid: ${'$'}iccid) {
                        imei_primary vendor_code category part_no hw_version serial_no
                        iccid euid system_id status admin_key user_key som_ble_mac_id
                        som_ble_pass_phrase som_wifi_mac_2_4ghz som_wifi_ssid_connection_2_5ghz
                        som_wifi_pass_phrase_2_5ghz som_wifi_mac_5ghz som_wifi_ssid_connection_5ghz
                        som_wifi_pass_phrase_5ghz bcm_1st_ble_mac_id bcm_1st_ble_connection_name
                        bcm_1st_ble_pass_phrase som_bt_mac_id som_bt_connection_name
                        som_bt_pass_phrase bcm_2nd_bt_mac_id bcm_2nd_bt_connection_name
                        bcm_2nd_bt_pass_phrase som_make som_sw_version model imei_secondary
                        gsm_creg shipment_invoice esim_part_no esim_vendor_code esim_imsi
                        esim_msisdn esim_apn created_by created_time updated_by updated_time
                        manufacturing_date firmware_version config_version
                    }
                }
            """.trimIndent()

            val variables = buildJsonObject { put("iccid", iccid) }

            try {
                // GraphQLClient.execute() returns the full JsonObject: {"data":{"getDevice":{...}}}
                val responseJson = GraphQLClient.execute(query = getDeviceQuery, variables = variables)

                // Navigate: data -> getDevice
                val deviceJson = responseJson["data"]
                    ?.jsonObject?.get("getDevice")
                    ?.jsonObject
                    ?: return@withContext Result.failure(
                        Exception("No supplier feed entry found for ICCID: $iccid")
                    )

                // Decode into SupplierFeedApiResponse using lenient Json
                // coerceInputValues handles null -> default, isLenient handles loose parsing
                val json = Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    coerceInputValues = true
                }
                val apiResponse = json.decodeFromJsonElement<SupplierFeedApiResponse>(deviceJson)

                Log.d("SupplierFeedRepo", "Supplier feed fetched successfully for ICCID: $iccid")
                Result.success(apiResponse)

            } catch (e: Exception) {
                Log.e("SupplierFeedRepo", "fetchSupplierFeed failed: ${e.message}", e)
                Result.failure(Exception("Supplier feed request failed: ${e.message}", e))
            }
        }

    fun requestAndBindCellularNetwork(context: Context) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .build()

        connectivityManager.requestNetwork(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                connectivityManager.bindProcessToNetwork(network)
                Log.d("NETWORK_FIX ::", " Cellular driver available. Process bound.")
            }

            override fun onLost(network: Network) {
                connectivityManager.bindProcessToNetwork(null)
                Log.e("NETWORK_FIX ::", " Cellular driver lost connection.")
            }
        })
    }

}
