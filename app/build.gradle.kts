import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

room {
    schemaDirectory("$projectDir/schemas")
}

val localProps = Properties().apply {
    val f = project.rootProject.file("local.properties")
    if (f.exists()) load(f.inputStream())
}

android {
    namespace = "com.mawaai.love.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.mawaai.love.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        ndk {
            // Phase 17 hardening: include both 64-bit and 32-bit ARM so the
            // app loads on real phones running in 32-bit-only mode (some
            // older Snapdragon mid-range devices still report
            // `armeabi-v7a` as their primary ABI even though the SoC is
            // 64-bit capable). x86_64 stays for emulator support; we drop
            // x86 because no real device ships it and it inflates the APK.
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86_64")
        }

        buildConfigField("String", "GEMINI_API_KEY", "\"${localProps.getProperty("GEMINI_API_KEY") ?: ""}\"")
        buildConfigField("String", "PEXELS_API_KEY", "\"${localProps.getProperty("PEXELS_API_KEY") ?: ""}\"")
        buildConfigField("String", "HUGGINGFACE_API_KEY", "\"${localProps.getProperty("HUGGINGFACE_API_KEY") ?: ""}\"")
        buildConfigField("String", "REMOVE_BG_API_KEY", "\"${localProps.getProperty("REMOVE_BG_API_KEY") ?: ""}\"")
        buildConfigField("String", "CLOUDFLARE_ACCOUNT_ID", "\"${localProps.getProperty("CLOUDFLARE_ACCOUNT_ID") ?: ""}\"")
        buildConfigField("String", "CLOUDFLARE_API_TOKEN", "\"${localProps.getProperty("CLOUDFLARE_API_TOKEN") ?: ""}\"")
    }

    signingConfigs {
        create("release") {
            val storeFilePath = localProps.getProperty("RELEASE_STORE_FILE")
            val storePass = localProps.getProperty("RELEASE_STORE_PASSWORD")
            val keyAliasProp = localProps.getProperty("RELEASE_KEY_ALIAS")
            val keyPass = localProps.getProperty("RELEASE_KEY_PASSWORD")
            if (!storeFilePath.isNullOrBlank() && file(storeFilePath).exists() &&
                !storePass.isNullOrBlank() && !keyAliasProp.isNullOrBlank() && !keyPass.isNullOrBlank()
            ) {
                storeFile = file(storeFilePath)
                storePassword = storePass
                keyAlias = keyAliasProp
                keyPassword = keyPass
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val rel = signingConfigs.getByName("release")
            signingConfig = if (rel.storeFile != null) rel else signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    kotlin {
        jvmToolchain(21)
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        // Phase 17 hardening: ship `.so` files compressed in the APK
        // (legacy mode) instead of the AGP-8 default of storing them
        // page-aligned & uncompressed. Some device-side dynamic linkers
        // fail to register JNI symbols for uncompressed entries when
        // the AAR was authored against the older packaging convention,
        // which leaks as `UnsatisfiedLinkError: Mat.n_Mat()` despite
        // `OpenCVLoader.initLocal()` returning true. Trade-off: ~2-3 MB
        // larger APK, ~30 ms slower first dlopen — acceptable.
        jniLibs {
            useLegacyPackaging = true
        }
    }
    androidResources {
        noCompress += listOf("tflite")
    }
    lint {
        // The bundled Compose runtime lint (Compose BOM 2024.02.00) ships
        // kotlinx-metadata-jvm 2.0.0, which cannot read Kotlin 2.1.0
        // metadata, so `ComposableStateFlowValueDetector` crashes lint
        // analysis on every file it visits. The detector reports under
        // the issue id `StateFlowValueCalledInComposition` (not its class
        // name — the two are different). Disable until Compose BOM
        // catches up.
        disable += "StateFlowValueCalledInComposition"
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.activity.compose)
    
    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.icons.extended)
    implementation(libs.androidx.compose.material3.windowsizeclass)
    
    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    
    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    
    // Navigation
    implementation(libs.navigation.compose)
    
    // WorkManager
    implementation(libs.work.runtime.ktx)
    
    // Utilities
    implementation(libs.coil.compose)
    implementation(libs.lottie.compose)
    implementation(libs.datastore.preferences)
    implementation(libs.splashscreen)
    implementation(libs.androidx.palette)
    implementation(libs.accompanist.systemuicontroller)
    implementation(libs.accompanist.permissions)
    
    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)
    
    // Media
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)

    // AI / Computer Vision (Phase C)
    implementation(libs.opencv)
    implementation(libs.mlkit.subject.segmentation)
    implementation(libs.tensorflow.lite)
    implementation(libs.tensorflow.lite.support)

    // Test
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
