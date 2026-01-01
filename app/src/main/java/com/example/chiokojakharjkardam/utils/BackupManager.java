package com.example.chiokojakharjkardam.utils;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;

import com.example.chiokojakharjkardam.data.database.AppDatabase;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BackupManager {

    private static final String DATABASE_NAME = "chio_koja_kharj_kardam_db";
    private static final String BACKUP_FOLDER = "ChioKojaKharjKardam_Backup";

    public interface BackupCallback {
        void onSuccess(String message);
        void onError(String error);
    }

    /**
     * ایجاد پشتیبان از دیتابیس
     */
    public static void createBackup(Context context, Uri destinationUri, BackupCallback callback) {
        try {
            // بستن کانکشن‌های دیتابیس
            AppDatabase.closeDatabase();

            // مسیر فایل دیتابیس
            File dbFile = context.getDatabasePath(DATABASE_NAME);

            if (!dbFile.exists()) {
                callback.onError("فایل دیتابیس یافت نشد");
                return;
            }

            // کپی به مقصد
            OutputStream outputStream = context.getContentResolver().openOutputStream(destinationUri);
            if (outputStream == null) {
                callback.onError("خطا در باز کردن فایل مقصد");
                return;
            }

            FileInputStream inputStream = new FileInputStream(dbFile);
            copyFile(inputStream, outputStream);

            inputStream.close();
            outputStream.close();

            callback.onSuccess("پشتیبان با موفقیت ایجاد شد");

        } catch (IOException e) {
            callback.onError("خطا در ایجاد پشتیبان: " + e.getMessage());
        }
    }

    /**
     * بازیابی دیتابیس از فایل پشتیبان
     */
    public static void restoreBackup(Context context, Uri sourceUri, BackupCallback callback) {
        try {
            // بستن کانکشن‌های دیتابیس
            AppDatabase.closeDatabase();

            // مسیر فایل دیتابیس
            File dbFile = context.getDatabasePath(DATABASE_NAME);

            // خواندن از منبع
            InputStream inputStream = context.getContentResolver().openInputStream(sourceUri);
            if (inputStream == null) {
                callback.onError("خطا در باز کردن فایل پشتیبان");
                return;
            }

            // کپی به دیتابیس
            FileOutputStream outputStream = new FileOutputStream(dbFile);
            copyFile(inputStream, outputStream);

            inputStream.close();
            outputStream.close();

            callback.onSuccess("بازیابی با موفقیت انجام شد. لطفاً برنامه را مجدداً راه‌اندازی کنید.");

        } catch (IOException e) {
            callback.onError("خطا در بازیابی: " + e.getMessage());
        }
    }

    /**
     * تولید نام پیش‌فرض برای فایل پشتیبان
     */
    public static String generateBackupFileName() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.US);
        String date = sdf.format(new Date());
        return "ChioKoja_Backup_" + date + ".db";
    }

    private static void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int length;
        while ((length = in.read(buffer)) > 0) {
            out.write(buffer, 0, length);
        }
    }
}

