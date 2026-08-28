package com.royalenfield.provisioning.feature.supplierfeed.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.royalenfield.provisioning.core.adb.AdbClient
import com.royalenfield.provisioning.core.adb.AdbResult
import com.royalenfield.provisioning.feature.supplierfeed.data.SupplierFeedRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


class SupplierFeedViewModel(
    val adbClient: AdbClient,
    val supplierFeedRepository: SupplierFeedRepository
) : ViewModel() {

    private val _deviceData = MutableStateFlow(SupplierFeedUiState())
    val deviceData = _deviceData.asStateFlow()

    init {
        // Start watching for cellular network early so it's ready when fetchSupplierFeed() is called
        supplierFeedRepository.startCellularMonitor()
    }

    override fun onCleared() {
        super.onCleared()
        supplierFeedRepository.stopCellularMonitor()
    }

    fun onIccidChanged(newValue: String) {
        _deviceData.update { it.copy(iccid = newValue) }
    }

    fun fetchIccidViaDadb() {
        viewModelScope.launch {
            adbClient.restartAsRoot()
            val response = adbClient.runShell("content query --uri content://telephony/siminfo")
            val regex = Regex("""icc_id=(\d{18,22})""")

            when (response) {
                is AdbResult.Failure -> {
                    Log.e("AllDataFailure", "${response.message}")
                }
                is AdbResult.Success -> {
                    Log.e("AllData", response.data)
                    val result = regex.find(response.data)?.groupValues?.get(1)
                    Log.e("TAG", "fetchIccidViaDadb: $result")
                    _deviceData.update { it.copy(iccid = result ?: "") }
                }
            }
        }
    }

    fun fetchSupplierFeedResponse() {
        viewModelScope.launch {
            _deviceData.update { it.copy(isLoading = true, errorMessage = null) }

            val response = supplierFeedRepository.fetchSupplierFeed(_deviceData.value.iccid)

            if (response.isSuccess) {
                val data = response.getOrNull()
                _deviceData.update {
                    it.copy(
                        isLoading = false,
                        // Core Hardware
                        iccid = data?.iccid ?: "",
                        euid = data?.euid ?: "",
                        systemId = data?.systemId ?: "",
                        status = data?.status ?: "",
                        model = data?.model ?: "",
                        imeiPrimary = data?.imeiPrimary ?: "",
                        imeiSecondary = data?.imeiSecondary ?: "",
                        serialNo = data?.serialNo ?: "",
                        partNo = data?.partNo?.trim() ?: "",
                        hwVersion = data?.hwVersion ?: "",
                        vendorCode = data?.vendorCode?.toString() ?: "",  // Int? -> String
                        category = data?.category ?: "",
                        // Security Keys
                        adminKey = data?.adminKey ?: "",
                        userKey = data?.userKey ?: "",
                        // SOM Wireless
                        wifiMac24 = data?.wifiMac24 ?: "",
                        wifiSsid24 = data?.wifiSsid24 ?: "",
                        wifiPass24 = data?.wifiPass24 ?: "",
                        wifiMac5 = data?.wifiMac5?.trim() ?: "",
                        wifiSsid5 = data?.wifiSsid5 ?: "",
                        wifiPass5 = data?.wifiPass5 ?: "",
                        somBtMac = data?.somBtMac ?: "",
                        somBtName = data?.somBtName ?: "",
                        somBtPass = data?.somBtPass ?: "",
                        somBleMac = data?.somBleMac ?: "",
                        somBlePass = data?.somBlePass ?: "",
                        // BCM Wireless
                        bcm1stBleMac = data?.bcm1stBleMac ?: "",
                        bcm1stBleName = data?.bcm1stBleName ?: "",
                        bcm1stBlePass = data?.bcm1stBlePass ?: "",
                        bcm2ndBtMac = data?.bcm2ndBtMac ?: "",
                        bcm2ndBtName = data?.bcm2ndBtName ?: "",
                        bcm2ndBtPass = data?.bcm2ndBtPass ?: "",
                        // eSIM / Cellular
                        esimImsi = data?.esimImsi ?: "",
                        esimMsisdn = data?.esimMsisdn ?: "",
                        esimApn = data?.esimApn ?: "",
                        esimPartNo = data?.esimPartNo ?: "",
                        esimVendorCode = data?.esimVendorCode ?: "",
                        gsmCreg = data?.gsmCreg ?: "",
                        shipmentInvoice = data?.shipmentInvoice ?: "",
                        // Metadata
                        somMake = data?.somMake ?: "",
                        somSwVersion = data?.somSwVersion ?: "",
                        firmwareVersion = data?.firmwareVersion?.toString() ?: "",  // Int? -> String
                        configVersion = data?.configVersion ?: "",
                        manufacturingDate = data?.manufacturingDate ?: "",
                        createdBy = data?.createdBy ?: "",
                        createdTime = data?.createdTime ?: "",
                        updatedBy = data?.updatedBy ?: "",
                        updatedTime = data?.updatedTime ?: ""
                    )
                }
            } else {
                val msg = response.exceptionOrNull()?.message ?: "Unknown error"
                Log.e("SupplierFeedViewModel", "fetchSupplierFeedResponse failed: $msg")
                _deviceData.update { it.copy(isLoading = false, errorMessage = msg) }
            }
        }
    }

    fun clearForm() {
        _deviceData.update { it.copy(iccid = "") }
    }
}

data class SupplierFeedUiState(
    // UI state
    val isLoading: Boolean = false,
    val errorMessage: String? = null,

    // Core System Hardware
    val iccid: String = "",
    val euid: String = "",
    val systemId: String = "",
    val status: String = "",
    val model: String = "",
    val imeiPrimary: String = "",
    val imeiSecondary: String = "",
    val serialNo: String = "",
    val partNo: String = "",
    val hwVersion: String = "",
    val vendorCode: String = "",
    val category: String = "",

    // Security Key Vault
    val adminKey: String = "",
    val userKey: String = "",

    // SOM Wireless (Wi-Fi / Bluetooth)
    val wifiMac24: String = "",
    val wifiSsid24: String = "",
    val wifiPass24: String = "",
    val wifiMac5: String = "",
    val wifiSsid5: String = "",
    val wifiPass5: String = "",
    val somBtMac: String = "",
    val somBtName: String = "",
    val somBtPass: String = "",
    val somBleMac: String = "",
    val somBlePass: String = "",

    // BCM Wireless (Bluetooth / BLE)
    val bcm1stBleMac: String = "",
    val bcm1stBleName: String = "",
    val bcm1stBlePass: String = "",
    val bcm2ndBtMac: String = "",
    val bcm2ndBtName: String = "",
    val bcm2ndBtPass: String = "",

    // Cellular Modem / eSIM Profile
    val esimImsi: String = "",
    val esimMsisdn: String = "",
    val esimApn: String = "",
    val esimPartNo: String = "",
    val esimVendorCode: String = "",
    val gsmCreg: String = "",
    val shipmentInvoice: String = "",

    // Software, Firmware / Lifecycle Metadata
    val somMake: String = "",
    val somSwVersion: String = "",
    val firmwareVersion: String = "",
    val configVersion: String = "",
    val manufacturingDate: String = "",
    val createdBy: String = "",
    val createdTime: String = "",
    val updatedBy: String = "",
    val updatedTime: String = ""
)
