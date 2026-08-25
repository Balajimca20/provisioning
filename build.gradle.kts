plugins {
    //trick: for the same plugin versions in all sub-modules
    alias(libs.plugins.androidApplication).apply(false)
    alias(libs.plugins.androidLibrary).apply(false)
    alias(libs.plugins.kotlinAndroid).apply(false)
    alias(libs.plugins.kotlinMultiplatform).apply(false)
    alias(libs.plugins.compose.compiler).apply(false)
    id ("androidx.navigation.safeargs") version "2.8.9" apply false
    alias(libs.plugins.google.gms.google.services) apply false
    id ("com.google.firebase.crashlytics") version "3.0.3" apply false
    alias(libs.plugins.sqldelight).apply(false)
    alias(libs.plugins.kotlinSerialization).apply(false)

    // --- Added for the PyQt -> androidApp port ---
    alias(libs.plugins.hiltAndroid).apply(false)
    alias(libs.plugins.ksp).apply(false)
}
