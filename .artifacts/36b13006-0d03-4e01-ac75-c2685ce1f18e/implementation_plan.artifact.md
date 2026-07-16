# Implementation Plan - Fix "Run" issues and Network Connectivity

Address the issue where the app cannot be "run" from the IDE and fix runtime network errors.

## User Review Required

> [!IMPORTANT]
> I am lowering the `minSdk` from 36 to 26 and `compileSdk`/`targetSdk` from 36 to 35.
> API 36 is a future/preview version, and `minSdk = 36` prevents the app from running on any current stable Android device (like your Pixel 8a which likely runs Android 14 or 15).

> [!NOTE]
> I am also enabling cleartext traffic (HTTP) in the manifest so the app can talk to your Raspberry Pi without SSL errors.

## Proposed Changes

### [Build Configuration]

#### [MODIFY] [build.gradle.kts](file:///C:/Users/ytaka/fbm/app/build.gradle.kts)
- Simplify `compileSdk` to `35`.
- Change `minSdk` to `26`.
- Change `targetSdk` to `35`.
- Use `libs.okhttp` instead of hardcoded string.

### [Manifest]

#### [MODIFY] [AndroidManifest.xml](file:///C:/Users/ytaka/fbm/app/src/main/AndroidManifest.xml)
- Add `android:usesCleartextTraffic="true"` to the `<application>` tag.

## Verification Plan

### Automated Tests
- Run `gradle assembleDebug` to ensure it still builds.
- Use `deploy` tool to verify it can be launched.

### Manual Verification
- Verify that the "Run" button in Android Studio is now enabled and works.
- Verify that notifications are sent to the Raspberry Pi without "CLEARTEXT communication not permitted" errors in logcat.
