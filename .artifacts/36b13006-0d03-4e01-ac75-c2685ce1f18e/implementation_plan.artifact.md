# Implementation Plan - Integrate Ollama AI on Raspberry Pi (Java)

Update the Raspberry Pi Java server guide to automatically trigger a local AI model (Ollama) when a notification is received.

## User Review Required

> [!IMPORTANT]
> This update assumes **Ollama** is installed and the model `yutayuma-ai` is already pulled/created on your Raspberry Pi.
> The Java code will execute shell commands directly. Ensure that the Raspberry Pi has sufficient resources (RAM/CPU) to run the AI model.

## Proposed Changes

### [Documentation Artifact]

#### [MODIFY] [raspberry_pi_setup_java.artifact.md](file:///C:/Users/ytaka/fbm/.artifacts/36b13006-0d03-4e01-ac75-c2685ce1f18e/raspberry_pi_setup_java.artifact.md)
- Add a new section for **Ollama Installation**.
- Update the **Java Program** code to:
    - Parse the incoming JSON to extract the notification message.
    - Use `ProcessBuilder` to execute `ollama run yutayuma-ai "<message>"`.
    - Print the AI's response to the console.
- Update the **Execution** steps if necessary.

## Verification Plan

### Manual Verification
1.  **Server side (Raspberry Pi):**
    - Compile the updated `NotificationServer.java`.
    - Run the server.
    - Manually send a mock POST request to `http://localhost:5000/notify` with a JSON body and verify that Ollama is triggered.
2.  **End-to-End:**
    - Trigger a notification on the Android phone.
    - Verify that the Raspberry Pi console shows the notification and then the generated AI response from Ollama.
