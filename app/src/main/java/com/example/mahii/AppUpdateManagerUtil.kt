/*
 * =====================================================================
 * AppUpdateManagerUtil.kt
 * =====================================================================
 * PURPOSE:
 *   Handles in-app update checking using Google Play Core library.
 *   When a new version of the app is published on the Play Store,
 *   this utility will detect it and prompt the user to update.
 *
 * HOW IT WORKS:
 *   1. On app launch, it checks the Play Store for available updates.
 *   2. If an update is available, it starts the update flow.
 *   3. Supports two update types:
 *      - IMMEDIATE: Full-screen prompt, user MUST update to continue.
 *      - FLEXIBLE: Background download with a Snackbar to restart.
 *   4. Uses the modern Activity Result API (no deprecated onActivityResult).
 *   5. Lifecycle-aware — automatically cleans up listeners on activity destroy.
 *   6. Uses WeakReference to prevent memory leaks.
 *
 * WHY THIS IS IMPORTANT:
 *   For an open-source TWA project, users will customize and republish
 *   this app. If they push an update, their users will be notified
 *   automatically thanks to this utility.
 *
 * USED BY:
 *   - IntroActivity.kt → initializes and calls checkForUpdate()
 * =====================================================================
 */

package com.example.mahii

// --- Android Framework Imports ---
import android.util.Log                                          // Logging utility for debug output
import androidx.activity.result.contract.ActivityResultContracts  // Modern replacement for onActivityResult
import androidx.appcompat.app.AppCompatActivity                  // Base class for activities
import androidx.core.content.ContextCompat                       // Helper for accessing color resources
import androidx.lifecycle.DefaultLifecycleObserver                // Lifecycle callback observer
import androidx.lifecycle.LifecycleOwner                         // Interface for lifecycle-aware components
import androidx.lifecycle.LiveData                                // Observable data holder (read-only)
import androidx.lifecycle.MutableLiveData                        // Observable data holder (read-write)

// --- View Binding Import ---
import com.example.mahii.databinding.ActivityIntroBinding          // Auto-generated from activity_intro.xml

// --- Google Material Design Import ---
import com.google.android.material.snackbar.Snackbar             // Shows a brief message at the bottom of the screen

// --- Google Play Core In-App Update Imports ---
import com.google.android.play.core.appupdate.AppUpdateInfo        // Contains information about an available update
import com.google.android.play.core.appupdate.AppUpdateManager     // Main class to manage in-app updates
import com.google.android.play.core.appupdate.AppUpdateManagerFactory // Factory to create AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateOptions     // Options for configuring the update flow
import com.google.android.play.core.install.model.AppUpdateType    // Enum: IMMEDIATE (1) or FLEXIBLE (0)
import com.google.android.play.core.install.model.InstallStatus    // Status of the update download
import com.google.android.play.core.install.model.UpdateAvailability // Status of update availability
import com.google.android.play.core.install.InstallStateUpdatedListener // Listener for FLEXIBLE update progress

// --- Java Utility Import ---
import java.lang.ref.WeakReference  // Prevents memory leaks by allowing garbage collection of the activity

/**
 * AppUpdateManagerUtil — Handles in-app updates using Google Play Core.
 *
 * This class is lifecycle-aware (implements DefaultLifecycleObserver) and
 * automatically cleans up resources when the activity is destroyed.
 *
 * @param activity   The activity that hosts the update flow (must be AppCompatActivity)
 * @param binding    The View Binding of the intro activity (used to display Snackbar)
 * @param updateType The type of update: AppUpdateType.IMMEDIATE or AppUpdateType.FLEXIBLE
 *
 * Usage in IntroActivity.kt:
 *   appUpdateManagerUtil = AppUpdateManagerUtil(this, binding, AppUpdateType.IMMEDIATE).apply {
 *       checkForUpdate()
 *   }
 */
class AppUpdateManagerUtil(
    activity: AppCompatActivity,
    private val binding: ActivityIntroBinding,
    private val updateType: Int // AppUpdateType.IMMEDIATE (1) or AppUpdateType.FLEXIBLE (0)
) : DefaultLifecycleObserver {

    // ─── WeakReference to the Activity ──────────────────────────────────
    // We use WeakReference to avoid holding a strong reference to the activity.
    // This prevents memory leaks — if the activity is destroyed, it can still
    // be garbage collected even if this utility is still alive.
    private val activityRef = WeakReference(activity)

    // ─── Play Core AppUpdateManager ─────────────────────────────────────
    // This is the main Google Play Core class that manages update checks
    // and update flows. It communicates with the Play Store.
    private val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(activity)

    // ─── LiveData for Update Availability ───────────────────────────────
    // LiveData is an observable data holder. Other components can observe
    // this to react when an update becomes available.
    // Initially set to false (no update available).
    private val updateAvailable = MutableLiveData<Boolean>().apply { value = false }

    // ─── Install State Listener (for FLEXIBLE updates only) ─────────────
    // This listener fires when the download status changes.
    // When the update finishes downloading, it shows a Snackbar
    // asking the user to restart the app to apply the update.
    private val installStateUpdatedListener = InstallStateUpdatedListener { state ->
        logMessage("Update State: $state")
        // If the update has been fully downloaded and we're in FLEXIBLE mode,
        // show a Snackbar prompting the user to restart
        if (state.installStatus() == InstallStatus.DOWNLOADED && updateType == AppUpdateType.FLEXIBLE) {
            showUpdateSnackbar()
        }
    }

    // ─── Activity Result Launcher (Modern API) ──────────────────────────
    // This replaces the deprecated onActivityResult() pattern.
    // It registers a callback that fires when the update flow completes,
    // whether the user accepted, denied, or the update failed.
    private val updateLauncher = activity.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        logMessage("Update Flow Result: ${result.resultCode}")
    }

    // ─── Initialization Block ───────────────────────────────────────────
    // This runs when the class is instantiated.
    init {
        // Register the install state listener only for FLEXIBLE updates
        // (IMMEDIATE updates don't need progress tracking since they're full-screen)
        if (updateType == AppUpdateType.FLEXIBLE) {
            appUpdateManager.registerListener(installStateUpdatedListener)
        }
        // Register this class as a lifecycle observer so onDestroy() is called
        // automatically when the activity is destroyed
        activity.lifecycle.addObserver(this)
    }

    /**
     * checkForUpdate() — Checks the Play Store for available updates.
     *
     * This is the main method to call. It:
     * 1. Queries the Play Store for update information
     * 2. If an update is available and allowed, starts the update flow
     * 3. Returns a LiveData<Boolean> that other components can observe
     *
     * @return LiveData<Boolean> — true if an update is available, false otherwise
     */
    fun checkForUpdate(): LiveData<Boolean> {
        // Request update info from the Play Store
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            // Check TWO conditions:
            // 1. Is there an update available?
            // 2. Is the chosen update type (IMMEDIATE/FLEXIBLE) allowed for this update?
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                    appUpdateInfo.isUpdateTypeAllowed(updateType)) {
                // Update is available — notify observers and start the update flow
                updateAvailable.value = true
                logMessage("Update Available: Version code ${appUpdateInfo.availableVersionCode()}")
                startForInAppUpdate(appUpdateInfo)
            } else {
                // No update available or update type not allowed
                updateAvailable.value = false
                logMessage("No Update Available")
            }
        }.addOnFailureListener { e ->
            // Update check failed (network error, Play Store issues, etc.)
            logMessage("Update Check Failed: ${e.message}")
        }
        return updateAvailable
    }

    /**
     * startForInAppUpdate() — Starts the actual update flow.
     *
     * For IMMEDIATE updates: Shows a full-screen update UI.
     * For FLEXIBLE updates: Downloads in the background.
     *
     * @param appUpdateInfo The update info from the Play Store
     */
    private fun startForInAppUpdate(appUpdateInfo: AppUpdateInfo?) {
        try {
            // Get the activity from the WeakReference (may be null if GC'd)
            activityRef.get()?.let { activity ->
                // Start the update flow using the modern AppUpdateOptions builder
                appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo!!,            // The update info object
                        updateLauncher,             // The Activity Result launcher
                        AppUpdateOptions.newBuilder(updateType).build()  // Options with update type
                )
            }
        } catch (e: Exception) {
            logMessage("Error Starting Update Flow: ${e.message}")
        }
    }

    /**
     * showUpdateSnackbar() — Shows a Snackbar when a FLEXIBLE update is downloaded.
     *
     * The Snackbar says "An update has just been downloaded." with a
     * "RESTART" button. Tapping RESTART applies the update and restarts the app.
     */
    private fun showUpdateSnackbar() {
        try {
            activityRef.get()?.let { activity ->
                Snackbar.make(
                        binding.parent,                         // Parent view for the Snackbar
                        "An update has just been downloaded.",   // Message text
                        Snackbar.LENGTH_INDEFINITE              // Stay visible until dismissed
                ).setAction("RESTART") {
                    // When the user taps "RESTART", complete the update
                    // This will install the update and restart the app
                    appUpdateManager.completeUpdate()
                }.apply {
                    // Style the action button text color
                    setActionTextColor(ContextCompat.getColor(activity, R.color.black))
                    show()  // Display the Snackbar
                }
            }
        } catch (e: Exception) {
            logMessage("Error Showing Snackbar: ${e.message}")
        }
    }

    /**
     * onDestroy() — Lifecycle callback, called when the activity is destroyed.
     *
     * Unregisters the install state listener to prevent memory leaks
     * and avoid callbacks firing on a destroyed activity.
     */
    override fun onDestroy(owner: LifecycleOwner) {
        // Only FLEXIBLE updates register a listener, so only unregister for FLEXIBLE
        if (updateType == AppUpdateType.FLEXIBLE) {
            appUpdateManager.unregisterListener(installStateUpdatedListener)
        }
        logMessage("Update Listener Unregistered")
    }

    /**
     * logMessage() — Helper function for consistent debug logging.
     *
     * All log messages are tagged with "AppUpdateManagerUtil" for easy
     * filtering in Logcat (Android Studio's log viewer).
     *
     * @param message The message to log
     */
    private fun logMessage(message: String) {
        Log.d("AppUpdateManagerUtil", message)
    }
}