plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.neras.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.neras.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 6
        versionName = "1.5"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Uses the debug key so the APK installs directly. For Google Play, add your own signing config.
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-ktx:1.9.1")
}
