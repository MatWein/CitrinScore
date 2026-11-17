# citrinScore

citrinScore is a free, open-source sheet music library for tablets, phones and desktop computers. It keeps your scores as
plain folders on your own storage, so there is no account, no cloud and no lock-in.

The app is built with Kotlin Multiplatform and Compose Multiplatform and runs on Android, Linux, Windows and macOS.
The shared code base is prepared for iOS.

## Features

- **Library** – browse a folder of your choice, organise scores in sub folders, search and sort them, delete scores and
  folders.
- **Parts** – a score can hold several parts, for example one per instrument, each imported from its own images, PDF or
  MusicXML file. The viewer switches between them and remembers the last one.
- **Editing** – add, rename, reorder or remove parts, reorder, remove or add pages and change title, composer and
  description.
- **Import wizard**
  - **Images** (PNG, JPG, WebP, BMP, SVG): one or more pages, reorderable, rendered as vectors for SVG.
  - **PDF**: a single document, rendered page by page.
  - **MusicXML** (`.musicxml`, `.xml`, `.mxl`): engraved with Verovio and laid out to fit the screen. Zooming reflows the
    music instead of scaling it.
  - **Scan**: take photos with the camera on Android/iOS, or pick scanned images and PDFs on desktop. Pages are converted
    to clean black and white with an adaptive threshold that can be tuned with a slider.
  - White margins are cropped automatically, previews show the result before importing.
- **Viewer**
  - Continuous vertical page layout with smooth zooming (pinch, mouse wheel, zoom slider) and panning.
  - Sharp rendering at every zoom level: only the visible area is re-rendered at the current resolution.
  - Previous/next page buttons that page by staff systems, so a partly visible system moves to the top instead of being cut.
  - Keyboard and Bluetooth page turner support (Page Up/Down, arrow keys, space, +/-).
  - Notes on the score: draw with finger, stylus or mouse and place text, with eraser and undo.
  - Follows the app theme: in the dark theme the score is recoloured to the theme's paper and text colours.
  - The screen stays on while a score is open.
  - Zoom level and position are remembered per score.
- **Audio attachments** – attach WAV or MP3 files or record with the microphone and play them back in a small player
  while reading the score.
- **File attachments** – keep any related file with a score, for example MuseScore or MusicXML files, and save it again
  with "Save as".
- **Settings** – language (English, German), light/dark/system theme, library location.

## Supported platforms

| Platform | Status |
|---|---|
| Android 8.0 (API 26) and newer | supported |
| Linux, Windows, macOS (JVM desktop) | supported |
| iOS | shared code compiles, no app project yet, no MusicXML |

## Score format

Every score is a directory with the suffix `.citscore`, so it can be copied, synced and backed up like any other folder. A score holds one or more parts, for example one per instrument, each in its own directory:

```
Etude.citscore/
├── manifest.json            metadata and the parts with their page lists, crop boxes, view state, staff system positions and attachments
├── thumbnail.jpg            preview of the first part used by the library
└── parts/<id>/
    ├── annotations.json     drawings and text notes, in page coordinates
    ├── pages/001.png        original images (image and scan imports)
    ├── score.pdf            original document (PDF imports)
    ├── score.musicxml       original file (MusicXML imports, also .xml or .mxl)
    ├── audio/               audio attachments (WAV, MP3)
    └── files/               other attached files
```

Scores created before parts existed keep their single part directly in the score directory.

Unknown keys in `manifest.json` are ignored, missing optional values are recomputed when a score is opened.

## Building

Requirements:

- JDK 17 or newer to start Gradle. Gradle provisions JDK 25 for the build automatically.
- Android SDK with platform 37 and the NDK version from `gradle.properties` for the Android app (configure `sdk.dir` in
  `local.properties` or `ANDROID_HOME`).
- CMake and a C++20 compiler to build Verovio for the desktop.
- The Verovio sources, checked out as a git submodule: `git submodule update --init`.

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
| `verovio` | Android library that builds the native Verovio engraver with the NDK |
| `desktopApp` | Desktop entry point and packaging |
| `branding` | App icon source and the script that generates all platform icons |

Source sets in `shared`:

| Source set | Platforms | Used for |
|---|---|---|
| `commonMain` | all | almost everything |
| `skikoMain` | desktop, iOS | Skia based image rendering |
| `jvmAndroidMain` | desktop, Android | Verovio binding through JNA |
| `mobileMain` | Android, iOS | camera capture |
| `androidMain`, `jvmMain`, `iosMain` | one each | PDF rendering, audio, platform integration |

## Libraries

- [Compose Multiplatform](https://www.jetbrains.com/compose-multiplatform/) and Material 3
- [FileKit](https://github.com/vinceglb/FileKit) for file and folder access, pickers and the camera
- [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization) for the manifest and settings
- [Apache PDFBox](https://pdfbox.apache.org/) for PDF rendering on desktop
- [AndroidSVG](https://github.com/BigBadaboom/androidsvg) for SVG rendering on Android
- [Apache Batik](https://xmlgraphics.apache.org/batik/) for SVG rendering on desktop
- [Verovio](https://www.verovio.org/) for engraving MusicXML, called through [JNA](https://github.com/java-native-access/jna)
- [JLayer and MP3SPI](https://github.com/pdudits/soundlibs) for MP3 playback on desktop

Audio playback and recording use the platform APIs (Java Sound, Android MediaPlayer/AudioRecord, AVFoundation).

## License

citrinScore is licensed under the [GNU General Public License v3.0](LICENSE).
