package com.example.fbm;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.content.ContextCompat;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

public class MyNotificationListenerService extends NotificationListenerService {

    private static final String TAG = "MyNotificationListener";
    private static final String PREFS_NAME = "FBM_Prefs";
    private static final String KEY_RPI_IP = "rpi_ip";
    private static final String KEY_RPI_PORT = "rpi_port";

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        String packageName = sbn.getPackageName();
        Log.d(TAG, "Notification received from: " + packageName);

        // Filter notifications to only process Gmail and Line
        if (!"com.google.android.gm".equals(packageName) && !"jp.naver.line.android".equals(packageName)) {
            Log.d(TAG, "Notification Ignored (Not Gmail/Line): Package=" + packageName);
            return;
        }

        Bundle extras = sbn.getNotification().extras;
        CharSequence titleCharSeq = extras.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence textCharSeq = extras.getCharSequence(Notification.EXTRA_TEXT);
        CharSequence bigTextCharSeq = extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
        CharSequence summaryTextCharSeq = extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT);
        CharSequence[] textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);

        String title = titleCharSeq != null ? titleCharSeq.toString() : "No Title";
        String mainText = textCharSeq != null ? textCharSeq.toString() : "";
        String bigText = bigTextCharSeq != null ? bigTextCharSeq.toString() : "";

        // Collect lines if present (InboxStyle)
        String linesText = "";
        if (textLines != null && textLines.length > 0) {
            StringBuilder sb = new StringBuilder();
            for (CharSequence line : textLines) {
                if (line != null && line.length() > 0) {
                    if (sb.length() > 0) {
                        sb.append("\n");
                    }
                    sb.append(line);
                }
            }
            linesText = sb.toString();
        }

        // Collect messages if present (MessagingStyle)
        String messagesText = "";
        Object[] messages = (Object[]) extras.get(Notification.EXTRA_MESSAGES);
        if (messages != null && messages.length > 0) {
            StringBuilder sb = new StringBuilder();
            for (Object msgObj : messages) {
                if (msgObj instanceof Bundle) {
                    Bundle msgBundle = (Bundle) msgObj;
                    CharSequence msgText = msgBundle.getCharSequence("text");
                    CharSequence msgSender = msgBundle.getCharSequence("sender");
                    if (msgText != null && msgText.length() > 0) {
                        if (sb.length() > 0) {
                            sb.append("\n");
                        }
                        if (msgSender != null && msgSender.length() > 0) {
                            sb.append(msgSender).append(": ");
                        }
                        sb.append(msgText);
                    }
                }
            }
            messagesText = sb.toString();
        }

        String text = "";

        // Combine subject and body/bigText/lines
        if (!bigText.isEmpty()) {
            if (mainText.isEmpty()) {
                text = bigText;
            } else if (bigText.equalsIgnoreCase(mainText)) {
                text = bigText;
            } else if (bigText.startsWith(mainText)) {
                text = bigText;
            } else {
                text = "件名: " + mainText + "\n本文: " + bigText;
            }
        } else if (!linesText.isEmpty()) {
            if (!mainText.isEmpty() && !linesText.contains(mainText)) {
                text = "件名: " + mainText + "\n" + linesText;
            } else {
                text = linesText;
            }
        } else if (!messagesText.isEmpty()) {
            if (!mainText.isEmpty() && !messagesText.contains(mainText)) {
                text = mainText + "\n" + messagesText;
            } else {
                text = messagesText;
            }
        } else if (!mainText.isEmpty()) {
            text = mainText;
        } else if (summaryTextCharSeq != null && summaryTextCharSeq.length() > 0) {
            text = summaryTextCharSeq.toString();
        } else {
            text = "null";
        }

        Log.d(TAG, "Notification Posted: Package=" + packageName + ", Title=" + title + ", Text=" + text);

        // Send to RPi
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String rpiIp = prefs.getString(KEY_RPI_IP, "").trim();
        String rpiPort = prefs.getString(KEY_RPI_PORT, "5000").trim();

        if (!rpiIp.isEmpty()) {
            checkNetworkStatus();
            String rpiUrl = "http://" + rpiIp + ":" + rpiPort + "/notify";
            NetworkClient.sendNotification(rpiUrl, packageName, title, text, new NetworkClient.NetworkCallback() {
                @Override
                public void onSuccess(String response) {
                    Log.d(TAG, "Successfully sent notification to RPi. Response: " + response);
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "Network request failed", e);
                    Intent intent = new Intent("com.example.fbm.NOTIFICATION_LISTENER_EXAMPLE");
                    String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                    intent.putExtra("notification_event", "Error: " + errorMsg);
                    sendBroadcast(intent);
                }
            });
        } else {
            Log.d(TAG, "RPi IP not configured, skipping send");
        }

        Intent intent = new Intent("com.example.fbm.NOTIFICATION_LISTENER_EXAMPLE");
        intent.putExtra("notification_event", "Posted: " + packageName + "\n" + title + ": " + text);
        sendBroadcast(intent);
    }

    private void showScamAlert(String originalTitle, String originalText) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, MainActivity.SCAM_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("⚠️ フィッシング詐欺の疑い")
                .setContentText("AIがこの通知を危険と判断しました: " + originalTitle)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("AIがフィッシング詐欺の疑いを検出しました。\n\n元の内容:\n" + originalText))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
        } else {
            Log.w(TAG, "Notification permission not granted, cannot show scam alert");
        }
    }

    private void checkNetworkStatus() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            Network activeNetwork = cm.getActiveNetwork();
            if (activeNetwork != null) {
                NetworkCapabilities capabilities = cm.getNetworkCapabilities(activeNetwork);
                if (capabilities != null) {
                    boolean isWiFi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
                    boolean isCellular = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
                    boolean hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                    Log.d(TAG, "Network Status: WiFi=" + isWiFi + ", Cellular=" + isCellular + ", InternetCap=" + hasInternet);
                }
            } else {
                Log.w(TAG, "No active network detected.");
            }
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        String packageName = sbn.getPackageName();
        if ("com.google.android.gm".equals(packageName) || "jp.naver.line.android".equals(packageName)) {
            Log.d(TAG, "Notification Removed: Package=" + packageName);
        }
    }
}
