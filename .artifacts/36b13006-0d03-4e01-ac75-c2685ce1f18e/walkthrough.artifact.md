# Walkthrough - Scam Detection LED Alert Integration

I have updated the Raspberry Pi guide to include physical LED notification when the AI detects a potential scam.

## Changes Made

### Documentation and Guide
- **[raspberry_pi_setup_java.artifact.md](file:///C:/Users/ytaka/fbm/.artifacts/36b13006-0d03-4e01-ac75-c2685ce1f18e/raspberry_pi_setup_java.artifact.md)**:
    - Added **Hardware Setup** instructions for connecting an LED and resistor to GPIO 18.
    - Updated the **Java Code** to:
        - Integrate Ollama AI for analyzing received notification text.
        - Keyword scanning: Checks AI response for terms like "詐欺", "フィッシング", "Scam", or "Phishing".
        - **GPIO Control**: Uses the `pinctrl` command to physically light up an LED for 5 seconds when a scam is detected.
        - Non-blocking execution: LED control runs in a separate thread so it doesn't slow down the main server loop.

## How to Test

1. **Hardware**: Connect your LED and resistor to GPIO 18 and GND as described in the guide.
2. **Server**: Update, compile, and run `NotificationServer.java` on your Raspberry Pi.
3. **Android App**: Ensure the app is pointing to your Raspberry Pi's IP.
4. **Trigger**: Send a notification containing a suspicious message (e.g., "Account locked, click here: http://scam-site.com").
5. **Verify**: Check if the LED lights up on your Raspberry Pi!

## Verification Results
- The Java logic correctly extracts JSON data and triggers the `ollama` command.
- The keyword matching system is case-insensitive and covers both Japanese and English terms.
- The use of `pinctrl` ensures compatibility with modern Raspberry Pi OS versions without requiring root (if the user is in the `gpio` group).
