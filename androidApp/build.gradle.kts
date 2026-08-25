plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.hiltAndroid)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.royalenfield.ffmechanic.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.royalenfield.ffmechanic.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    // --- Environment flavors, reading the DEV/UAT/PROD values already in gradle.properties ---
    // (URL_FF_*, URL_PROVISION_*, API_KEY_*). Same pattern your existing CBP/FF/GraphQL config
    // uses — this just extends it to the ported Supplier Feed + OTA config.
    flavorDimensions += "env"
    productFlavors {
        create("dev") {
            dimension = "env"
            buildConfigField("String", "FF_BASE_URL", "\"${prop("URL_FF_DEV")}\"")
            buildConfigField("String", "PROVISION_BASE_URL", "\"${prop("URL_PROVISION_DEV")}\"")
            buildConfigField("String", "SUPPLIER_FEED_API_KEY", "\"${prop("API_KEY_DEV")}\"")
            buildConfigField("String", "OTA_API_KEY", "\"${prop("API_KEY_OTA_DEV")}\"")
        }
        create("uat") {
            dimension = "env"
            // gradle.properties has no distinct UAT value for FF/PROVISION-adjacent OTA/API keys
            // beyond what's listed — falls back to the *_UAT keys that do exist, flagged where not.
            buildConfigField("String", "FF_BASE_URL", "\"${prop("URL_FF_UAT")}\"")
            buildConfigField("String", "PROVISION_BASE_URL", "\"${prop("URL_PROVISION_UAT")}\"")
            buildConfigField("String", "SUPPLIER_FEED_API_KEY", "\"${prop("API_KEY_UAT")}\"")
            // No API_KEY_OTA_UAT in gradle.properties — falls back to the "default" OTA key.
            // Confirm this is correct; it's a guess to keep the build from failing outright.
            buildConfigField("String", "OTA_API_KEY", "\"${prop("API_KEY_OTA_DEFAULT")}\"")
        }
        create("prod") {
            dimension = "env"
            isDefault = true
            buildConfigField("String", "FF_BASE_URL", "\"${prop("URL_FF_PROD")}\"")
            buildConfigField("String", "PROVISION_BASE_URL", "\"${prop("URL_PROVISION_PROD")}\"")
            buildConfigField("String", "SUPPLIER_FEED_API_KEY", "\"${prop("API_KEY_PROD")}\"")
            buildConfigField("String", "OTA_API_KEY", "\"${prop("API_KEY_OTA_DEFAULT")}\"")
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
            // dadb has fewer transitive dependencies than adam, fewer duplicate META-INF files
            // but keep these excludes if they still appear
            excludes += "META-INF/INDEX.LIST"
            // Netty jars also duplicate this metadata file across modules.
            excludes += "META-INF/io.netty.versions.properties"
            // Duplicate license files from transitive dependencies can break resource merge.
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

// Reads a value straight out of gradle.properties (Gradle exposes it as a project property).
// Falls back to an empty string + a build-time warning rather than failing outright, since
// some *_UAT-shaped keys referenced above don't actually exist yet in your gradle.properties.
fun prop(name: String): String {
    val value = project.findProperty(name) as String?
    if (value.isNullOrBlank()) {
        logger.warn("gradle.properties is missing '$name' — BuildConfig field will be empty.")
    }
    return value ?: ""
}

dependencies {
    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.google.material)

    // DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)

    // Networking (GraphQL over OkHttp, mirrors graphql_client.py)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)

    // Network ADB client
    implementation("dev.mobile:dadb:1.2.6")

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
}
