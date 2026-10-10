plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "mg.radarreseau"
    compileSdk = 35
    defaultConfig {
        applicationId = "mg.radarreseau"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
}
