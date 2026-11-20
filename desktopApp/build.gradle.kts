import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.desktop.application.tasks.AbstractJLinkTask

plugins {
    kotlin("jvm")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
}

val javaVersion: String by project
val coroutinesVersion: String by project
val slf4jVersion: String by project

kotlin {
    jvmToolchain(javaVersion.toInt())
}

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:$coroutinesVersion")
    implementation("org.slf4j:slf4j-api:$slf4jVersion")
}

compose.desktop {
    application {
        mainClass = "de.citrinscore.desktop.MainKt"
        jvmArgs += "--enable-native-access=ALL-UNNAMED"

        buildTypes.release.proguard {
            isEnabled = false
        }

        nativeDistributions {
            targetFormats(TargetFormat.Deb, TargetFormat.Rpm, TargetFormat.Msi)
            packageName = rootProject.name
            packageVersion = project.version.toString()
            modules("java.instrument", "java.management", "java.security.jgss", "java.sql", "jdk.security.auth", "jdk.unsupported", "jdk.xml.dom")

            linux {
                iconFile = file("packaging/icon.png")
                shortcut = true
                menuGroup = "AudioVideo;Audio"
            }
            windows {
                iconFile = file("packaging/icon.ico")
                menu = true
                shortcut = true
                upgradeUuid = "bc46cebd-3747-4492-bb3d-63996ce758ae"
            }
        }
    }
}

if (System.getProperty("os.name").startsWith("Windows")) {
    val vswhere = "${System.getenv("ProgramFiles(x86)")}\\Microsoft Visual Studio\\Installer\\vswhere.exe"
    val msvcRuntimeLibrary = providers.exec {
        commandLine(vswhere, "-latest", "-products", "*", "-find", "VC\\Redist\\MSVC\\*\\x64\\Microsoft.VC*.CRT\\msvcp140_1.dll")
    }.standardOutput.asText.map { output -> file(output.lines().last(String::isNotBlank)) }

    tasks.named<AbstractJLinkTask>("createRuntimeImage") {
        val runtimeBin = destinationDir.dir("bin")
        inputs.file(msvcRuntimeLibrary)
        doLast {
            val library = msvcRuntimeLibrary.get()
            library.copyTo(runtimeBin.get().file(library.name).asFile, overwrite = true)
        }
    }
}
