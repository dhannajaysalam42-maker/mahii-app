// =====================================================================
// app/build.gradle.kts
// =====================================================================
// PURPOSE:
//   This is the main App-level Gradle build script. It contains the
//   configuration for compiling the app, handling dependencies, and
//   defining the Application ID (Package Name).
//
// TO CUSTOMIZE:
//   - Change the `applicationId` to your own unique package name.
//   - Example: "com.yourcompany.yourapp"
// =====================================================================

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    // 📦 Namespace is for internal resource generation
    namespace = "com.example.mahii"
    
    // 🛠️ SDK version to compile against
    compileSdk = 36 // Note: using 36 as defined, could be libs.versions.compileSdk.get().toInt()

    defaultConfig {
        // 🚨 IMPORTANT: CHANGE THIS TO YOUR APP'S PACKAGE NAME
        // This is the unique identifier for your app on the Google Play Store.
        applicationId = "com.example.mahii"
        
        // Minimum Android version required to run this app (API 24 = Android 7.0)
        minSdk = 24
        // Target Android version the app is tested against
        targetSdk = 36
        // App version code (increment this number by 1 for each new Play Store update)
        versionCode = 1
        // App version name (visible to users, e.g., "1.0", "1.1", "2.0")
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // 🔐 Signed build configuration (Play Store ready out-of-the-box!)
    signingConfigs {
        fun envOrDefault(name: String, fallback: String): String {
            return System.getenv(name)?.takeIf { it.isNotBlank() } ?: fallback
        }

        create("release") {
            storeFile = file(envOrDefault("KEYSTORE_FILE", "keystore/keystore.jks"))
            storePassword = envOrDefault("KEYSTORE_PASSWORD", "androidpassword")
            keyAlias = envOrDefault("KEY_ALIAS", "myalias")
            keyPassword = envOrDefault("KEY_PASSWORD", "androidpassword")
        }
    }

    // 👷 Build types define how the app is built
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false // Set to true to shrink code, false for easier debugging
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    
    // ⚙️ Java language level compatibility
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    
    // ⚙️ Kotlin language level compatibility
    kotlinOptions {
        jvmTarget = "11"
    }

    // ✅ Enable ViewBinding to easily access views from XML without findViewById
    buildFeatures {
        viewBinding = true
    }
}

// 📚 Dependencies block defines all the external libraries the app uses
dependencies {
    implementation(libs.androidx.core.ktx)           // Kotlin extensions for core Jetpack libraries
    implementation(libs.androidx.appcompat)          // Support library for older Android versions
    implementation(libs.material)                    // Material Design components (buttons, text fields, etc.)
    implementation(libs.androidx.activity)           // Modern activity APIs
    implementation(libs.androidx.constraintlayout)   // Flexible layout system

    testImplementation(libs.junit)                   // Unit testing library
    androidTestImplementation(libs.androidx.junit)   // Android instrumentation testing
    androidTestImplementation(libs.androidx.espresso.core) // Android UI testing

    // 🌐 TWA dependency - required for launching the Trusted Web Activity
    implementation(libs.androidbrowserhelper)
    
    // 🖼️ SplashScreen API for Android 12+ backwards compatibility
    implementation(libs.androidx.core.splashscreen)
    
    // 🔄 In-App Update API from Google Play Core
    implementation(libs.app.update.ktx)
}
