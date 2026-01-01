package com.example.chiokojakharjkardam.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.chiokojakharjkardam.data.database.AppDatabase;
import com.example.chiokojakharjkardam.data.database.entity.Bill;

import java.util.List;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            // ایجاد کانال اعلان
            NotificationHelper.createNotificationChannel(context);

            // زمان‌بندی مجدد همه یادآوری‌ها
            AppDatabase.databaseWriteExecutor.execute(() -> {
                AppDatabase db = AppDatabase.getDatabase(context);
                List<Bill> unpaidBills = db.billDao().getUnpaidBillsSync();

                for (Bill bill : unpaidBills) {
                    BillReminderScheduler.scheduleReminder(context, bill);
                }
            });
        }
    }
}

