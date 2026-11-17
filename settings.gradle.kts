rootProject.name = "citrinScore"

pluginManagement {
    val kotlinVersion = providers.gradleProperty("kotlinVersion").get()
    val composeVersion = providers.gradleProperty("composeVersion").get()
    val agpVersion = providers.gradleProperty("agpVersion").get()

    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }

    plugins {
        kotlin("multiplatform") version kotlinVersion
        kotlin("jvm") version kotlinVersion
        kotlin("plugin.compose") version kotlinVersion
        kotlin("plugin.serialization") version kotlinVersion
        id("org.jetbrains.compose") version composeVersion
        id("com.android.application") version agpVersion
        id("com.android.library") version agpVersion
        id("com.android.kotlin.multiplatform.library") version agpVersion
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

include(":shared")
include(":androidApp")
include(":desktopApp")
include(":verovio")
