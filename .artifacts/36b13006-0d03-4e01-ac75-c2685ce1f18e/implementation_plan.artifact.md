# Implementation Plan - Debug and Improve AI Response Display

Fix the issue where Ollama's response is not appearing on the Raspberry Pi console and improve the robustness of the server.

## User Review Required

> [!IMPORTANT]
> I am updating the Java server code to be more robust. If you still don't see the output after this update, please check if `ollama run yutayuma-ai "test"` works manually in your Raspberry Pi terminal.

## Proposed Changes

### [Documentation Artifact]

#### [MODIFY] [raspberry_pi_setup_java.artifact.md](file:///C:/Users/ytaka/fbm/.artifacts/36b13006-0d03-4e01-ac75-c2685ce1f18e/raspberry_pi_setup_java.artifact.md)
- **Improve Logging**: Add printing of the raw JSON body to confirm the server is receiving data from the Android app.
- **Robust JSON Parsing**: Update the `extractValue` method to handle escaped characters (like `\"` or `\n`) which often appear in notification text.
- **Clean AI Output**: Add logic to strip ANSI escape codes (spinners, progress bars) from Ollama's output for a cleaner terminal display.
- **Asynchronous Processing**: Move the Ollama execution to a separate thread. This ensures the Android app receives a "Success" response immediately, while the AI processes the message in the background.

## Verification Plan

### Manual Verification
1.  **Receipt Check**: Verify that "--- Raw JSON Received ---" appears in the terminal when a notification is sent.
2.  **Extraction Check**: Verify that "Title" and "Text" are correctly parsed and printed.
3.  **AI Execution Check**: Verify that `[Ollama AI 処理開始]` appears and is followed by the model's response.
4.  **Keyword/LED Check**: Verify that if the AI identifies a scam, the LED still triggers as expected.
