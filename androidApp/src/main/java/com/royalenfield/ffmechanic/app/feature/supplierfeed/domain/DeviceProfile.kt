package com.royalenfield.ffmechanic.app.feature.supplierfeed.domain

import kotlinx.serialization.Serializable

/**
 * Mirrors the `getDevice` GraphQL response fields from supplier_feed_tab.py's GET_DEVICE_QUERY.
 * Kept as one flat serializable model since the API returns one flat object; the UI groups
 * fields into categories via [FieldCategories] below, same grouping as FIELD_CATEGORIES.
 */
@Serializable
data class DeviceProfile(
    // Core system & hardware
    val iccid: String? = null,
    val euid: String? = null,
    val system_id: String? = null,
    val status: String? = null,
    val model: String? = null,
    val imei_primary: String? = null,
    val imei_secondary: String? = null,
    val serial_no: String? = null,
    val part_no: String? = null,
    val hw_version: String? = null,
    val vendor_code: String? = null,
    val category: String? = null,

    // Security & key vault
    val admin_key: String? = null,
    val user_key: String? = null,

    // SOM wireless (Wi-Fi & Bluetooth)
    val som_wifi_mac_2_4ghz: String? = null,
    val som_wifi_ssid_connection_2_5ghz: String? = null,
    val som_wifi_pass_phrase_2_5ghz: String? = null,
    val som_wifi_mac_5ghz: String? = null,
    val som_wifi_ssid_connection_5ghz: String? = null,
    val som_wifi_pass_phrase_5ghz: String? = null,
    val som_bt_mac_id: String? = null,
    val som_bt_connection_name: String? = null,
    val som_bt_pass_phrase: String? = null,
    val som_ble_mac_id: String? = null,
    val som_ble_pass_phrase: String? = null,

    // BCM wireless (Bluetooth & BLE)
    val bcm_1st_ble_mac_id: String? = null,
    val bcm_1st_ble_connection_name: String? = null,
    val bcm_1st_ble_pass_phrase: String? = null,
    val bcm_2nd_bt_mac_id: String? = null,
    val bcm_2nd_bt_connection_name: String? = null,
    val bcm_2nd_bt_pass_phrase: String? = null,

    // Cellular modem & eSIM profile
    val esim_imsi: String? = null,
    val esim_msisdn: String? = null,
    val esim_apn: String? = null,
    val esim_part_no: String? = null,
    val esim_vendor_code: String? = null,
    val gsm_creg: String? = null,
    val shipment_invoice: String? = null,

    // Software, firmware & lifecycle metadata
    val som_make: String? = null,
    val som_sw_version: String? = null,
    val firmware_version: String? = null,
    val config_version: String? = null,
    val manufacturing_date: String? = null,
    val created_by: String? = null,
    val created_time: String? = null,
    val updated_by: String? = null,
    val updated_time: String? = null,
) {
    /** Reflective-free field access for the generic category UI below. */
    fun value(key: String): String? = FieldCategories.getter(key)?.invoke(this)
}

/**
 * Mirrors FIELD_CATEGORIES in supplier_feed_tab.py: (field_key, display_label) grouped
 * under a section title. Used to drive the Compose grid the same way the PyQt GridLayout did.
 */
object FieldCategories {
    data class Field(val key: String, val label: String, val getter: (DeviceProfile) -> String?)

    val sections: List<Pair<String, List<Field>>> = listOf(
        "📦 Core System & Hardware" to listOf(
            Field("iccid", "ICCID") { it.iccid },
            Field("euid", "EUID") { it.euid },
            Field("system_id", "System ID") { it.system_id },
            Field("status", "Status") { it.status },
            Field("model", "Model") { it.model },
            Field("imei_primary", "IMEI Primary") { it.imei_primary },
            Field("imei_secondary", "IMEI Secondary") { it.imei_secondary },
            Field("serial_no", "Serial No") { it.serial_no },
            Field("part_no", "Part No") { it.part_no },
            Field("hw_version", "HW Version") { it.hw_version },
            Field("vendor_code", "Vendor Code") { it.vendor_code },
            Field("category", "Category") { it.category },
        ),
        "🔐 Security & Key Vault" to listOf(
            Field("admin_key", "Admin Key") { it.admin_key },
            Field("user_key", "User Key") { it.user_key },
        ),
        "📶 SOM Wireless (Wi-Fi & Bluetooth)" to listOf(
            Field("som_wifi_mac_2_4ghz", "Wi-Fi MAC (2.4GHz)") { it.som_wifi_mac_2_4ghz },
            Field("som_wifi_ssid_connection_2_5ghz", "Wi-Fi SSID (2.4GHz)") { it.som_wifi_ssid_connection_2_5ghz },
            Field("som_wifi_pass_phrase_2_5ghz", "Wi-Fi Passphrase (2.4GHz)") { it.som_wifi_pass_phrase_2_5ghz },
            Field("som_wifi_mac_5ghz", "Wi-Fi MAC (5GHz)") { it.som_wifi_mac_5ghz },
            Field("som_wifi_ssid_connection_5ghz", "Wi-Fi SSID (5GHz)") { it.som_wifi_ssid_connection_5ghz },
            Field("som_wifi_pass_phrase_5ghz", "Wi-Fi Passphrase (5GHz)") { it.som_wifi_pass_phrase_5ghz },
            Field("som_bt_mac_id", "SOM BT MAC") { it.som_bt_mac_id },
            Field("som_bt_connection_name", "SOM BT Connection Name") { it.som_bt_connection_name },
            Field("som_bt_pass_phrase", "SOM BT Passphrase") { it.som_bt_pass_phrase },
            Field("som_ble_mac_id", "SOM BLE MAC") { it.som_ble_mac_id },
            Field("som_ble_pass_phrase", "SOM BLE Passphrase") { it.som_ble_pass_phrase },
        ),
        "📡 BCM Wireless (Bluetooth & BLE)" to listOf(
            Field("bcm_1st_ble_mac_id", "BCM 1st BLE MAC") { it.bcm_1st_ble_mac_id },
            Field("bcm_1st_ble_connection_name", "BCM 1st BLE Name") { it.bcm_1st_ble_connection_name },
            Field("bcm_1st_ble_pass_phrase", "BCM 1st BLE Passphrase") { it.bcm_1st_ble_pass_phrase },
            Field("bcm_2nd_bt_mac_id", "BCM 2nd BT MAC") { it.bcm_2nd_bt_mac_id },
            Field("bcm_2nd_bt_connection_name", "BCM 2nd BT Name") { it.bcm_2nd_bt_connection_name },
            Field("bcm_2nd_bt_pass_phrase", "BCM 2nd BT Passphrase") { it.bcm_2nd_bt_pass_phrase },
        ),
        "📱 Cellular Modem & eSIM Profile" to listOf(
            Field("esim_imsi", "eSIM IMSI") { it.esim_imsi },
            Field("esim_msisdn", "eSIM MSISDN") { it.esim_msisdn },
            Field("esim_apn", "eSIM APN") { it.esim_apn },
            Field("esim_part_no", "eSIM Part No") { it.esim_part_no },
            Field("esim_vendor_code", "eSIM Vendor Code") { it.esim_vendor_code },
            Field("gsm_creg", "GSM CREG") { it.gsm_creg },
            Field("shipment_invoice", "Shipment Invoice") { it.shipment_invoice },
        ),
        "🏭 Software, Firmware & Lifecycle Metadata" to listOf(
            Field("som_make", "SOM Make") { it.som_make },
            Field("som_sw_version", "SOM SW Version") { it.som_sw_version },
            Field("firmware_version", "Firmware Version") { it.firmware_version },
            Field("config_version", "Config Version") { it.config_version },
            Field("manufacturing_date", "Manufacturing Date") { it.manufacturing_date },
            Field("created_by", "Created By") { it.created_by },
            Field("created_time", "Created Time") { it.created_time },
            Field("updated_by", "Updated By") { it.updated_by },
            Field("updated_time", "Updated Time") { it.updated_time },
        ),
    )

    fun getter(key: String): ((DeviceProfile) -> String?)? =
        sections.flatMap { it.second }.firstOrNull { it.key == key }?.getter
}
