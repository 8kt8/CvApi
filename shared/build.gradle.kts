plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.kmp.library)
}

// Bundles ../cv.json into the app as the offline fallback, so cv.json stays the single source of truth.
val cvJson = layout.projectDirectory.file("../cv.json")
val bundledCvDir = layout.buildDirectory.dir("generated/bundledCv/kotlin")
val generateBundledCv = tasks.register("generateBundledCv") {
    inputs.file(cvJson)
    outputs.dir(bundledCvDir)
    val input = cvJson.asFile
    val output = bundledCvDir.map { it.file("com/katlewski/cv/data/BundledCvJson.kt").asFile }
    doLast {
        val json = input.readText().replace("$", "\${'$'}")
        output.get().apply {
            parentFile.mkdirs()
            writeText(
                "// Generated from cv.json - do not edit.\npackage com.katlewski.cv.data\n\n" +
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
