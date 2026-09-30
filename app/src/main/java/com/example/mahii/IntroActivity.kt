/*
 * =====================================================================
 * IntroActivity.kt
 * =====================================================================
 * PURPOSE:
 *   This is the LAUNCHER activity — the first screen the user sees.
 *   It shows a native onboarding screen (app logo, name, description,
 *   and a "Get Started" button) so the app registers real usage time
 *   in Android's app-usage tracker. This solves the TWA problem where
 *   Chrome-based activity is NOT counted as app usage.
 *
 * HOW IT WORKS:
 *   1. A splash screen is shown first (Android 12+ SplashScreen API).
 *   2. If the user has already completed onboarding (saved in SharedPreferences),
 *      the app skips the intro and directly launches the TWA website.
 *   3. If it's the first launch, the onboarding screen is displayed.
 *   4. When the user taps "Get Started", their preference is saved and
 *      the TWA (website) is launched using TwaLauncher.
 *   5. The in-app update checker runs in the background to notify
 *      users of any future updates on the Play Store.
 *
 * FILES THIS DEPENDS ON:
 *   - assets/twa-manifest.json  → Provides app name, description, and website URL
 *   - ManifestReader.kt         → Reads and parses the twa-manifest.json
 *   - AppUpdateManagerUtil.kt   → Handles in-app update checking
 *   - res/layout/activity_intro.xml → The onboarding screen layout
 *
 * TO CUSTOMIZE:
 *   - Change app name/description/URL in assets/twa-manifest.json
 *   - Change onboarding text in res/values/strings.xml
 *   - Change the package name in build.gradle.kts
 * =====================================================================
 */

package com.example.mahii

// --- Android Framework Imports ---
import android.os.Bundle          // Used to pass data between activities and save state
import android.view.View          // Used to control UI element visibility
import androidx.appcompat.app.AppCompatActivity  // Base class for modern Android activities
import androidx.core.content.edit // Kotlin extension to simplify SharedPreferences editing
import androidx.core.net.toUri    // Kotlin extension to convert a String to a Uri object
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen  // Android 12+ splash screen API

// --- Project-specific Imports ---
import com.example.mahii.databinding.ActivityIntroBinding  // View Binding auto-generated class for activity_intro.xml

// --- Third-party / Google Library Imports ---
import com.google.android.play.core.install.model.AppUpdateType  // Defines update types: IMMEDIATE or FLEXIBLE
import com.google.androidbrowserhelper.trusted.TwaLauncher        // Launches a Trusted Web Activity (your website in Chrome)

// ─── SharedPreferences Constants ─────────────────────────────────────
// These constants define the key names used in SharedPreferences
// to remember whether the user has already completed the onboarding.
const val PREF_NAME = "app_prefs"      // Name of the SharedPreferences file
const val KEY_STARTED = "started"      // Key to store if onboarding is completed (true/false)

/**
 * IntroActivity — The main launcher activity.
 *
 * This activity serves as the entry point of the app. It displays a native
 * onboarding screen with the app's name, description, and a "Get Started"
 * button. This ensures that the app registers real usage time in Android's
 * usage tracker (important for Google Play closed testing compliance).
 *
 * After the user taps "Get Started", the app launches the Trusted Web Activity
 * (TWA) which opens the configured website URL inside Chrome in a fullscreen,
 * app-like experience.
 */
class IntroActivity : AppCompatActivity() {

    // 📐 View Binding instance — auto-generated from activity_intro.xml
    // This lets us access UI elements like binding.itemTitle, binding.subtitle, etc.
    // without using findViewById(). It's type-safe and null-safe.
    private lateinit var binding: ActivityIntroBinding

    // 🔄 App update manager utility instance for handling in-app updates.
    // This checks if a newer version of the app is available on the Play Store.
    private lateinit var appUpdateManagerUtil: AppUpdateManagerUtil

    /**
     * onCreate() — Called when the activity is first created.
     *
     * This is where we set up the UI, check if onboarding was already done,
     * read app configuration from the manifest, and initialize the update checker.
     */
    override fun onCreate(savedInstanceState: Bundle?) {

        // ─── Step 1: Install the Splash Screen ──────────────────────────
        // Shows the splash screen (defined in res/values/splash.xml).
        // setKeepOnScreenCondition { false } means the splash screen will
        // dismiss immediately after it's shown (no loading delay).
        installSplashScreen().setKeepOnScreenCondition { false }

        // Call the parent class's onCreate — required for proper activity setup
        super.onCreate(savedInstanceState)

        // ─── Step 2: Inflate the layout using View Binding ──────────────
        // This loads the activity_intro.xml layout and creates the binding object
        binding = ActivityIntroBinding.inflate(layoutInflater)
        setContentView(binding.root)  // Set the inflated layout as the content view

        // ─── Step 3: Check if user already completed onboarding ─────────
        // We use SharedPreferences to persist a boolean flag.
        // If the user already tapped "Get Started" before, skip the intro.
        val prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE)

        if (prefs.getBoolean(KEY_STARTED, false)) {
            // User has already seen onboarding → go directly to the website
            startMain()
            return  // Exit onCreate early, no need to set up the intro UI
        } else {
            // First-time user → show the onboarding screen
            // The layout starts as GONE (invisible) to prevent flicker,
            // so we make it VISIBLE here
            binding.parent.visibility = View.VISIBLE
        }

        // ─── Step 4: Read app configuration from twa-manifest.json ──────
        // The manifest file contains the app name, description, and website URL.
        // This data is used to populate the onboarding screen dynamically.
        val manifest = readManifestData(this)

        // ─── Step 5: Apply title & description to the onboarding screen ─
        // Set the app name and description from the manifest JSON
        binding.itemTitle.text = manifest.name          // e.g., "Virtuala FansOnly"
        binding.subtitle.text = manifest.description    // e.g., "Connect with friends..."

        // ─── Step 6: Handle "Get Started" button click ──────────────────
        // When the user taps the button:
        //   1. Save that onboarding is complete (won't show again)
        //   2. Launch the TWA (website)
        binding.getStartedButton.setOnClickListener {
            prefs.edit { putBoolean(KEY_STARTED, true) }  // Save preference
            startMain()  // Launch the website
        }

        // ─── Step 7: Initialize In-App Update Checker ───────────────────
        // This checks with Google Play if there's a newer version of the app.
        // Using IMMEDIATE update type means the user MUST update before
        // continuing to use the app (a full-screen update prompt).
        // Change to AppUpdateType.FLEXIBLE for a non-blocking update banner.
        appUpdateManagerUtil = AppUpdateManagerUtil(this, binding, AppUpdateType.IMMEDIATE).apply {
            checkForUpdate()  // Start checking for updates
        }
    }

    /**
     * startMain() — Launches the Trusted Web Activity (TWA).
     *
     * This reads the website URL from twa-manifest.json and opens it
     * using Google's TwaLauncher. The website opens in Chrome but looks
     * like a native app (no browser address bar if domain verification
     * is configured — see assetlinks.json).
     */
    private fun startMain() {
        val manifest = readManifestData(this)
        // Launch the TWA with the start_url from twa-manifest.json
        // .toUri() converts the String URL to a Uri object required by TwaLauncher
        TwaLauncher(this).launch(manifest.startUrl.toUri())
    }
}
