package com.example.fbm;

import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class NetworkClient {
    private static final String TAG = "NetworkClient";
    private static final OkHttpClient client = new OkHttpClient();
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    public interface NetworkCallback {
        void onSuccess(String response);
        void onFailure(Exception e);
    }

    public static void sendNotification(String url, String packageName, String title, String text, NetworkCallback callback) {
        if (url == null || url.isEmpty()) {
            Log.e(TAG, "URL is empty, skipping network request");
            return;
        }

        String json = "{"
                + "\"package\": \"" + escapeJson(packageName) + "\","
                + "\"title\": \"" + escapeJson(title) + "\","
                + "\"text\": \"" + escapeJson(text) + "\""
                + "}";

        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Failed to send notification to " + url, e);
                if (callback != null) callback.onFailure(e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body() != null ? response.body().string() : "";
                    Log.d(TAG, "Successfully sent notification to " + url + ". Response: " + responseData);
                    if (callback != null) callback.onSuccess(responseData);
                } else {
                    Log.e(TAG, "Failed to send notification. HTTP Code: " + response.code());
                    if (callback != null) callback.onFailure(new IOException("Unexpected code " + response));
                }
            }
        });
    }

    private static String escapeJson(String input) {
        if (input == null) return "null";
        return input.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }
}
