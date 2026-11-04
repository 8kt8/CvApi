# CvApi

Shared Kotlin Multiplatform **data layer** for the **My CV** apps:

- Android: [8kt8/MyCV-Android](https://github.com/8kt8/MyCV-Android)
- iOS: [8kt8/MyCV-iOS](https://github.com/8kt8/MyCV-iOS)

Both apps include this repo as a git submodule at `CvApi/`.

This repo contains **no real CV data**. Real CVs are served as static JSON from a private host;
the repo only ships fictional sample data.

## Contents

| Path | What |
| --- | --- |
| `sample-cv.json` | Fictional CV ("Alex Sample"), bundled by default so the apps build and run out of the box. |
| `shared/` | KMP module: models, CV loading (bundled, remote, per-company variants), link parsing. No UI. |

## Where the data comes from

The host (default `https://mycv-szk.pages.dev`, `CvLinks.DEFAULT_BASE_URL`) serves:

| URL | What |
| --- | --- |
| `<baseUrl>/default.json` | The public default CV |
| `<baseUrl>/v/<code>.json` | A per-company variant; `code` is exactly 12 characters `[A-Za-z0-9]` |

Variant share links look like `https://<host>/c/<code>` (also `?v=<code>`). `VariantCode.fromLink(link)`
returns the code, or null when the link has no valid code.

| Mode | `initialCv()` (show immediately) | `latestCv()` (network) |
| --- | --- | --- |
| `CvMode.Default` | CV bundled at build time | `default.json` |
| `CvMode.Variant(code)` | Cached copy of that variant, or `null` (never the bundled CV) | `v/<code>.json`; cached on success, **throws** on failure (never falls back to the default CV) |

Requests time out after 8 s. Unknown JSON keys are ignored.

## Usage

```kotlin
val code = VariantCode.fromLink(intent.data?.toString())          // null -> default CV
val service = CvService(
    client = HttpClient(),
    mode = code?.let { CvMode.Variant(it) } ?: CvMode.Default,
    cache = myPersistentCache,                                    // VariantCache; defaults to InMemoryVariantCache()
)                                                                 // owns its client, call close() when done
service.initialCv()?.let(::show)                                  // suspend - instant, offline
show(service.latestCv())                                          // suspend - throws on failure
```

```swift
let service = CvService()                                         // default CV, default host
// or: CvService(baseUrl: CvLinks.shared.DEFAULT_BASE_URL, code: VariantCode.shared.fromLink(link: url.absoluteString))
if let cv = try await service.initialCv() { show(cv) }
let cv = try await service.latestCv()                             // Kotlin suspend functions become Swift async
```

`bundledCv()` is still available for backwards compatibility.

## Build with your own CV

The CV compiled into the app comes from the Gradle property `cv.bundledJson`
(default `../sample-cv.json`, relative to `shared/`):

```bash
./gradlew :shared:assemble -Pcv.bundledJson=/path/to/my-cv.json
```

The JSON must match the `Cv` model in `shared/src/commonMain/kotlin/com/katlewski/cv/data/CvModels.kt`
(see `sample-cv.json`). Keep your real file outside this repo. Store links per experience:

```json
"apps": [
  { "name": "My App", "store": "GooglePlay", "url": "https://play.google.com/store/apps/details?id=..." },
  { "name": "My App", "store": "AppStore", "url": "https://apps.apple.com/app/id..." }
]
```

`store` is `GooglePlay`, `AppStore` or `Web`. When `logoUrl` is missing, the company logo comes from the favicon of `companyUrl`.

## Build and test

```bash
./gradlew :shared:compileKotlinIosSimulatorArm64 :shared:compileAndroidMain
./gradlew :shared:jvmTest      # common tests on the JVM, no Android SDK or Xcode needed
```

Requires JDK 17+, Android SDK platform 37 (`sdk.dir` in `local.properties` or `ANDROID_HOME`) and Xcode for iOS targets.
The `jvm()` target exists only to run `commonTest` quickly; the apps don't use it.

The version catalog `gradle/libs.versions.toml` is shared with MyCV-Android, so it also lists that app's AndroidX Compose libraries.

## Stack

Kotlin 2.4.20 · Ktor 3.6 · kotlinx.serialization · Android Gradle Plugin 9.4
