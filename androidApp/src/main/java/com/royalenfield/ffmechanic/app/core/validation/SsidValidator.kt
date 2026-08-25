package com.royalenfield.ffmechanic.app.core.validation

/**
 * Validates target Wi-Fi SSID patterns for vehicle access points.
 *
 * Rules:
 * - Must start with "RE_" (case-insensitive)
 * - Must have exactly 14 characters total (e.g., "RE_XXXX_XXXXXX")
 * - Leading/trailing quotes are automatically stripped before validation
 *
 * Examples:
 * - Valid:   "RE_LXHD_250925" (14 chars)
 * - Invalid: "RE_SHORT_1"     (11 chars)
 * - Invalid: "OTHER_SSID"     (doesn't start with RE_)
 */
object SsidValidator {

    private const val EXPECTED_LENGTH = 14
    private const val VEHICLE_SSID_PREFIX = "RE_"

    /**
     * Checks if the provided SSID matches the vehicle access point pattern.
     *
     * @param ssid Raw SSID string (may include surrounding quotes)
     * @return true if SSID is valid vehicle AP format, false otherwise
     */
    fun isValidVehicleSsid(ssid: String?): Boolean {
        if (ssid.isNullOrBlank()) return false

        // Strip surrounding quotes (common from Wi-Fi system APIs)
        val stripped = ssid.trim().trim('"')

        // Check length and prefix
        return stripped.length == EXPECTED_LENGTH &&
                stripped.startsWith(VEHICLE_SSID_PREFIX, ignoreCase = true)
    }

    /**
     * Sanitizes SSID by removing surrounding quotes and whitespace.
     *
     * @param ssid Raw SSID string
     * @return Cleaned SSID
     */
    fun sanitize(ssid: String?): String {
        return ssid?.trim()?.trim('"')?.trim() ?: ""
    }

    /**
     * Returns a user-friendly validation error message.
     *
     * @param ssid Raw SSID string
     * @return Error message if validation fails, null if valid
     */
    fun getValidationError(ssid: String?): String? {
        val cleaned = sanitize(ssid)
        return when {
            cleaned.isBlank() -> "SSID cannot be empty"
            !cleaned.startsWith(VEHICLE_SSID_PREFIX, ignoreCase = true) ->
                "SSID must start with \"$VEHICLE_SSID_PREFIX\""
            cleaned.length != EXPECTED_LENGTH ->
                "SSID must be exactly $EXPECTED_LENGTH characters (e.g., RE_LXHD_250925)"
            else -> null
        }
    }
}

