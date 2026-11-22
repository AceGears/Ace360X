plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Commit the build came from, so bug reports can name an exact build.
val gitSha: String = System.getenv("GITHUB_SHA")?.take(7) ?: "local"

android {
    namespace = "com.acegears.ace360x"
    compileSdk = 35
    // Pinned NDK (CLAUDE.md §5). AGP installs it automatically if missing.
    ndkVersion = "29.0.14206865"

    defaultConfig {
        applicationId = "com.acegears.ace360x"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0-dev"
        buildConfigField("String", "GIT_SHA", "\"$gitSha\"")

        ndk { abiFilters += "arm64-v8a" }
        externalNativeBuild {
            cmake {
                arguments += listOf("-DANDROID_STL=c++_static")
                // Only build our library and what it links; skips upstream tools/demos.
                targets += "ace360x"
            }
        }
    }

    signingConfigs {
        // Shared DEBUG-ONLY key so every CI build can update the previous one
        // on your phone. It is public on purpose and must never sign a release.
        getByName("debug") {
            storeFile = rootProject.file("ci/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "+$gitSha"
            // The emulation core is always built optimised: an -O0 JIT/GPU core is
            // too slow to be representative. Kotlin/Java code stays debuggable.
            externalNativeBuild {
                cmake { arguments += "-DCMAKE_BUILD_TYPE=Release" }
            }
        }
        release {
            isMinifyEnabled = false
            // No release signing configured yet (Phase 12).
        }
    }

    externalNativeBuild {
        cmake {
            path = file("../native/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        // Extracted native libs, so the Vulkan loader can find the debug-only validation
        // layer in the app's library directory (it is absent from release builds).
        jniLibs { useLegacyPackaging = true }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    testImplementation("junit:junit:4.13.2")
}
