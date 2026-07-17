# Implementation Plan - Scam Detection LED Alert on Raspberry Pi

Update the Raspberry Pi Java server guide to detect if the AI response indicates a scam and trigger a physical LED alert.

## User Review Required

> [!IMPORTANT]
> This update involves physical hardware connection. You will need:
> - 1x LED (any color)
> - 1x 220-330 ohm Resistor
> - Jumper wires
>
> I will assume you are using **GPIO 18** (Physical Pin 12) for the LED.
> The guide will use the `pinctrl` command (standard on Raspberry Pi OS Bookworm) to control the LED.

## Proposed Changes

### [Documentation Artifact]

#### [MODIFY] [raspberry_pi_setup_java.artifact.md](file:///C:/Users/ytaka/fbm/.artifacts/36b13006-0d03-4e01-ac75-c2685ce1f18e/raspberry_pi_setup_java.artifact.md)
- Add a **Hardware Setup** section with a simple diagram/description of LED + Resistor wiring.
- Update the **Java Program** code to:
    - Capture the full output from the Ollama process.
    - Scan the output for keywords indicating a scam (e.g., "詐欺", "フィッシング", "Scam").
    - Call `pinctrl` to turn on the LED (GPIO 18) for 5 seconds when a scam is detected.

## Verification Plan

### Manual Verification
1.  **Hardware:** Connect the LED long leg (Anode) to GPIO 18 through a resistor, and the short leg (Cathode) to GND.
2.  **Software:**
    - Update `NotificationServer.java` on the Raspberry Pi.
    - Compile with `javac NotificationServer.java`.
    - Run with `java NotificationServer`.
    - Send a "Scam" notification (e.g., from a test SMS app or by modifying the data in your phone's memory) and verify the LED lights up.
    - Send a "Normal" notification and verify the LED remains off.
