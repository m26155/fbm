# Implementation Plan - Fix Raspberry Pi Connection and SDK Compatibility

Address the connection failure to Raspberry Pi (Cleartext error) and adjust SDK versions for better device compatibility.

## User Review Required

> [!IMPORTANT]
> I am lowering the `minSdk` to 26 and `compileSdk`/`targetSdk` to 35.
> API 36 is a future/preview version, and `minSdk = 36` prevents the app from running on any current stable Android device.

> [!NOTE]
> I am enabling cleartext traffic (HTTP) in the manifest so the app can talk to your Raspberry Pi without SSL errors.

## Proposed Changes

### [Dependencies]

#### [MODIFY] [libs.versions.toml](file:///C:/Users/ytaka/fbm/gradle/libs.versions.toml)
- Add `okhttp` version and library definition.

### [Build Configuration]

#### [MODIFY] [build.gradle.kts](file:///C:/Users/ytaka/fbm/app/build.gradle.kts)
- Set `compileSdk` to `35`.
- Change `minSdk` to `26`.
- Change `targetSdk` to `35`.
- Use `libs.okhttp` from the version catalog.

### [Manifest]

#### [MODIFY] [AndroidManifest.xml](file:///C:/Users/ytaka/fbm/app/src/main/AndroidManifest.xml)
- Add `android:usesCleartextTraffic="true"` to the `<application>` tag.

## Verification Plan

### Automated Tests
- Run `gradle assembleDebug` to ensure it builds.

### Manual Verification
- Verify that notifications are sent to the Raspberry Pi without "CLEARTEXT communication not permitted" errors in logcat.
