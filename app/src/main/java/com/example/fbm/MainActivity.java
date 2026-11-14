package com.example.fbm;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.activity.EdgeToEdge;

import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;

import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.navigation.fragment.NavHostFragment;

import com.example.fbm.databinding.ActivityMainBinding;

import android.view.Menu;
import android.view.MenuItem;
import android.net.Uri;
import android.os.Build;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.content.pm.PackageManager;
import android.Manifest;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private NotificationReceiver notificationReceiver;
    public static final String SCAM_CHANNEL_ID = "scam_alerts";

    class NotificationReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String temp = intent.getStringExtra("notification_event");
            if (temp != null) {
                if (temp.startsWith("Error:")) {
                    Snackbar.make(binding.getRoot(), temp, Snackbar.LENGTH_INDEFINITE)
                            .setAction("Retry", v -> {
                                // Potentially add retry logic here if needed, but for now just dismiss
                            })
                            .show();
                } else {
                    Snackbar.make(binding.getRoot(), temp, Snackbar.LENGTH_LONG).show();
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        createNotificationChannel();
        requestNotificationPermission();

        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        setSupportActionBar(binding.toolbar);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_content_main);

        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();

            appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
            NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        }

        if (!isNotificationServiceEnabled()) {
            Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
            startActivity(intent);
        }

        notificationReceiver = new NotificationReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.example.fbm.NOTIFICATION_LISTENER_EXAMPLE");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(notificationReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(notificationReceiver, filter);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "詐欺警告アラート";
            String description = "AIがフィッシング詐欺と判断した際に警告を表示します";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(SCAM_CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 500, 200, 500});

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, 
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(notificationReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_test_send) {
            performTestSend();
            return true;
        } else if (id == R.id.action_demo_scam_alert) {
            showDemoScamAlert();
            return true;
        } else if (id == R.id.action_github) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/m26155/fbm"));
            startActivity(browserIntent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void performTestSend() {
        android.content.SharedPreferences prefs = getSharedPreferences("FBM_Prefs", Context.MODE_PRIVATE);
        String ip = prefs.getString("rpi_ip", "").trim();
        String port = prefs.getString("rpi_port", "5000").trim();

        if (ip.isEmpty()) {
            android.widget.Toast.makeText(this, "RPiのIPアドレスが設定されていません", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        String url = "http://" + ip + ":" + (port.isEmpty() ? "5000" : port) + "/notify";
        android.widget.Toast.makeText(this, "ラズパイへテスト送信中...", android.widget.Toast.LENGTH_SHORT).show();

        NetworkClient.sendNotification(url, "com.google.android.gm", "テスト通知タイトル", "これはラズパイとの通信テストメッセージです", new NetworkClient.NetworkCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() ->
                        android.widget.Toast.makeText(MainActivity.this, "送信成功！レスポンス: " + response, android.widget.Toast.LENGTH_LONG).show()
                );
            }

            @Override
            public void onFailure(Exception e) {
                String err = e.getClass().getSimpleName() + ": " + e.getMessage();
                runOnUiThread(() ->
                        android.widget.Toast.makeText(MainActivity.this, "送信失敗: " + err, android.widget.Toast.LENGTH_LONG).show()
                );
            }
        });
    }

    private void showDemoScamAlert() {
        Intent openAppIntent = new Intent(this, MainActivity.class);
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        android.app.PendingIntent pendingIntent = android.app.PendingIntent.getActivity(
                this, (int) System.currentTimeMillis(), openAppIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE);

        androidx.core.app.NotificationCompat.Builder builder = new androidx.core.app.NotificationCompat.Builder(this, SCAM_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("⚠️ 【デモ】フィッシング詐欺の疑い")
                .setContentText("AIが危険と判定した通知のアラートデモです")
                .setStyle(new androidx.core.app.NotificationCompat.BigTextStyle()
                        .bigText("【デモ警告通知】\nAIがフィッシング詐欺の可能性を検出しました。\n\n差出人: security-update@example.com\n件名: 【重要】アカウント凍結のお知らせ\n本文: 本人確認を行ってください。"))
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_MAX)
                .setCategory(androidx.core.app.NotificationCompat.CATEGORY_ALARM)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        androidx.core.app.NotificationManagerCompat notificationManager = androidx.core.app.NotificationManagerCompat.from(this);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
            android.widget.Toast.makeText(this, "デモ警告通知を発行しました", android.widget.Toast.LENGTH_SHORT).show();
        } else {
            android.widget.Toast.makeText(this, "通知権限が許可されていません", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_content_main);
        NavController navController = navHostFragment.getNavController();
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    private boolean isNotificationServiceEnabled() {
        String pkgName = getPackageName();
        final String flat = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        if (!TextUtils.isEmpty(flat)) {
            final String[] names = flat.split(":");
            for (int i = 0; i < names.length; i++) {
                final ComponentName cn = ComponentName.unflattenFromString(names[i]);
                if (cn != null) {
                    if (TextUtils.equals(pkgName, cn.getPackageName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}