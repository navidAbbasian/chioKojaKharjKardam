package com.example.chiokojakharjkardam.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.example.chiokojakharjkardam.data.database.entity.Bill;

import java.util.Calendar;

public class BillReminderScheduler {

    public static void scheduleReminder(Context context, Bill bill) {
        if (bill.isPaid()) {
            cancelReminder(context, bill);
            return;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        // محاسبه زمان یادآوری
        Calendar reminderTime = Calendar.getInstance();
        reminderTime.setTimeInMillis(bill.getDueDate());
        reminderTime.add(Calendar.DAY_OF_MONTH, -bill.getNotifyBefore());
        reminderTime.set(Calendar.HOUR_OF_DAY, 9); // ساعت 9 صبح
        reminderTime.set(Calendar.MINUTE, 0);
        reminderTime.set(Calendar.SECOND, 0);

        // اگر زمان یادآوری گذشته، یادآوری نکن
        if (reminderTime.getTimeInMillis() < System.currentTimeMillis()) {
            return;
        }

        Intent intent = new Intent(context, BillReminderReceiver.class);
        intent.putExtra(BillReminderReceiver.EXTRA_BILL_ID, bill.getId());
        intent.putExtra(BillReminderReceiver.EXTRA_BILL_TITLE, bill.getTitle());
        intent.putExtra(BillReminderReceiver.EXTRA_BILL_AMOUNT, CurrencyUtils.formatToman(bill.getAmount()));
        intent.putExtra(BillReminderReceiver.EXTRA_DAYS_LEFT, bill.getNotifyBefore());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                (int) bill.getId(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // تنظیم آلارم
        alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminderTime.getTimeInMillis(),
                pendingIntent
        );
    }

    public static void cancelReminder(Context context, Bill bill) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, BillReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                (int) bill.getId(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        alarmManager.cancel(pendingIntent);
    }
}

