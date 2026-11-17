plugins {
    id("com.android.library")
}

val androidCompileSdk: String by project
val androidMinSdk: String by project
val androidNdkVersion: String by project
val androidCmakeVersion: String by project
val androidAbis: String by project
val fluidsynthCmakeOptions: String by project

android {
    namespace = "${project.group}.fluidsynth"
    compileSdk = androidCompileSdk.toInt()
    ndkVersion = androidNdkVersion

    defaultConfig {
        minSdk = androidMinSdk.toInt()
        ndk {
            abiFilters += androidAbis.split(",")
        }
        externalNativeBuild {
            cmake {
                arguments += listOf("-DCMAKE_BUILD_TYPE=Release") + fluidsynthCmakeOptions.split(",")
                targets += "libfluidsynth"
            }
        }
    }

    externalNativeBuild {
        cmake {
            version = androidCmakeVersion
            path = rootProject.file("third_party/fluidsynth/CMakeLists.txt")
        }
    }
}
