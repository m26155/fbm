package com.example.fbm;

import android.content.Intent;
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

        String title = sbn.getNotification().extras.getString("android.title");
        String text = sbn.getNotification().extras.getString("android.text");

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
