plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.compose")
}

val composeVersion = "1.12.2"

kotlin {
    android {
        namespace = "org.citrinscore.shared"
        compileSdk = 37
        minSdk = 26
    }

    jvm()

    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            api("org.jetbrains.compose.runtime:runtime:$composeVersion")
            api("org.jetbrains.compose.foundation:foundation:$composeVersion")
            api("org.jetbrains.compose.ui:ui:$composeVersion")
            api("org.jetbrains.compose.material3:material3:1.9.0")
        }
    }
}
