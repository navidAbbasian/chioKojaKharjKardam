package com.example.chiokojakharjkardam.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BillReminderReceiver extends BroadcastReceiver {

    public static final String EXTRA_BILL_ID = "bill_id";
    public static final String EXTRA_BILL_TITLE = "bill_title";
    public static final String EXTRA_BILL_AMOUNT = "bill_amount";
    public static final String EXTRA_DAYS_LEFT = "days_left";

    @Override
    public void onReceive(Context context, Intent intent) {
        long billId = intent.getLongExtra(EXTRA_BILL_ID, -1);
        String title = intent.getStringExtra(EXTRA_BILL_TITLE);
        String amount = intent.getStringExtra(EXTRA_BILL_AMOUNT);
        int daysLeft = intent.getIntExtra(EXTRA_DAYS_LEFT, 0);

        if (billId != -1 && title != null) {
            NotificationHelper.showBillReminderNotification(context, billId, title, amount, daysLeft);
        }
    }
}

