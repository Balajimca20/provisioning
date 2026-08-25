package com.royalenfield.ffmechanic.app.feature.supplierfeed.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.royalenfield.ffmechanic.app.feature.supplierfeed.data.SupplierFeedRepository
import com.royalenfield.ffmechanic.app.feature.supplierfeed.domain.DeviceProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class BannerState {
    data object Idle : BannerState()
    data class Loaded(val iccid: String, val model: String, val status: String) : BannerState()
    data object NotFound : BannerState()
}

data class SupplierFeedUiState(
    val iccidInput: String = "",
    val isLoading: Boolean = false,
    val device: DeviceProfile? = null,
    val banner: BannerState = BannerState.Idle,
    val logLines: List<Pair<String, String>> = emptyList(), // (message, color) like _log()
)

@HiltViewModel
class SupplierFeedViewModel @Inject constructor(
    private val repository: SupplierFeedRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SupplierFeedUiState())
    val uiState: StateFlow<SupplierFeedUiState> = _uiState.asStateFlow()

    fun onIccidChanged(value: String) {
        _uiState.update { it.copy(iccidInput = value) }
    }

    /** Port of _fetch_device() / _handle_fetch_result() / _handle_fetch_error(). */
    fun fetchDevice() {
        val iccid = _uiState.value.iccidInput.trim()
        if (iccid.isEmpty()) {
            log("Please enter an ICCID to look up.", "red")
            return
        }

        log("Fetching full parameter profile for ICCID: $iccid…")
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            try {
                val device = repository.getDevice(iccid)
                if (device == null) {
                    _uiState.update { it.copy(isLoading = false, banner = BannerState.NotFound, device = null) }
                    log("Device not found.", "orange")
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            device = device,
                            banner = BannerState.Loaded(
                                iccid = device.iccid ?: "N/A",
                                model = device.model ?: "N/A",
                                status = device.status ?: "UNKNOWN",
                            ),
                        )
                    }
                    log("Device data for ICCID ${device.iccid} successfully loaded.", "#4CAF50")
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
                log("Error fetching device: ${e.message}", "red")
            }
        }
    }

    /** Port of _clear_detail_form(). */
    fun clearForm() {
        _uiState.value = SupplierFeedUiState()
        log("Form cleared. Ready for new lookup.")
    }

    private fun log(message: String, color: String = "#00E5FF") {
        _uiState.update { it.copy(logLines = it.logLines + (message to color)) }
    }
}
