import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    kotlin("plugin.serialization")
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.compose")
}

val composeVersion: String by project
val material3Version: String by project
val lifecycleVersion: String by project
val navigationVersion: String by project
val coroutinesVersion: String by project
val serializationVersion: String by project
val fileKitVersion: String by project
val pdfboxVersion: String by project
val exifInterfaceVersion: String by project
val androidSvgVersion: String by project
val activityComposeVersion: String by project
val androidCompileSdk: String by project
val androidMinSdk: String by project

kotlin {
    android {
        namespace = "${project.group}.shared"
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
            api("org.jetbrains.compose.material3:material3:$material3Version")
            api("org.jetbrains.compose.components:components-resources:$composeVersion")
            api("io.github.vinceglb:filekit-dialogs-compose:$fileKitVersion")
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:$lifecycleVersion")
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose:$lifecycleVersion")
            implementation("org.jetbrains.androidx.navigation:navigation-compose:$navigationVersion")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
        }
        jvmMain.dependencies {
            implementation("org.apache.pdfbox:pdfbox:$pdfboxVersion")
        }
        androidMain.dependencies {
            implementation("androidx.exifinterface:exifinterface:$exifInterfaceVersion")
            implementation("com.caverock:androidsvg-aar:$androidSvgVersion")
            api("androidx.activity:activity-compose:$activityComposeVersion")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
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
