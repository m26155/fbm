# Walkthrough - AI Response Visibility Fix

I have improved the Raspberry Pi Java server to ensure AI responses are correctly captured and displayed in the terminal.

## Key Improvements

### Robust Output Capture
- **ANSI Stripping**: Added logic to remove terminal formatting codes (like `\x1B[m`) that Ollama sometimes emits. These codes can hide text in some terminal environments or make it look like garbage.
- **Error Stream Merging**: Merged the error output of the Ollama process into the standard output. This ensures that if the AI fails to start or encounters an issue, the error message is visible in your Java console.

### Enhanced Debugging
- **Raw JSON Logging**: The server now prints `[Raw JSON Received]` as soon as data arrives from the Android app. This helps confirm that the connection itself is working.
- **Detailed Parsing**: Prints the "Parsed Title" and "Parsed Text" to verify that the message content is being correctly extracted before being sent to the AI.

### Performance & Stability
- **Asynchronous Execution**: The AI analysis now runs in a separate thread. This means the Android app receives a "success" signal immediately, preventing timeouts on the phone while the AI is thinking on the Raspberry Pi.
- **Improved Regex**: Updated the JSON extraction to handle special characters and escaped quotes more reliably.

## Verification
- To verify the fix, please update and re-compile `NotificationServer.java` on your Raspberry Pi.
- When you send a notification, you should see the raw data, followed by `[Ollama AI 処理開始]`, and then the step-by-step output from the AI.
