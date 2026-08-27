plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kotlinSerialization)
}

android {
    namespace = "com.royalenfield.provisioning"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.royalenfield.provisioning"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    // --- Environment flavors (DEV, UAT, PROD) with unique application IDs, manifest placeholders & config ---
    flavorDimensions += "env"
    productFlavors {
        create("dev") {
            dimension = "env"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            manifestPlaceholders["appName"] = "FF Provisioning (DEV)"
            manifestPlaceholders["envName"] = "DEV"
            manifestPlaceholders["usesCleartextTraffic"] = "true"

            buildConfigField("String", "BUILD_VARIANT", "\"dev\"")
            buildConfigField("String", "ENVIRONMENT_NAME", "\"DEV\"")
            buildConfigField("String", "FF_BASE_URL", "\"${prop("URL_FF_DEV")}\"")
            buildConfigField("String", "PROVISION_BASE_URL", "\"${prop("URL_PROVISION_DEV")}\"")
            buildConfigField("String", "SUPPLIER_FEED_API_KEY", "\"${prop("API_KEY_DEV")}\"")
            buildConfigField("String", "OTA_API_KEY", "\"${prop("API_KEY_OTA_DEV")}\"")
            buildConfigField("boolean", "IS_DEV", "true")
            buildConfigField("boolean", "IS_UAT", "false")
            buildConfigField("boolean", "IS_PROD", "false")
            buildConfigField("boolean", "ENABLE_MOCK_FALLBACK", "false")
            buildConfigField("boolean", "ENABLE_DEBUG_LOGGING", "true")
        }
        create("uat") {
            dimension = "env"
            applicationIdSuffix = ".uat"
            versionNameSuffix = "-uat"
            manifestPlaceholders["appName"] = "FF Provisioning (UAT)"
            manifestPlaceholders["envName"] = "UAT"
            manifestPlaceholders["usesCleartextTraffic"] = "true"

            buildConfigField("String", "BUILD_VARIANT", "\"uat\"")
            buildConfigField("String", "ENVIRONMENT_NAME", "\"UAT\"")
            buildConfigField("String", "FF_BASE_URL", "\"${prop("URL_FF_UAT")}\"")
            buildConfigField("String", "PROVISION_BASE_URL", "\"${prop("URL_PROVISION_UAT")}\"")
            buildConfigField("String", "SUPPLIER_FEED_API_KEY", "\"${prop("API_KEY_UAT")}\"")
            buildConfigField("String", "OTA_API_KEY", "\"${prop("API_KEY_OTA_UAT")}\"")
            buildConfigField("boolean", "IS_DEV", "false")
            buildConfigField("boolean", "IS_UAT", "true")
            buildConfigField("boolean", "IS_PROD", "false")
            buildConfigField("boolean", "ENABLE_MOCK_FALLBACK", "false")
            buildConfigField("boolean", "ENABLE_DEBUG_LOGGING", "true")
        }
        create("prod") {
            dimension = "env"
            isDefault = true
            manifestPlaceholders["appName"] = "FF Provisioning"
            manifestPlaceholders["envName"] = "PROD"
            manifestPlaceholders["usesCleartextTraffic"] = "false"

            buildConfigField("String", "BUILD_VARIANT", "\"prod\"")
            buildConfigField("String", "ENVIRONMENT_NAME", "\"PROD\"")
            buildConfigField("String", "FF_BASE_URL", "\"${prop("URL_FF_PROD")}\"")
            buildConfigField("String", "PROVISION_BASE_URL", "\"${prop("URL_PROVISION_PROD")}\"")
            buildConfigField("String", "SUPPLIER_FEED_API_KEY", "\"${prop("API_KEY_PROD")}\"")
            buildConfigField("String", "OTA_API_KEY", "\"${prop("API_KEY_OTA_DEFAULT")}\"")
            buildConfigField("boolean", "IS_DEV", "false")
            buildConfigField("boolean", "IS_UAT", "false")
            buildConfigField("boolean", "IS_PROD", "true")
            buildConfigField("boolean", "ENABLE_MOCK_FALLBACK", "false")
            buildConfigField("boolean", "ENABLE_DEBUG_LOGGING", "false")
        }
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
            isDebuggable = true
            isMinifyEnabled = false
        }
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }

    packaging {
        resources {
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/io.netty.versions.properties"
            excludes += "META-INF/LICENSE.md"
            excludes += "META-INF/LICENSE-notice.md"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

// Reads a value straight out of gradle.properties
fun prop(name: String): String {
    val value = project.findProperty(name) as String?
    if (value.isNullOrBlank()) {
        logger.warn("gradle.properties is missing '$name' — BuildConfig field will be empty.")
    }
    return value ?: ""
}

dependencies {
    // Jetpack Compose & AndroidX
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.google.material)

    // Koin Dependency Injection (replacing Hilt)
    val koinVersion = "3.5.6"
    implementation("io.insert-koin:koin-core:$koinVersion")
    implementation("io.insert-koin:koin-android:$koinVersion")
    implementation("io.insert-koin:koin-androidx-compose:$koinVersion")

    // Ktor HTTP Client (replacing raw OkHttp)
    val ktorVersion = "2.3.12"
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-okhttp:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")
    implementation("io.ktor:ktor-client-auth:$ktorVersion")

    // Kotlinx Serialization & Coroutines
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    // Network ADB client
    implementation("dev.mobile:dadb:1.2.6")
}
