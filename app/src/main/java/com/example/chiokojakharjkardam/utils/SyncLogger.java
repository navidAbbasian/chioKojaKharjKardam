package com.example.chiokojakharjkardam.utils;

import android.content.Context;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Logs sync operations (check status, upload, download) to a file in the app's files directory.
 * Thread-safe singleton. All log methods are safe to call from any thread.
 *
 * Log file: {filesDir}/sync_log.txt
 */
public class SyncLogger {

    private static final String TAG = "SyncLogger";
    private static final String LOG_FILE_NAME = "sync_log.txt";
    private static final long MAX_LOG_SIZE = 2 * 1024 * 1024; // 2 MB — rotate when exceeded

    private static volatile SyncLogger instance;
    private final File logFile;
    private final SimpleDateFormat dateFormat;

    private SyncLogger(Context context) {
        logFile = new File(context.getApplicationContext().getFilesDir(), LOG_FILE_NAME);
        dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
    }

    public static void init(Context context) {
        if (instance == null) {
            synchronized (SyncLogger.class) {
                if (instance == null) instance = new SyncLogger(context);
            }
        }
    }

    public static SyncLogger getInstance() {
        if (instance == null) throw new IllegalStateException("SyncLogger not initialised");
        return instance;
    }

    // ──────────────────────────────────────────────────────────────
    // Public logging API
    // ──────────────────────────────────────────────────────────────

    /** Logs the start of a sync operation. */
    public void logSyncStart(String operation) {
        write("═══ " + operation + " شروع شد ═══");
    }

    /** Logs the end of a sync operation. */
    public void logSyncEnd(String operation, boolean success) {
        write("═══ " + operation + (success ? " — موفق" : " — ناموفق") + " ═══");
    }

    /** Logs a check-status event. */
    public void logCheckStatus(String entity, String message) {
        write("[بررسی] " + entity + ": " + message);
    }

    /** Logs an upload event. */
    public void logUpload(String entity, int count, String result) {
        write("[آپلود] " + entity + " — تعداد: " + count + " — " + result);
    }

    /** Logs a download event. */
    public void logDownload(String entity, int count, String result) {
        write("[دانلود] " + entity + " — تعداد: " + count + " — " + result);
    }

    /** Logs an error. */
    public void logError(String entity, String error) {
        write("[خطا] " + entity + ": " + error);
    }

    /** Logs an informational message. */
    public void logInfo(String message) {
        write("[اطلاع] " + message);
    }

    /** Logs a warning. */
    public void logWarning(String message) {
        write("[هشدار] " + message);
    }

    /** Logs detailed sync status report. */
    public void logStatusReport(int pendingUploadCat, int pendingUploadTag,
                                 int pendingUploadCards, int pendingUploadTx,
                                 int pendingDeletes,
                                 int remoteOnlyCat, int remoteOnlyTag,
                                 int remoteOnlyCards, int remoteOnlyTx) {
        write("[وضعیت] آپلود منتظر → دسته‌بندی:" + pendingUploadCat
                + " برچسب:" + pendingUploadTag
                + " کارت:" + pendingUploadCards
                + " تراکنش:" + pendingUploadTx
                + " حذف:" + pendingDeletes);
        write("[وضعیت] فقط ابری → دسته‌بندی:" + remoteOnlyCat
                + " برچسب:" + remoteOnlyTag
                + " کارت:" + remoteOnlyCards
                + " تراکنش:" + remoteOnlyTx);
    }

    /** Returns the log file path so UI can share/export it. */
    public File getLogFile() {
        return logFile;
    }

    /** Clears the log file. */
    public void clearLog() {
        synchronized (this) {
            try {
                new FileWriter(logFile, false).close();
            } catch (IOException e) {
                Log.e(TAG, "Failed to clear log", e);
            }
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Private
    // ──────────────────────────────────────────────────────────────

    private void write(String message) {
        synchronized (this) {
            try {
                // Rotate if too large
                if (logFile.exists() && logFile.length() > MAX_LOG_SIZE) {
                    File backup = new File(logFile.getParent(), "sync_log_old.txt");
                    if (backup.exists()) backup.delete();
                    logFile.renameTo(backup);
                }

                try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
                    String timestamp = dateFormat.format(new Date());
                    writer.write("[" + timestamp + "] " + message);
                    writer.newLine();
                }
            } catch (IOException e) {
                Log.e(TAG, "Failed to write sync log: " + e.getMessage());
            }
        }
        // Also mirror to logcat
        Log.d(TAG, message);
    }
}

