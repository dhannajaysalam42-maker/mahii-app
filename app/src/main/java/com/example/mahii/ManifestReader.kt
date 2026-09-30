/*
 * =====================================================================
 * ManifestReader.kt
 * =====================================================================
 * PURPOSE:
 *   Reads the TWA configuration from "assets/twa-manifest.json".
 *   This file contains the app name, description, and website URL
 *   that the TWA will open.
 *
 * HOW IT WORKS:
 *   1. Opens the file "twa-manifest.json" from the Android assets folder.
 *   2. Reads the entire JSON content as a string.
 *   3. Parses the JSON to extract: name, description, start_url.
 *   4. Returns a ManifestData object with these values.
 *
 * IMPORTANT:
 *   When you want to change the app name, description, or website URL,
 *   you should edit the file: app/src/main/assets/twa-manifest.json
 *   This function will automatically pick up the new values.
 *
 * USED BY:
 *   - IntroActivity.kt → to populate the onboarding screen text
 *   - IntroActivity.kt → to get the website URL for TwaLauncher
 * =====================================================================
 */

package com.example.mahii

// --- Android Framework Imports ---
import android.content.Context    // Provides access to app resources (assets, files, etc.)

// --- JSON Parsing Import ---
import org.json.JSONObject        // Android's built-in JSON parser

// --- Java I/O Import ---
import java.io.BufferedReader     // Efficient reading of character streams

/**
 * ManifestData — A data class that holds the parsed values from twa-manifest.json.
 *
 * @param name        The full app name (e.g., "Virtuala FansOnly")
 * @param description A short description of the app
 * @param startUrl    The website URL that the TWA will open (e.g., "https://virtuala.site/")
 *
 * TO CUSTOMIZE: Edit the values in assets/twa-manifest.json
 */
data class ManifestData(
    val name: String,         // Maps to "name" field in twa-manifest.json
    val description: String,  // Maps to "description" field in twa-manifest.json
    val startUrl: String      // Maps to "start_url" field in twa-manifest.json
)

/**
 * readManifestData() — Reads and parses the twa-manifest.json file.
 *
 * @param context The Android Context, needed to access the assets folder.
 * @return ManifestData object containing the app name, description, and start URL.
 *
 * Example twa-manifest.json content:
 * {
 *   "name": "My App Name",
 *   "short_name": "My App",
 *   "description": "Your app description here",
 *   "start_url": "https://yourwebsite.com/",
 *   "display": "fullscreen",
 *   "orientation": "portrait"
 * }
 */
fun readManifestData(context: Context): ManifestData {
    // Step 1: Open the twa-manifest.json file from the assets folder
    // The assets folder is located at: app/src/main/assets/
    val inputStream = context.assets.open("twa-manifest.json")

    // Step 2: Read the entire file content as a single String
    // bufferedReader() wraps the stream for efficient reading
    // .use() ensures the reader is properly closed after reading
    val json = inputStream.bufferedReader().use(BufferedReader::readText)

    // Step 3: Parse the JSON string into a JSONObject
    val obj = JSONObject(json)

    // Step 4: Extract the required fields and return as ManifestData
    return ManifestData(
        name = obj.getString("name"),             // Get the "name" field
        description = obj.getString("description"), // Get the "description" field
        startUrl = obj.getString("start_url")       // Get the "start_url" field
    )
}
