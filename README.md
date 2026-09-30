# Mahii

Generated Android Trusted Web Activity (TWA) project for **Mahii**.

![App icon](app/src/main/ic_launcher-playstore.png)

## Project Summary

| Field | Value |
| --- | --- |
| App name | Mahii |
| Short name | Mahi |
| Package ID | `com.example.mahii` |
| Website URL | https://mahii-voice-flow.base44.app/ |
| Verified host | mahii-voice-flow.base44.app |
| Description | Smart AI assistant for voice commands, chat and study. |
| Generated at | 2026-09-30 15:06:00 UTC |

## What This Project Contains

- Native Android wrapper for your website using Trusted Web Activity.
- App launcher icons generated from the uploaded logo.
- Manifest, strings, package ID, and TWA config updated from the builder form.
- GitHub Actions workflow to build signed release APK and Play Store AAB artifacts.
- Default keystore for immediate test builds.
- Optional production keystore support through GitHub Actions secrets.

## Download APK and AAB From GitHub Actions

1. Open this repository on GitHub.
2. Go to the **Actions** tab.
3. Open the latest **Build Android TWA (APK & AAB)** workflow run.
4. Download the artifacts:
   - `TWA-Release-APK` for direct APK testing.
   - `TWA-Release-AAB` for Google Play Console upload.

## Signing Details

This project can build immediately with the included default keystore:

| Setting | Value |
| --- | --- |
| Keystore file | `app/keystore/keystore.jks` |
| Store password | `androidpassword` |
| Key alias | `myalias` |
| Key password | `androidpassword` |

For production Play Store releases, use your own private keystore by adding these GitHub repository secrets:

- `JKS_BASE64`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

The GitHub workflow automatically uses those secrets when they are present. If they are missing, it falls back to the default keystore so users can still generate APK and AAB files.

## Domain Verification

The generated project includes an `assetlinks.json` file for the default bundled keystore:

- `assetlinks.json`
- `.well-known/assetlinks.json`

To hide the browser toolbar and complete TWA verification, upload that file to your website:

```text
https://mahii-voice-flow.base44.app/.well-known/assetlinks.json
```

Default SHA-256 fingerprint:

```text
8B:C3:6C:62:6F:DC:28:A7:4A:95:E6:A6:C0:8E:17:AB:F8:84:CD:EE:CC:FD:79:45:57:4C:73:75:78:0F:D8:BF
```

If you use Google Play App Signing or your own production keystore, replace the fingerprint in `assetlinks.json` with the SHA-256 fingerprint from Google Play Console before uploading it to your website.

## Local Build Commands

```bash
./gradlew assembleRelease
./gradlew bundleRelease
```

Build outputs:

- APK: `app/build/outputs/apk/release/app-release.apk`
- AAB: `app/build/outputs/bundle/release/app-release.aab`
