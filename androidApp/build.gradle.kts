plugins {
    id("com.android.application")
    kotlin("plugin.compose")
}

val androidCompileSdk: String by project
val androidTargetSdk: String by project
val androidMinSdk: String by project

android {
    namespace = "de.citrinscore.android"
    compileSdk = androidCompileSdk.toInt()

    defaultConfig {
        applicationId = "de.citrinscore"
        minSdk = androidMinSdk.toInt()
        targetSdk = androidTargetSdk.toInt()
        versionCode = 1
        versionName = project.version.toString()
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":shared"))
    implementation("androidx.activity:activity-compose:1.13.0")
}
