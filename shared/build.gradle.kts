import com.mikepenz.aboutlibraries.plugin.StrictMode
import java.time.LocalDate
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    kotlin("plugin.serialization")
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.compose")
    id("com.mikepenz.aboutlibraries.plugin")
}

val composeVersion: String by project
val aboutLibrariesVersion: String by project
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
val ktmidiVersion: String by project
val jgitVersion: String by project
val batikVersion: String by project
val slf4jVersion: String by project
val logbackVersion: String by project
val logbackAndroidVersion: String by project
val activityComposeVersion: String by project
val androidCompileSdk: String by project
val androidMinSdk: String by project
val appAuthor: String by project
val appSourceUrl: String by project

val generateAppInfo by tasks.registering {
    val appPackage = project.group.toString()
    val properties = mapOf(
        "NAME" to rootProject.name,
        "ID" to appPackage,
        "VERSION" to project.version.toString(),
        "AUTHOR" to appAuthor,
        "SOURCE_URL" to appSourceUrl,
    )
    val output = layout.buildDirectory.dir("generated/appInfo/kotlin")
    inputs.properties(properties)
    outputs.dir(output)
    outputs.upToDateWhen { false }
    doLast {
        val values = properties + ("BUILD_DATE" to LocalDate.now().toString())
        val constants = values.entries.joinToString("\n") { (name, value) -> "    const val $name = \"$value\"" }
        output.get().file("${appPackage.replace('.', '/')}/AppInfo.kt").asFile.apply {
            parentFile.mkdirs()
            writeText("package $appPackage\n\nobject AppInfo {\n$constants\n}\n")
        }
    }
}

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
            group("jvmAndroid") {
                withJvm()
                withCompilations { it.platformType == KotlinPlatformType.androidJvm }
            }
        }
    }

    sourceSets {
        commonMain {
            kotlin.srcDir(generateAppInfo)
        }
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
            implementation("com.mikepenz:aboutlibraries-compose-m3:$aboutLibrariesVersion")
            implementation("org.slf4j:slf4j-api:$slf4jVersion")
        }
        named("jvmAndroidMain").dependencies {
            implementation(project(":omr"))
            compileOnly("net.java.dev.jna:jna:$jnaVersion")
            implementation("dev.atsushieno:ktmidi:$ktmidiVersion")
            implementation("org.eclipse.jgit:org.eclipse.jgit:$jgitVersion")
        }
        jvmMain.dependencies {
            implementation("net.java.dev.jna:jna:$jnaVersion")
            implementation("org.apache.xmlgraphics:batik-bridge:$batikVersion") {
                exclude(group = "xml-apis", module = "xml-apis")
            }
            implementation("org.apache.pdfbox:pdfbox:$pdfboxVersion")
            implementation("ch.qos.logback:logback-classic:$logbackVersion")
            implementation("com.googlecode.soundlibs:mp3spi:$mp3spiVersion") {
                exclude(group = "junit")
            }
        }
        androidMain.dependencies {
            implementation(project(":verovio"))
            implementation(project(":fluidsynth"))
            implementation("net.java.dev.jna:jna:$jnaVersion@aar")
            implementation("androidx.exifinterface:exifinterface:$exifInterfaceVersion")
            implementation("com.caverock:androidsvg-aar:$androidSvgVersion")
            implementation("com.github.tony19:logback-android:$logbackAndroidVersion")
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
val fluidsynthCmakeOptions: String by project
val thirdParty: Directory = rootProject.layout.projectDirectory.dir("third_party")
val verovioSource: Directory = thirdParty.dir("verovio")
val nativeResourcePrefix = run {
    val os = System.getProperty("os.name").lowercase()
    val arch = when (val name = System.getProperty("os.arch")) {
        "amd64", "x86_64" -> "x86-64"
        else -> name
    }
    if (os.startsWith("windows")) "win32-$arch" else "linux-$arch"
}

fun registerNativeBuild(name: String, source: Directory, sources: List<String>, options: String, target: String): TaskProvider<Exec> {
    val buildDirectory = layout.buildDirectory.dir("native/$name")
    val configure = tasks.register<Exec>("configure$name") {
        inputs.file(source.file("CMakeLists.txt"))
        inputs.property("options", options)
        outputs.file(buildDirectory.map { it.file("CMakeCache.txt") })
        commandLine(
            listOf("cmake", "-S", source.asFile.absolutePath, "-B", buildDirectory.get().asFile.absolutePath, "-DCMAKE_BUILD_TYPE=Release") +
                options.split(","),
        )
    }
    return tasks.register<Exec>("build$name") {
        dependsOn(configure)
        sources.forEach { inputs.dir(source.dir(it)) }
        outputs.dir(buildDirectory)
        commandLine(
            "cmake", "--build", buildDirectory.get().asFile.absolutePath, "--config", "Release", "--target", target,
            "--parallel", Runtime.getRuntime().availableProcessors().toString(),
        )
    }
}

val buildVerovio = registerNativeBuild(
    name = "Verovio",
    source = verovioSource.dir("cmake"),
    sources = listOf("../src", "../include", "../libmei", "../tools"),
    options = "-DBUILD_AS_LIBRARY=ON,-DCMAKE_WINDOWS_EXPORT_ALL_SYMBOLS=ON,$verovioCmakeOptions",
    target = "verovio",
)

val buildFluidSynth = registerNativeBuild(
    name = "FluidSynth",
    source = thirdParty.dir("fluidsynth"),
    sources = listOf("src", "include"),
    options = fluidsynthCmakeOptions,
    target = "libfluidsynth",
)

val nativeLibraries by tasks.registering(Sync::class) {
    val fluidsynth = System.mapLibraryName("fluidsynth")
    val fluidsynthWindows = "libfluidsynth-3.dll"
    from(buildVerovio, buildFluidSynth)
    include("**/${System.mapLibraryName("verovio")}", "**/$fluidsynth", "**/$fluidsynthWindows")
    exclude("**/CMakeFiles/**")
    eachFile { path = name.replace(fluidsynthWindows, fluidsynth) }
    includeEmptyDirs = false
    into(layout.buildDirectory.dir("native/libraries/$nativeResourcePrefix"))
}

val verovioResources by tasks.registering(Zip::class) {
    from(verovioSource.dir("data")) {
        include("Bravura/**", "Bravura.xml", "Leipzig/**", "Leipzig.xml", "text/**", "*.json")
    }
    from(verovioSource.dir("fonts/Leipzig")) {
        include("Leipzig.ttf")
    }
    archiveFileName = "verovio.zip"
    destinationDirectory = layout.buildDirectory.dir("native/archives")
}

val bundledResources by tasks.registering(Sync::class) {
    from(verovioResources)
    from(tasks.named("exportLibraryDefinitions"))
    from(thirdParty.dir("generaluser-gs")) {
        include("GeneralUser-GS.sf2")
    }
    into(layout.buildDirectory.dir("bundled/resources/files"))
}

kotlin.sourceSets.named("jvmMain") {
    resources.srcDir(nativeLibraries.map { layout.buildDirectory.dir("native/libraries").get() })
}

aboutLibraries {
    collect {
        configPath = layout.projectDirectory.dir("aboutlibraries")
        filterVariants.addAll("android", "jvm")
    }
    license {
        strictMode = StrictMode.FAIL
        allowedLicenses.addAll("AGPL-3.0-only", "Apache-2.0", "CC-BY-4.0", "CC0-1.0", "BSD-2-Clause", "BSD-3-Clause", "LGPL-2.1-or-later", "LGPL-3.0-or-later", "MIT", "OFL-1.1", "generaluser-gs", "msvc-runtime", "6f2d5d14a0a0caf9830496e9db99e0d3")
        val logback = listOf("ch.qos.logback:logback-classic", "ch.qos.logback:logback-core")
        allowedLicensesMap.putAll(mapOf("EPL-2.0" to logback, "LGPL-2.1-only" to logback, "BSD License" to listOf("org.openpnp:opencv")))
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "de.citrinscore.resources"
    customDirectory("jvmAndroidMain", bundledResources.map { layout.buildDirectory.dir("bundled/resources").get() })
}

tasks.named<Test>("jvmTest") {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}
