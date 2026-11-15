plugins {
    id("com.android.application")
    kotlin("plugin.compose")
}

val androidCompileSdk = providers.gradleProperty("androidCompileSdk").get()
val androidTargetSdk = providers.gradleProperty("androidTargetSdk").get()
val androidMinSdk = providers.gradleProperty("androidMinSdk").get()

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

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":shared"))
    implementation("androidx.activity:activity-compose:1.13.0")
}
