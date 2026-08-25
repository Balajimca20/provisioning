package com.royalenfield.ffmechanic.app.feature.supplierfeed.data

import com.royalenfield.ffmechanic.app.core.network.GraphQLClient
import com.royalenfield.ffmechanic.app.feature.supplierfeed.domain.DeviceProfile
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class GetDeviceData(val getDevice: DeviceProfile? = null)

/** Port of supplier_feed_tab.py's GET_DEVICE_QUERY. */
private const val GET_DEVICE_QUERY = """
query GetDevice(${'$'}iccid: String!) {
    getDevice(iccid: ${'$'}iccid) {
        imei_primary vendor_code category part_no hw_version serial_no iccid euid system_id status
        admin_key user_key
        som_ble_mac_id som_ble_pass_phrase
        som_wifi_mac_2_4ghz som_wifi_ssid_connection_2_5ghz som_wifi_pass_phrase_2_5ghz
        som_wifi_mac_5ghz som_wifi_ssid_connection_5ghz som_wifi_pass_phrase_5ghz
        bcm_1st_ble_mac_id bcm_1st_ble_connection_name bcm_1st_ble_pass_phrase
        som_bt_mac_id som_bt_connection_name som_bt_pass_phrase
        bcm_2nd_bt_mac_id bcm_2nd_bt_connection_name bcm_2nd_bt_pass_phrase
        som_make som_sw_version model imei_secondary gsm_creg shipment_invoice
        esim_part_no esim_vendor_code esim_imsi esim_msisdn esim_apn
        created_by created_time updated_by updated_time manufacturing_date
        firmware_version config_version
    }
}
"""

@Singleton
class SupplierFeedRepository @Inject constructor(
    private val graphQLClient: GraphQLClient,
) {
    /** Throws GraphQLException on failure — same contract as GraphQLClient.execute in Python. */
    suspend fun getDevice(iccid: String): DeviceProfile? {
        val data: GetDeviceData = graphQLClient.execute(
            query = GET_DEVICE_QUERY,
            variables = mapOf("iccid" to JsonPrimitive(iccid)),
        )
        return data.getDevice
    }
}
