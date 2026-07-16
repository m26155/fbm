# Walkthrough - Separate IP and Port for Raspberry Pi

I have modified the application to allow separate input for the Raspberry Pi's IP address and Port number.

## Changes Made

### UI and Resources
- Updated [strings.xml](file:///C:/Users/ytaka/fbm/app/src/main/res/values/strings.xml) with descriptive hints for IP and Port.
- Redesigned [fragment_first.xml](file:///C:/Users/ytaka/fbm/app/src/main/res/layout/fragment_first.xml) to replace the single URL field with two dedicated input fields for IP and Port.

### Logic and Persistence
- Modified [FirstFragment.java](file:///C:/Users/ytaka/fbm/app/src/main/java/com/example/fbm/FirstFragment.java) to:
    - Load separate IP and Port values from `SharedPreferences`.
    - Provide a default port of `5000`.
    - Save both values independently when the "Save Configuration" button is clicked.
- Updated [MyNotificationListenerService.java](file:///C:/Users/ytaka/fbm/app/src/main/java/com/example/fbm/MyNotificationListenerService.java) to:
    - Fetch both the IP and Port from settings.
    - Dynamically construct the full URL as `http://<IP>:<PORT>/notify`.

## Verification Results

### Automated Tests
- Ran `gradle assembleDebug` and the build passed successfully.

### Manual Verification Recommended
- Please open the app and verify the new input fields.
- Enter your Raspberry Pi's IP (e.g., `192.168.1.10`) and Port (e.g., `5000`).
- Save the settings and verify that they persist across app restarts.
- Send a test notification (via Gmail or Line) and check your Raspberry Pi logs to ensure it receives the request at `http://<IP>:<PORT>/notify`.
