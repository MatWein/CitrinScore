rootProject.name = "citrinScore"

pluginManagement {
    val kotlinVersion: String by settings
    val composeVersion: String by settings
    val agpVersion: String by settings

    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }

    plugins {
        kotlin("multiplatform") version kotlinVersion
        kotlin("jvm") version kotlinVersion
        kotlin("plugin.compose") version kotlinVersion
        id("org.jetbrains.compose") version composeVersion
        id("com.android.application") version agpVersion
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
