# Development

citrinScore is built with Kotlin Multiplatform and Compose Multiplatform.

| Platform | Status |
|---|---|
| Android 8.0 (API 26) and newer | supported |
| Linux, Windows (JVM desktop) | supported |

macOS and iOS are not supported.

## Building

Requirements:

- JDK 17 or newer to start Gradle. Gradle provisions JDK 25 for the build automatically.
- Android SDK with platform 37 and the NDK and CMake versions from `gradle.properties` for the Android app (configure
  `sdk.dir` in `local.properties` or `ANDROID_HOME`).
- CMake 3.24 or newer and a C++20 compiler to build Verovio and FluidSynth for the desktop.
- The third party sources, checked out as git submodules: `git submodule update --init --recursive`.
- `dpkg-deb` and `rpmbuild` to package the desktop app on Linux, since jpackage builds both a `.deb` and an `.rpm`
  (on Arch Linux: `pacman -S dpkg rpm-tools`).

All versions are defined in [`gradle.properties`](gradle.properties).

| Task | Command |
|---|---|
| Run the desktop app | `./gradlew :desktopApp:run` |
| Package the desktop app for the current OS | `./gradlew :desktopApp:packageReleaseDistributionForCurrentOS` |
| Build the Android app | `./gradlew :androidApp:assembleRelease` |
| Run the tests | `./gradlew :shared:jvmTest` |

The desktop app keeps its settings and extracted resources in `~/.citrinscore`. Start it with
`-Dcitrinscore.home=/absolute/path` to use another directory, for example a separate test library.

## Logging

The app logs through [SLF4J](https://www.slf4j.org/). Opened screens, user actions, file operations and synchronization
steps are logged as info, calls into Verovio, FluidSynth and the audio system as debug and failed operations as warnings
with their stack trace.

- Desktop: [Logback](https://logback.qos.ch/) writes info messages to `~/.citrinscore/logs/citrinscore.log` and the
  console. The log rolls over daily or at 10 MB into compressed `.log.gz` files, which are kept for 14 days and 100 MB
  in total. The configuration is [`desktopApp/src/main/resources/logback.xml`](desktopApp/src/main/resources/logback.xml).
- Android: [logback-android](https://github.com/tony19/logback-android) writes warnings and errors to logcat with the
  tag `citrinScore` (`adb logcat -s citrinScore`). The configuration is
  [`androidApp/src/main/assets/logback.xml`](androidApp/src/main/assets/logback.xml).

## Project structure

| Module | Content |
|---|---|
| `shared` | UI, view models, rendering, import and storage for all platforms |
| `androidApp` | Android entry point |
| `verovio` | Android library that builds the native Verovio engraver with the NDK |
| `fluidsynth` | Android library that builds the native FluidSynth synthesizer with the NDK |
| `desktopApp` | Desktop entry point and packaging |
| `branding` | App icon source and the script that generates all platform icons |

| Source set in `shared` | Platforms | Used for |
|---|---|---|
| `commonMain` | all | almost everything |
| `jvmAndroidMain` | desktop, Android | Verovio and FluidSynth bindings through JNA, Git synchronization |
| `androidMain`, `jvmMain` | one each | rendering, audio, camera, storage and platform integration |

## Libraries and licenses

The main building blocks are [Compose Multiplatform](https://www.jetbrains.com/compose-multiplatform/),
[FileKit](https://github.com/vinceglb/FileKit), [Verovio](https://www.verovio.org/) for engraving MusicXML,
[FluidSynth](https://www.fluidsynth.org/) with the [GeneralUser GS](https://www.schristiancollins.com/generaluser) sound
font for playback, [JGit](https://www.eclipse.org/jgit/) for synchronization, [Apache PDFBox](https://pdfbox.apache.org/)
and [Apache Batik](https://xmlgraphics.apache.org/batik/) on desktop and [AndroidSVG](https://github.com/BigBadaboom/androidsvg)
on Android.

The licenses of all libraries are collected at build time with [AboutLibraries](https://github.com/mikepenz/AboutLibraries)
and shown in the app. Native components that are not Maven dependencies are described in
[`shared/aboutlibraries`](shared/aboutlibraries). The build fails when a dependency uses a license that is not in the
allowed list in [`shared/build.gradle.kts`](shared/build.gradle.kts).

## Score format

Every score is a directory with the suffix `.citscore`, so it can be copied, synced and backed up like any other folder.
A score holds one or more parts, each in its own directory:

```
Etude.citscore/
├── manifest.json            metadata and the parts with their page lists, crop boxes, staff system positions and attachments
├── device.json              last opened time, selected part and view state of this device, not synchronized
├── thumbnail.jpg            preview of the first part used by the library
└── parts/<id>/
    ├── annotations.json     drawings and text notes, in page coordinates or anchored to measures for MusicXML
    ├── pages/001.png        original images (image and scan imports)
    ├── score.pdf            original document (PDF imports)
    ├── score.musicxml       original file (MusicXML imports, also .xml or .mxl)
    ├── audio/               audio attachments (WAV, MP3)
    └── files/               other attached files
```

Scores created before parts existed keep their single part directly in the score directory. Unknown keys in
`manifest.json` are ignored, missing optional values are recomputed when a score is opened.

## Library location and synchronization

When synchronization is set up, the library folder is a Git repository. Its `.gitignore` excludes the `device.json`
files, and empty folders get a `.gitkeep` file so that they are synchronized as well.

Git works on plain files, which Android only offers for the app's own storage without special permissions. A library in
a folder chosen through the system folder picker can therefore not be synchronized on Android. The app sets
`hasFragileUserData`, so Android asks whether to keep the data when the app is uninstalled from the system settings.

Moving the library copies all entries to the new location, saves the settings and then deletes the old entries. Nothing
is changed when the target already contains an entry with the same name or copying fails. The Git repository is left
behind and the synchronization disconnected when the new location cannot be synchronized.

## Signing the Android app

Release builds are signed with the key configured by these Gradle properties, for example in
`~/.gradle/gradle.properties` (never in the repository):

```properties
androidSigningStoreFile=/absolute/path/to/citrinscore-release.jks
androidSigningStorePassword=...
androidSigningKeyAlias=citrinscore
androidSigningKeyPassword=...
```

Without them, release builds are signed with the debug key for local testing. The version code is derived from the
version (`1.2.3` → `10203`).

## Releases

Two workflows on GitHub Actions build the `.deb`, `.rpm`, portable `.tar.gz` and Arch Linux package, the Windows `.msi` and
the signed Android `.apk`:

- [Build](.github/workflows/build.yml) only builds the packages and keeps them as workflow artifacts for seven days.
  Start it manually and choose a platform or all of them.
- [Release](.github/workflows/release.yml) is started manually as well. If there is no tag for the current `version`
  yet, it builds all packages, creates the tag `v<version>` and a draft release with the packages. Check the generated
  notes and publish it.

To release a new version, set the new `version` in [`gradle.properties`](gradle.properties), push it to `master` and
start the Release workflow.

Building the Android app needs these repository secrets:

| Secret | Value |
|---|---|
| `ANDROID_SIGNING_KEYSTORE` | the keystore file, Base64 encoded (`base64 -w0 citrinscore-release.jks`) |
| `ANDROID_SIGNING_STORE_PASSWORD` | keystore password |
| `ANDROID_SIGNING_KEY_ALIAS` | key alias |
| `ANDROID_SIGNING_KEY_PASSWORD` | key password |

The Arch Linux job also uploads the `PKGBUILD` and `.SRCINFO` with the checksums of the release as workflow artifact
`citrinScore-arch`, ready to be pushed to the AUR. The template lives in [`desktopApp/packaging/arch`](desktopApp/packaging/arch).
