plugins {
    id("com.android.library")
}

val androidCompileSdk: String by project
val androidMinSdk: String by project
val androidNdkVersion: String by project
val androidCmakeVersion: String by project
val androidAbis: String by project
val verovioCmakeOptions: String by project

android {
    namespace = "${project.group}.verovio"
    compileSdk = androidCompileSdk.toInt()
    ndkVersion = androidNdkVersion

    defaultConfig {
        minSdk = androidMinSdk.toInt()
        ndk {
            abiFilters += androidAbis.split(",")
        }
        externalNativeBuild {
            cmake {
                arguments += listOf("-DBUILD_AS_ANDROID_LIBRARY=ON", "-DCMAKE_BUILD_TYPE=Release") + verovioCmakeOptions.split(",")
                targets += "verovio"
            }
        }
    }

    externalNativeBuild {
        cmake {
            version = androidCmakeVersion
            path = rootProject.file("third_party/verovio/cmake/CMakeLists.txt")
        }
    }
}
