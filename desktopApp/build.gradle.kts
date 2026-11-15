import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
}

val composeVersion: String by project

kotlin {
    jvmToolchain(25)
}

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.compose.components:components-resources:$composeVersion")
}

compose.resources {
    packageOfResClass = "de.citrinscore.desktop.resources"
}

compose.desktop {
    application {
        mainClass = "de.citrinscore.desktop.MainKt"
        jvmArgs += "--enable-native-access=ALL-UNNAMED"

        nativeDistributions {
            targetFormats(TargetFormat.Deb, TargetFormat.Rpm, TargetFormat.Msi, TargetFormat.Dmg)
            packageName = "citrinScore"
            packageVersion = project.version.toString()

            linux.iconFile = file("src/main/composeResources/drawable/app_icon.png")
            windows.iconFile = file("packaging/icon.ico")
            macOS.iconFile = file("packaging/icon.icns")
        }
    }
}
