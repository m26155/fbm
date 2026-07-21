# Implementation Plan - Reliability Fix for Raspberry Pi AI Response

Address the issue where AI responses from Ollama are not appearing on the Raspberry Pi server by switching from CLI-based execution to the more robust Ollama HTTP API.

## User Review Required

> [!IMPORTANT]
> This update changes how the Java server communicates with Ollama. Instead of running the `ollama` command directly, it will use the Ollama local web API (usually on port 11434).
>
> Please ensure:
> 1. The Ollama service is running on your Raspberry Pi.
> 2. You can access `http://localhost:11434` on the Pi.

## Proposed Changes

### [Documentation Artifact]

#### [MODIFY] [raspberry_pi_setup_java.artifact.md](file:///C:/Users/ytaka/fbm/.artifacts/guide/raspberry_pi_setup_java.artifact.md)
- **Update Java Program**:
    - Replace `ProcessBuilder` logic in `runOllama` with `java.net.http.HttpClient`.
    - Call the `/api/generate` endpoint with `stream: false` to get a single, clean response.
    - Add explicit timeout handling (e.g., 60 seconds) to prevent hanging if the AI is slow.
    - Add an "API Check" at startup to verify if Ollama is reachable.
- **Update Troubleshooting**:
    - Add instructions on how to test the Ollama API using `curl`.

## Verification Plan

### Manual Verification (on Raspberry Pi)
1.  **API Check**: Run `curl http://localhost:11434` on the Pi to verify Ollama is up.
2.  **Server Start**: Run the updated `NotificationServer`. It should print `Ollama API is reachable` at startup.
3.  **Test Notification**: Send a notification from the Android app.
4.  **Verify Logs**:
    - Check if `[Raw JSON Received]` appears.
    - Check if `[Ollama API リクエスト送信]` appears.
    - Check if the AI's response is printed clearly after a few seconds.
