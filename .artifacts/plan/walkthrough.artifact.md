# Walkthrough - AI Response Stability (API Integration)

I have updated the Raspberry Pi Java server to use the **Ollama HTTP API** instead of command-line execution. This ensures that AI responses are captured and displayed reliably.

## Changes Made

### Documentation and Guide
- **[raspberry_pi_setup_java.artifact.md](file:///C:/Users/ytaka/fbm/.artifacts/guide/raspberry_pi_setup_java.artifact.md)**:
    - **API Integration**: Replaced `ProcessBuilder` with `java.net.http.HttpClient`. This communicates directly with the Ollama service running on the Pi, bypassing terminal formatting issues.
    - **Stream Handling**: Set `"stream": false` in the API request. This tells Ollama to wait for the full response and send it in one piece, making it much easier for the Java server to process and print.
    - **Timeout Configuration**: Added a 60-second timeout to accommodate longer thinking times by the AI model.
    - **Escaping Fix**: Improved the prompt handling to correctly escape quotation marks, preventing JSON errors when notification text contains special characters.

## How to Test

1. **Prerequisite**: Ensure Java 11 or higher is installed on your Raspberry Pi.
2. **Setup**: Update your `NotificationServer.java` with the new code from the guide.
3. **Run**: Compile and run the server. You should see `Ollama API Target: http://localhost:11434/api/generate` in the console.
4. **Trigger**: Send a notification from your phone.
5. **Verify**: The server will log `[Ollama API リクエスト送信中...]` and should display the AI's answer within a few seconds.

## Benefits
- **No missing output**: By using the official API, we eliminate cases where terminal output is "hidden" or corrupted by ANSI codes.
- **Better performance**: HTTP communication is generally more efficient than spawning a new OS process for every notification.
- **Improved reliability**: The server now explicitly handles HTTP error codes and timeouts.
