plugins {
    id("com.android.application")
    kotlin("plugin.compose")
}

val androidCompileSdk: String by project
val androidTargetSdk: String by project
val androidMinSdk: String by project
val androidVersionCode: String by project

android {
    namespace = "de.citrinscore.android"
    compileSdk = androidCompileSdk.toInt()

    defaultConfig {
        applicationId = "de.citrinscore"
        minSdk = androidMinSdk.toInt()
        targetSdk = androidTargetSdk.toInt()
        versionCode = androidVersionCode.toInt()
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
}
