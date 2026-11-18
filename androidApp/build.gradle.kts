plugins {
    id("com.android.application")
    kotlin("plugin.compose")
}

val androidCompileSdk: String by project
val androidTargetSdk: String by project
val androidMinSdk: String by project
val androidAbis: String by project
val signingStoreFile: Provider<String> = providers.gradleProperty("androidSigningStoreFile")

android {
    namespace = "${project.group}.android"
    compileSdk = androidCompileSdk.toInt()

    defaultConfig {
        applicationId = project.group.toString()
        minSdk = androidMinSdk.toInt()
        targetSdk = androidTargetSdk.toInt()
        versionCode = project.version.toString().split(".").fold(0) { code, part -> code * 100 + part.toInt() }
        versionName = project.version.toString()
        ndk {
            abiFilters += androidAbis.split(",")
        }
    }

    signingConfigs {
        if (signingStoreFile.isPresent) {
            create("release") {
                storeFile = file(signingStoreFile.get())
                storePassword = providers.gradleProperty("androidSigningStorePassword").get()
                keyAlias = providers.gradleProperty("androidSigningKeyAlias").get()
                keyPassword = providers.gradleProperty("androidSigningKeyPassword").get()
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":shared"))
}
