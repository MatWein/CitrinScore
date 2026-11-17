import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
}

val javaVersion: String by project
val coroutinesVersion: String by project

kotlin {
    jvmToolchain(javaVersion.toInt())
}

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:$coroutinesVersion")
}

compose.desktop {
    application {
        mainClass = "de.citrinscore.desktop.MainKt"
        jvmArgs += "--enable-native-access=ALL-UNNAMED"

        buildTypes.release.proguard {
            isEnabled = false
        }

        nativeDistributions {
            targetFormats(TargetFormat.Deb, TargetFormat.Rpm, TargetFormat.Msi, TargetFormat.Dmg)
            packageName = rootProject.name
            packageVersion = project.version.toString()
            modules("java.instrument", "java.management", "java.security.jgss", "java.sql", "jdk.security.auth", "jdk.unsupported", "jdk.xml.dom")

            linux.iconFile = file("packaging/icon.png")
            windows {
                iconFile = file("packaging/icon.ico")
                menu = true
                shortcut = true
                upgradeUuid = "bc46cebd-3747-4492-bb3d-63996ce758ae"
            }
            macOS.iconFile = file("packaging/icon.icns")
        }
    }
}
