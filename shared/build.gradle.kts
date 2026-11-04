plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.kmp.library)
}

// Bundles a CV JSON into the app as the offline fallback for the default CV.
// Defaults to the fictional ../sample-cv.json; build with your own via -Pcv.bundledJson=/path/to/cv.json
// or `cv.bundledJson=...` in ~/.gradle/gradle.properties (relative paths resolve against this module's directory).
val bundledCvJsonProperty = providers.gradleProperty("cv.bundledJson")
val bundledCvJson = layout.projectDirectory.file(bundledCvJsonProperty.orElse("../sample-cv.json"))
// True only when the fictional sample is bundled; lets tests skip sample-specific assertions for real builds.
val bundledIsSample = bundledCvJsonProperty.map { false }.orElse(true)
val bundledCvDir = layout.buildDirectory.dir("generated/bundledCv/kotlin")
val generateBundledCv = tasks.register("generateBundledCv") {
    inputs.file(bundledCvJson)
    inputs.property("bundledIsSample", bundledIsSample)
    outputs.dir(bundledCvDir)
    val input = bundledCvJson.map { it.asFile }
    val output = bundledCvDir.map { it.file("com/katlewski/cv/data/BundledCvJson.kt").asFile }
    val isSample = bundledIsSample
    doLast {
        val json = input.get().readText().replace("$", "\${'$'}")
        output.get().apply {
            parentFile.mkdirs()
            writeText(
                "// Generated from the cv.bundledJson Gradle property - do not edit.\npackage com.katlewski.cv.data\n\n" +
                    "internal const val BUNDLED_CV_JSON: String = \"\"\"$json\"\"\"\n\n" +
                    "/** True when the default fictional sample-cv.json is bundled (no cv.bundledJson property). */\n" +
                    "internal const val BUNDLED_IS_SAMPLE: Boolean = ${isSample.get()}\n",
            )
        }
    }
}

kotlin {
    android {
        namespace = "com.katlewski.cv.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    // JVM target only to run commonTest without an Android SDK (./gradlew :shared:jvmTest). Not used by the apps.
    jvm()

    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain {
            kotlin.srcDir(generateBundledCv)
            dependencies {
                api(libs.ktor.client.core)
                implementation(libs.kotlinx.serialization.json)
            }
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.ktor.client.mock)
            implementation(libs.kotlinx.coroutines.test)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}
