# Walkthrough - Fixed Raspberry Pi Connection and SDK Compatibility

I have updated the application to resolve the connection issues with your Raspberry Pi and adjusted the SDK configuration for better device compatibility.

## Changes Made

### Network Security
- **[AndroidManifest.xml](file:///C:/Users/ytaka/fbm/app/src/main/AndroidManifest.xml)**: Added `android:usesCleartextTraffic="true"` to allow HTTP communication with the Raspberry Pi. This fixes the "CLEARTEXT communication not permitted" error.

### Build Configuration
- **[build.gradle.kts](file:///C:/Users/ytaka/fbm/app/build.gradle.kts)**:
    - Set `minSdk = 26` (Android 8.0) to ensure it runs on your Pixel 8a and other stable devices.
    - Set `targetSdk = 35` (Android 15) to follow current standards.
    - Set `compileSdk = 36` to satisfy dependency requirements while maintaining runtime compatibility.
- **[libs.versions.toml](file:///C:/Users/ytaka/fbm/gradle/libs.versions.toml)**: Formally added OkHttp to the version catalog for cleaner dependency management.

## Verification Results

### Automated Tests
- Ran `gradle assembleDebug` and the build passed successfully.

### Connectivity Check
- The app is now configured to allow unencrypted (HTTP) requests to your Raspberry Pi's IP address.
- Please verify by running the app and sending a test notification. The logs should no longer show "UnknownServiceException: CLEARTEXT communication not permitted".
