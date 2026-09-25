# CvApi

Shared Kotlin Multiplatform **data layer** and CV data for the **My CV** apps:

- Android: [8kt8/MyCV-Android](https://github.com/8kt8/MyCV-Android)
- iOS: [8kt8/MyCV-iOS](https://github.com/8kt8/MyCV-iOS)

Both apps include this repo as a git submodule at `CvApi/`.

## Contents

| Path | What |
| --- | --- |
| `cv.json` | The CV data. Edit this to update the apps. |
| `shared/` | KMP module: models, `cv.json` loading (bundled + remote), display helpers. No UI. |
| `db.json` | Legacy data (2020), kept for existing consumers. |

## UI per platform

The UI is native on each platform and lives in the app repos:

- Android: Jetpack Compose + Material 3
- iOS: SwiftUI, following Apple's Human Interface Guidelines

Both use `CvService` from this module:

```kotlin
val service = CvService()      // owns its HTTP client, call close() when done
service.bundledCv()            // suspend - instant, offline
service.latestCv()             // suspend - downloads cv.json from GitHub
```

```swift
let service = CvService()
let cv = try await service.latestCv()   // Kotlin suspend functions become Swift async
```

## How the data flows

1. `cv.json` is compiled into the app at build time (`BundledCvRepository`), so the app works offline.
2. On launch and on pull-to-refresh, the app downloads the latest `cv.json` from
   `https://raw.githubusercontent.com/8kt8/CvApi/main/cv.json` (`RemoteCvRepository`).

Pushing a change to `cv.json` on `main` updates installed apps on their next refresh. No release needed.

### Store links per experience

```json
"apps": [
  { "name": "My App", "store": "GooglePlay", "url": "https://play.google.com/store/apps/details?id=..." },
  { "name": "My App", "store": "AppStore", "url": "https://apps.apple.com/app/id..." }
]
```

`store` is `GooglePlay`, `AppStore` or `Web`. When `logoUrl` is missing, the company logo comes from the favicon of `companyUrl`.

## Build

```bash
./gradlew :shared:compileKotlinIosSimulatorArm64 :shared:compileAndroidMain
```

Requires JDK 17+, Android SDK platform 37 (`sdk.dir` in `local.properties` or `ANDROID_HOME`) and Xcode for iOS targets.

The version catalog `gradle/libs.versions.toml` is shared with MyCV-Android, so it also lists that app's AndroidX Compose libraries.

## Stack

Kotlin 2.4.20 · Ktor 3.6 · kotlinx.serialization · Android Gradle Plugin 9.4

