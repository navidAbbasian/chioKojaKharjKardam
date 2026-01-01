package com.example.chiokojakharjkardam.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.chiokojakharjkardam.R;
import com.example.chiokojakharjkardam.ui.MainActivity;

public class NotificationHelper {

    public static final String CHANNEL_ID = "bill_reminder_channel";
    public static final String CHANNEL_NAME = "یادآوری قبوض";
    public static final int NOTIFICATION_ID_BASE = 1000;

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("یادآوری‌های سررسید قبوض و اقساط");
            channel.enableVibration(true);

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    public static void showBillReminderNotification(Context context, long billId, String title, String amount, int daysLeft) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("navigate_to", "bills");

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                (int) billId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String contentText;
        if (daysLeft == 0) {
            contentText = "امروز سررسید " + title + " به مبلغ " + amount + " است";
        } else if (daysLeft == 1) {
            contentText = "فردا سررسید " + title + " به مبلغ " + amount + " است";
        } else {
            contentText = daysLeft + " روز دیگر سررسید " + title + " به مبلغ " + amount + " است";
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_bills)
                .setContentTitle("یادآوری قبض")
                .setContentText(contentText)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(contentText));

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID_BASE + (int) billId, builder.build());
        }
    }
}

