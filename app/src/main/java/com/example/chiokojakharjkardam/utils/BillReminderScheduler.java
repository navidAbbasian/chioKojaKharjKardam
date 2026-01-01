package com.example.chiokojakharjkardam.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.example.chiokojakharjkardam.data.database.entity.Bill;

import java.util.Calendar;

public class BillReminderScheduler {

    public static void scheduleReminder(Context context, Bill bill) {
        if (bill == null) return;

        // اگر یادآوری غیرفعال است، فقط لغو کن
        if (bill.getNotifyBefore() <= 0) {
            cancelReminder(context, bill);
            return;
        }

        if (bill.isPaid()) {
            cancelReminder(context, bill);
            return;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        // بررسی permission برای Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                // اگر permission ندارد، از آلارم غیردقیق استفاده کن
                scheduleInexactReminder(context, bill, alarmManager);
                return;
            }
        }

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

        PendingIntent pendingIntent = createPendingIntent(context, bill);

        // تنظیم آلارم
        try {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminderTime.getTimeInMillis(),
                    pendingIntent
            );
        } catch (SecurityException e) {
            // Fallback به آلارم غیردقیق
            scheduleInexactReminder(context, bill, alarmManager);
        }
    }

    private static void scheduleInexactReminder(Context context, Bill bill, AlarmManager alarmManager) {
        Calendar reminderTime = Calendar.getInstance();
        reminderTime.setTimeInMillis(bill.getDueDate());
        reminderTime.add(Calendar.DAY_OF_MONTH, -bill.getNotifyBefore());
        reminderTime.set(Calendar.HOUR_OF_DAY, 9);
        reminderTime.set(Calendar.MINUTE, 0);
        reminderTime.set(Calendar.SECOND, 0);

        if (reminderTime.getTimeInMillis() < System.currentTimeMillis()) {
            return;
        }

        PendingIntent pendingIntent = createPendingIntent(context, bill);

        // استفاده از آلارم غیردقیق
        alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                reminderTime.getTimeInMillis(),
                pendingIntent
        );
    }

    private static PendingIntent createPendingIntent(Context context, Bill bill) {
        Intent intent = new Intent(context, BillReminderReceiver.class);
        intent.putExtra(BillReminderReceiver.EXTRA_BILL_ID, bill.getId());
        intent.putExtra(BillReminderReceiver.EXTRA_BILL_TITLE, bill.getTitle());
        intent.putExtra(BillReminderReceiver.EXTRA_BILL_AMOUNT, CurrencyUtils.formatToman(bill.getAmount()));
        intent.putExtra(BillReminderReceiver.EXTRA_DAYS_LEFT, bill.getNotifyBefore());

        return PendingIntent.getBroadcast(
                context,
                (int) bill.getId(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    public static void cancelReminder(Context context, Bill bill) {
        if (bill == null) return;

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

