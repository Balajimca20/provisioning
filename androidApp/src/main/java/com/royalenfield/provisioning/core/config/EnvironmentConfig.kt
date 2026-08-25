package com.royalenfield.provisioning.core.config

import androidx.compose.ui.graphics.Color
import com.royalenfield.provisioning.BuildConfig

enum class AppEnvironment(
    val id: String,
    val title: String,
    val badgeText: String,
    val badgeColor: Color,
    val badgeBackground: Color
) {
    DEV(
        id = "dev",
        title = "Development",
        badgeText = "DEV",
        badgeColor = Color(0xFF06B6D4), // Cyan Accent
        badgeBackground = Color(0xFF083344)
    ),
    UAT(
        id = "uat",
        title = "User Acceptance Testing",
        badgeText = "UAT",
        badgeColor = Color(0xFFF59E0B), // Amber Accent
        badgeBackground = Color(0xFF451A03)
    ),
    PROD(
        id = "prod",
        title = "Production",
        badgeText = "PROD",
        badgeColor = Color(0xFF10B981), // Emerald Accent
        badgeBackground = Color(0xFF064E3B)
    );

    companion object {
        fun fromVariant(variant: String): AppEnvironment {
            return when (variant.lowercase()) {
                "dev" -> DEV
                "uat" -> UAT
                "prod" -> PROD
                else -> PROD
            }
        }
    }
}

object EnvironmentConfig {
    val current: AppEnvironment = AppEnvironment.fromVariant(BuildConfig.ENVIRONMENT_NAME)

    val environmentName: String = BuildConfig.ENVIRONMENT_NAME
    val buildVariant: String = BuildConfig.BUILD_VARIANT
    val ffBaseUrl: String = BuildConfig.FF_BASE_URL
    val provisionBaseUrl: String = BuildConfig.PROVISION_BASE_URL
    val supplierFeedApiKey: String = BuildConfig.SUPPLIER_FEED_API_KEY
    val otaApiKey: String = BuildConfig.OTA_API_KEY

    val isDev: Boolean = BuildConfig.IS_DEV
    val isUat: Boolean = BuildConfig.IS_UAT
    val isProd: Boolean = BuildConfig.IS_PROD
    val isDebugLoggingEnabled: Boolean = BuildConfig.ENABLE_DEBUG_LOGGING
    val isMockFallbackAllowed: Boolean = BuildConfig.ENABLE_MOCK_FALLBACK

    val formattedVariantDisplay: String
        get() = "${current.badgeText} (${BuildConfig.BUILD_TYPE.uppercase()})"
}
