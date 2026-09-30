// =====================================================================
// project/build.gradle.kts (Top-level)
// =====================================================================
// PURPOSE:
//   This is the Root-level Gradle build script. It tells Gradle what
//   plugins to apply to the entire project but does not apply them
//   to sub-modules immediately (apply false).
//
// TO CUSTOMIZE:
//   - You generally don't need to change anything here.
// =====================================================================

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
}