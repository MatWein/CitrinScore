# citrinScore

citrinScore is a free, open-source sheet music library for tablets, phones and desktop computers. It keeps your scores as
plain folders on your own storage, so there is no account, no cloud and no lock-in.

The app is built with Kotlin Multiplatform and Compose Multiplatform and runs on Android, Linux, Windows and macOS.
The shared code base is prepared for iOS.

## Features

- **Library** – browse a folder of your choice, organise scores in sub folders, search and sort them, delete scores and
  folders.
- **Editing** – reorder, remove or add pages after the import and change title, composer and description.
- **Import wizard**
  - **Images** (PNG, JPG, WebP, BMP, SVG): one or more pages, reorderable, rendered as vectors for SVG.
  - **PDF**: a single document, rendered page by page.
  - **Scan**: take photos with the camera on Android/iOS, or pick scanned images and PDFs on desktop. Pages are converted
    to clean black and white with an adaptive threshold that can be tuned with a slider.
  - White margins are cropped automatically, previews show the result before importing.
- **Viewer**
  - Continuous vertical page layout with smooth zooming (pinch, mouse wheel, zoom slider) and panning.
  - Sharp rendering at every zoom level: only the visible area is re-rendered at the current resolution.
  - Previous/next page buttons that page by staff systems, so a partly visible system moves to the top instead of being cut.
  - Keyboard and Bluetooth page turner support (Page Up/Down, arrow keys, space, +/-).
  - Zoom level and position are remembered per score.
- **Audio attachments** – attach WAV files or record with the microphone and play them back in a small player while
  reading the score.
- **Settings** – language (English, German), light/dark/system theme, library location.

## Supported platforms

| Platform | Status |
|---|---|
| Android 8.0 (API 26) and newer | supported |
| Linux, Windows, macOS (JVM desktop) | supported |
| iOS | shared code compiles, no app project yet |

## Score format

Every score is a directory with the suffix `.citscore`, so it can be copied, synced and backed up like any other folder:

```
Etude.citscore/
├── manifest.json     metadata, page list with crop boxes, view state, staff system positions, audio attachments
├── thumbnail.jpg     preview used by the library
├── pages/001.png     original images (image and scan imports)
├── score.pdf         original document (PDF imports)
└── audio/*.wav       audio attachments
```

Unknown keys in `manifest.json` are ignored, missing optional values are recomputed when a score is opened.

## Building

Requirements:

- JDK 17 or newer to start Gradle. Gradle provisions JDK 25 for the build automatically.
- Android SDK with platform 37 for the Android app (configure `sdk.dir` in `local.properties` or `ANDROID_HOME`).

All versions are defined in [`gradle.properties`](gradle.properties).

| Task | Command |
|---|---|
| Run the desktop app | `./gradlew :desktopApp:run` |
| Package the desktop app for the current OS | `./gradlew :desktopApp:packageReleaseDistributionForCurrentOS` |
| Build the Android app | `./gradlew :androidApp:assembleRelease` |
| Run the tests | `./gradlew :shared:jvmTest` |

The release build of the Android app is signed with the debug key for local testing. Replace the signing configuration in
[`androidApp/build.gradle.kts`](androidApp/build.gradle.kts) before publishing to a store.

## Project structure

| Module | Content |
|---|---|
| `shared` | UI, view models, rendering, import and storage for all platforms |
| `androidApp` | Android entry point |
| `desktopApp` | Desktop entry point and packaging |
| `branding` | App icon source and the script that generates all platform icons |

Source sets in `shared`:

| Source set | Platforms | Used for |
|---|---|---|
| `commonMain` | all | almost everything |
| `skikoMain` | desktop, iOS | Skia based image and SVG rendering |
| `mobileMain` | Android, iOS | camera capture |
| `androidMain`, `jvmMain`, `iosMain` | one each | PDF rendering, audio, platform integration |

## Libraries

- [Compose Multiplatform](https://www.jetbrains.com/compose-multiplatform/) and Material 3
- [FileKit](https://github.com/vinceglb/FileKit) for file and folder access, pickers and the camera
- [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization) for the manifest and settings
- [Apache PDFBox](https://pdfbox.apache.org/) for PDF rendering on desktop
- [AndroidSVG](https://github.com/BigBadaboom/androidsvg) for SVG rendering on Android

Audio playback and recording use the platform APIs (Java Sound, Android MediaPlayer/AudioRecord, AVFoundation).

## License

citrinScore is licensed under the [GNU General Public License v3.0](LICENSE).
