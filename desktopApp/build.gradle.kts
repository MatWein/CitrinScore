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
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.11.0")
}

compose.desktop {
    application {
        mainClass = "de.citrinscore.desktop.MainKt"
        jvmArgs += "--enable-native-access=ALL-UNNAMED"

        nativeDistributions {
            targetFormats(TargetFormat.Deb, TargetFormat.Rpm, TargetFormat.Msi, TargetFormat.Dmg)
            packageName = "citrinScore"
            packageVersion = project.version.toString()
            modules("java.instrument", "jdk.security.auth", "jdk.unsupported")

            linux.iconFile = file("packaging/icon.png")
            windows.iconFile = file("packaging/icon.ico")
            macOS.iconFile = file("packaging/icon.icns")
        }
    }
}
