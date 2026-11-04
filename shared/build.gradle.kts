plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.kmp.library)
}

// Bundles a CV JSON into the app as the offline fallback for the default CV.
// Defaults to the fictional ../sample-cv.json; build with your own via -Pcv.bundledJson=/path/to/cv.json
// (relative paths resolve against this module's directory).
val bundledCvJson = layout.projectDirectory.file(
    providers.gradleProperty("cv.bundledJson").orElse("../sample-cv.json"),
)
val bundledCvDir = layout.buildDirectory.dir("generated/bundledCv/kotlin")
val generateBundledCv = tasks.register("generateBundledCv") {
    inputs.file(bundledCvJson)
    outputs.dir(bundledCvDir)
    val input = bundledCvJson.map { it.asFile }
    val output = bundledCvDir.map { it.file("com/katlewski/cv/data/BundledCvJson.kt").asFile }
    doLast {
        val json = input.get().readText().replace("$", "\${'$'}")
        output.get().apply {
            parentFile.mkdirs()
            writeText(
                "// Generated from the cv.bundledJson Gradle property - do not edit.\npackage com.katlewski.cv.data\n\n" +
                    "internal const val BUNDLED_CV_JSON: String = \"\"\"$json\"\"\"\n",
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
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}
