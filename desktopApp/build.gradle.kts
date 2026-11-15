import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
}

kotlin {
    jvmToolchain(25)
}

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
}

compose.desktop {
    application {
        mainClass = "org.citrinscore.desktop.MainKt"
        // Skiko (Compose-Renderer) lädt native Libraries – ab Java 24 sonst Warnung.
        jvmArgs += "--enable-native-access=ALL-UNNAMED"

        nativeDistributions {
            targetFormats(TargetFormat.Deb, TargetFormat.Rpm, TargetFormat.Msi, TargetFormat.Dmg)
            packageName = "citrinScore"
            packageVersion = "0.1.0"
        }
    }
}
