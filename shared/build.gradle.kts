import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    kotlin("plugin.serialization")
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.compose")
}

val composeVersion = providers.gradleProperty("composeVersion").get()
val androidCompileSdk = providers.gradleProperty("androidCompileSdk").get()
val androidMinSdk = providers.gradleProperty("androidMinSdk").get()
val lifecycleVersion = "2.11.0"

kotlin {
    android {
        namespace = "de.citrinscore.shared"
        compileSdk = androidCompileSdk.toInt()
        minSdk = androidMinSdk.toInt()
        androidResources {
            enable = true
        }
    }

    jvm()

    applyDefaultHierarchyTemplate {
        common {
            group("skiko") {
                withJvm()
                group("ios") {
                    withIos()
                }
            }
            group("mobile") {
                withCompilations { it.platformType == KotlinPlatformType.androidJvm }
                group("ios")
            }
        }
    }

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
            api("org.jetbrains.compose.components:components-resources:$composeVersion")
            api("io.github.vinceglb:filekit-dialogs-compose:0.17.0")
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:$lifecycleVersion")
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose:$lifecycleVersion")
            implementation("org.jetbrains.androidx.navigation:navigation-compose:2.10.0")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.13.0")
        }
        jvmMain.dependencies {
            implementation("org.apache.pdfbox:pdfbox:3.0.8")
        }
        androidMain.dependencies {
            implementation("androidx.exifinterface:exifinterface:1.4.2")
            implementation("com.caverock:androidsvg-aar:1.4")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
        }
        jvmTest.dependencies {
            implementation(compose.desktop.currentOs)
            implementation("org.jetbrains.compose.ui:ui-test:$composeVersion")
        }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "de.citrinscore.resources"
}

tasks.named<Test>("jvmTest") {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}
