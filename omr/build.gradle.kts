import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import java.net.URI

plugins {
    kotlin("multiplatform")
    id("com.android.kotlin.multiplatform.library")
}

val onnxRuntimeVersion: String by project
val openCvDesktopVersion: String by project
val openCvAndroidVersion: String by project
val commonsNumbersVersion: String by project
val slf4jVersion: String by project
val androidCompileSdk: String by project
val androidMinSdk: String by project
val omrModelUrl: String by project

val models = mapOf(
    "SEGNET" to providers.gradleProperty("omrSegnetModel").get(),
    "ENCODER" to providers.gradleProperty("omrEncoderModel").get(),
    "DECODER" to providers.gradleProperty("omrDecoderModel").get(),
    "STAFF_RELATIONS" to providers.gradleProperty("omrStaffRelationsModel").get(),
    "STAFF_PIECES" to providers.gradleProperty("omrStaffPiecesModel").get(),
)
val modelPackage = "${project.group}.omr"
val modelResourceDirectory = modelPackage.replace('.', '/') + "/models"

val downloadModels by tasks.registering {
    val urls = models.values.associateWith { "$omrModelUrl/$it.zip" }
    val output = layout.buildDirectory.dir("models/archives")
    inputs.property("urls", urls)
    outputs.dir(output)
    outputs.cacheIf { true }
    doLast {
        val directory = output.get().asFile.apply { mkdirs() }
        urls.forEach { (name, url) ->
            val target = File(directory, "$name.zip")
            if (!target.exists()) {
                val partial = File(directory, "$name.zip.part")
                URI(url).toURL().openStream().use { input -> partial.outputStream().use { input.copyTo(it) } }
                partial.renameTo(target)
            }
        }
    }
}

val modelResources by tasks.registering(Sync::class) {
    val archives = layout.buildDirectory.dir("models/archives")
    dependsOn(downloadModels)
    models.values.forEach { name -> from(archives.map { zipTree(it.file("$name.zip")) }) { include("$name.onnx") } }
    into(layout.buildDirectory.dir("models/resources/$modelResourceDirectory"))
}

val generateModelNames by tasks.registering {
    val output = layout.buildDirectory.dir("generated/models/kotlin")
    val packageName = modelPackage
    val resourceDirectory = modelResourceDirectory
    val constants = models.entries.joinToString("\n") { (name, file) -> "    const val $name = \"$file.onnx\"" }
    inputs.property("constants", constants)
    inputs.property("package", packageName)
    outputs.dir(output)
    doLast {
        output.get().file("${packageName.replace('.', '/')}/OmrModelFiles.kt").asFile.apply {
            parentFile.mkdirs()
            writeText("package $packageName\n\ninternal object OmrModelFiles {\n    const val DIRECTORY = \"$resourceDirectory\"\n$constants\n}\n")
        }
    }
}

kotlin {
    android {
        namespace = modelPackage
        compileSdk = androidCompileSdk.toInt()
        minSdk = androidMinSdk.toInt()
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
        named("jvmAndroidMain") {
            kotlin.srcDir(generateModelNames)
            dependencies {
                compileOnly("com.microsoft.onnxruntime:onnxruntime:$onnxRuntimeVersion")
                compileOnly("org.openpnp:opencv:$openCvDesktopVersion")
                implementation("org.apache.commons:commons-numbers-fraction:$commonsNumbersVersion")
                implementation("org.slf4j:slf4j-api:$slf4jVersion")
            }
        }
        jvmMain {
            resources.srcDir(modelResources.map { layout.buildDirectory.dir("models/resources").get() })
            dependencies {
                implementation("com.microsoft.onnxruntime:onnxruntime:$onnxRuntimeVersion")
                implementation("org.openpnp:opencv:$openCvDesktopVersion")
            }
        }
        androidMain {
            resources.srcDir(modelResources.map { layout.buildDirectory.dir("models/resources").get() })
            dependencies {
                implementation("com.microsoft.onnxruntime:onnxruntime-android:$onnxRuntimeVersion")
                implementation("org.opencv:opencv:$openCvAndroidVersion")
            }
        }
        jvmTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
