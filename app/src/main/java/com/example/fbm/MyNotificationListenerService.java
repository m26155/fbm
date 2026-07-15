package com.example.fbm;

import android.app.Notification;
import android.content.Intent;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

public class MyNotificationListenerService extends NotificationListenerService {

    private static final String TAG = "MyNotificationListener";

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        String packageName = sbn.getPackageName();

        // Filter notifications to only process Gmail and Line
        if (!"com.google.android.gm".equals(packageName) && !"jp.naver.line.android".equals(packageName)) {
            Log.d(TAG, "Notification Ignored: Package=" + packageName);
            return;
        }

        Bundle extras = sbn.getNotification().extras;
        CharSequence titleCharSeq = extras.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence textCharSeq = extras.getCharSequence(Notification.EXTRA_TEXT);
        
        if (textCharSeq == null) {
            textCharSeq = extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
        }

        if (textCharSeq == null) {
            textCharSeq = extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT);
        }

        if (textCharSeq == null) {
            CharSequence[] textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
            if (textLines != null && textLines.length > 0) {
                textCharSeq = textLines[textLines.length - 1];
            }
        }

        String title = titleCharSeq != null ? titleCharSeq.toString() : "No Title";
        String text = textCharSeq != null ? textCharSeq.toString() : "null";

        // Fallback for MessagingStyle
        if ("null".equals(text)) {
            // Check if there are messages in extras (Notification.EXTRA_MESSAGES is android.messages)
            // This is available from API 24+
            Object[] messages = (Object[]) extras.get(Notification.EXTRA_MESSAGES);
            if (messages != null && messages.length > 0) {
                // The last message is usually the most recent one
                Bundle lastMessage = (Bundle) messages[messages.length - 1];
                CharSequence messageText = lastMessage.getCharSequence("text");
                if (messageText != null) {
                    text = messageText.toString();
                }
            }
        }

        Log.d(TAG, "Notification Posted: Package=" + packageName + ", Title=" + title + ", Text=" + text);

        Intent intent = new Intent("com.example.fbm.NOTIFICATION_LISTENER_EXAMPLE");
        intent.putExtra("notification_event", "Posted: " + packageName + "\n" + title + ": " + text);
        sendBroadcast(intent);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        String packageName = sbn.getPackageName();
        if ("com.google.android.gm".equals(packageName) || "jp.naver.line.android".equals(packageName)) {
            Log.d(TAG, "Notification Removed: Package=" + packageName);
        }
    }
}
