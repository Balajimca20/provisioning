package com.royalenfield.provisioning.core.validation

/**
 * Validates vehicle SoftAP SSIDs.
 * Format: RE_XXXX_XXXXXX (exactly 14 characters, uppercase alphanumeric + underscore)
 */
object SsidValidator {

    private const val PREFIX = "RE_"
    private const val EXPECTED_LENGTH = 14
    private val SSID_PATTERN = Regex("^RE_[A-Z0-9]{4}_[A-Z0-9]{6}$")

    fun sanitize(ssid: String): String {
        return ssid.trim().removeSurrounding("\"")
    }

    fun isValidVehicleSsid(rawSsid: String): Boolean {
        val sanitized = sanitize(rawSsid)
        return SSID_PATTERN.matches(sanitized)
    }

    fun getValidationError(rawSsid: String): String? {
        val sanitized = sanitize(rawSsid)
        if (sanitized.isEmpty()) {
            return "SSID cannot be empty"
        }
        if (!sanitized.startsWith(PREFIX)) {
            return "SSID must start with '$PREFIX'"
        }
        if (sanitized.length != EXPECTED_LENGTH) {
            return "SSID must be exactly $EXPECTED_LENGTH characters (currently ${sanitized.length})"
        }
        if (!SSID_PATTERN.matches(sanitized)) {
            return "Invalid format. Expected RE_XXXX_XXXXXX (e.g. RE_LXHD_250925)"
        }
        return null
    }
}
