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
val navigationEventVersion: String by project
val coroutinesVersion: String by project
val collectionVersion: String by project
val serializationVersion: String by project
val fileKitVersion: String by project
val pdfboxVersion: String by project
val mp3spiVersion: String by project
val exifInterfaceVersion: String by project
val androidSvgVersion: String by project
val jnaVersion: String by project
val batikVersion: String by project
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
        optimization {
            consumerKeepRules.file("consumer-rules.pro")
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
            group("jvmAndroid") {
                withJvm()
                withCompilations { it.platformType == KotlinPlatformType.androidJvm }
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
            implementation("org.jetbrains.androidx.navigationevent:navigationevent-compose:$navigationEventVersion")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
            implementation("androidx.collection:collection:$collectionVersion")
        }
        named("jvmAndroidMain").dependencies {
            compileOnly("net.java.dev.jna:jna:$jnaVersion")
        }
        jvmMain.dependencies {
            implementation("net.java.dev.jna:jna:$jnaVersion")
            implementation("org.apache.xmlgraphics:batik-bridge:$batikVersion") {
                exclude(group = "xml-apis", module = "xml-apis")
            }
            implementation("org.apache.pdfbox:pdfbox:$pdfboxVersion")
            implementation("com.googlecode.soundlibs:mp3spi:$mp3spiVersion") {
                exclude(group = "junit")
            }
        }
        androidMain.dependencies {
            implementation(project(":verovio"))
            implementation("net.java.dev.jna:jna:$jnaVersion@aar")
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

val verovioCmakeOptions: String by project
val verovioSource: Directory = rootProject.layout.projectDirectory.dir("third_party/verovio")
val verovioCmakeDirectory = layout.buildDirectory.dir("verovio/cmake")
val verovioNativeDirectory = layout.buildDirectory.dir("verovio/native")
val verovioResourcePrefix = run {
    val os = System.getProperty("os.name").lowercase()
    val arch = when (val name = System.getProperty("os.arch")) {
        "amd64", "x86_64" -> "x86-64"
        else -> name
    }
    when {
        os.startsWith("windows") -> "win32-$arch"
        os.startsWith("mac") -> "darwin-$arch"
        else -> "linux-$arch"
    }
}

val configureVerovio by tasks.registering(Exec::class) {
    val output = verovioNativeDirectory.get().dir(verovioResourcePrefix).asFile.absolutePath
    inputs.dir(verovioSource.dir("cmake"))
    inputs.property("options", verovioCmakeOptions)
    outputs.file(verovioCmakeDirectory.map { it.file("CMakeCache.txt") })
    commandLine(
        listOf(
            "cmake",
            "-S", verovioSource.dir("cmake").asFile.absolutePath,
            "-B", verovioCmakeDirectory.get().asFile.absolutePath,
            "-DBUILD_AS_LIBRARY=ON",
            "-DCMAKE_BUILD_TYPE=Release",
            "-DCMAKE_LIBRARY_OUTPUT_DIRECTORY=$output",
            "-DCMAKE_LIBRARY_OUTPUT_DIRECTORY_RELEASE=$output",
            "-DCMAKE_RUNTIME_OUTPUT_DIRECTORY=$output",
            "-DCMAKE_RUNTIME_OUTPUT_DIRECTORY_RELEASE=$output",
        ) + verovioCmakeOptions.split(","),
    )
}

val buildVerovio by tasks.registering(Exec::class) {
    dependsOn(configureVerovio)
    listOf("src", "include", "libmei", "tools").forEach { inputs.dir(verovioSource.dir(it)) }
    outputs.dir(verovioNativeDirectory)
    commandLine("cmake", "--build", verovioCmakeDirectory.get().asFile.absolutePath, "--config", "Release", "--target", "verovio", "--parallel", Runtime.getRuntime().availableProcessors().toString())
}

val verovioResources by tasks.registering(Zip::class) {
    from(verovioSource.dir("data")) {
        include("Bravura/**", "Bravura.xml", "Leipzig/**", "Leipzig.xml", "text/**", "*.json")
    }
    from(verovioSource.dir("fonts/Leipzig")) {
        include("Leipzig.ttf")
    }
    archiveFileName = "verovio.zip"
    destinationDirectory = layout.buildDirectory.dir("verovio/resources/files")
}

kotlin.sourceSets.named("jvmMain") {
    resources.srcDir(buildVerovio)
}

compose.resources {
    publicResClass = true
    packageOfResClass = "de.citrinscore.resources"
    customDirectory("jvmAndroidMain", verovioResources.map { layout.buildDirectory.dir("verovio/resources").get() })
}

tasks.named<Test>("jvmTest") {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}
