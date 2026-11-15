plugins {
    id("com.android.application")
    kotlin("plugin.compose")
}

android {
    namespace = "org.citrinscore.android"
    compileSdk = 37

    defaultConfig {
        applicationId = "org.citrinscore"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":shared"))
    implementation("androidx.activity:activity-compose:1.13.0")
}
