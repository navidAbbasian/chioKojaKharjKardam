package com.example.chiokojakharjkardam.ui.sync;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.chiokojakharjkardam.utils.SyncLogger;
import com.example.chiokojakharjkardam.utils.SyncManager;

public class CloudSyncViewModel extends AndroidViewModel {

    public enum State { IDLE, LOADING, SUCCESS, ERROR }

    private final MutableLiveData<State> state = new MutableLiveData<>(State.IDLE);
    private final MutableLiveData<String> statusMessage = new MutableLiveData<>("");
    private final MutableLiveData<Integer> progressCurrent = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> progressTotal = new MutableLiveData<>(100);
    private final MutableLiveData<SyncManager.SyncResult> lastResult = new MutableLiveData<>(null);
    private final MutableLiveData<SyncManager.SyncStatusReport> statusReport = new MutableLiveData<>(null);
    /** Fires true when the session has expired and the user must re-login. */
    private final MutableLiveData<Boolean> needsRelogin = new MutableLiveData<>(false);

    public CloudSyncViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<State> getState() { return state; }
    public LiveData<String> getStatusMessage() { return statusMessage; }
    public LiveData<Integer> getProgressCurrent() { return progressCurrent; }
    public LiveData<Integer> getProgressTotal() { return progressTotal; }
    public LiveData<SyncManager.SyncResult> getLastResult() { return lastResult; }
    public LiveData<SyncManager.SyncStatusReport> getStatusReport() { return statusReport; }
    public LiveData<Boolean> getNeedsRelogin() { return needsRelogin; }

    public void checkStatus() {
        state.setValue(State.LOADING);
        statusMessage.setValue("در حال بررسی وضعیت همگام‌سازی...");
        progressCurrent.setValue(0);
        progressTotal.setValue(100);
        lastResult.setValue(null);
        statusReport.setValue(null);

        SyncLogger.getInstance().logSyncStart("بررسی وضعیت");

        SyncManager.getInstance().checkSyncStatus(new SyncManager.SyncStatusCallback() {
            @Override
            public void onResult(SyncManager.SyncStatusReport report) {
                SyncLogger.getInstance().logStatusReport(
                        report.pendingUploadCategories, report.pendingUploadTags,
                        report.pendingUploadCards, report.pendingUploadTransactions,
                        report.pendingDeletes,
                        report.remoteOnlyCategories, report.remoteOnlyTags,
                        report.remoteOnlyCards, report.remoteOnlyTransactions);
                SyncLogger.getInstance().logSyncEnd("بررسی وضعیت", true);
                statusReport.postValue(report);
                state.postValue(State.SUCCESS);
                statusMessage.postValue("بررسی کامل شد");
            }
            @Override
            public void onError(String msg) {
                SyncLogger.getInstance().logError("بررسی وضعیت", msg);
                SyncLogger.getInstance().logSyncEnd("بررسی وضعیت", false);
                SyncManager.getInstance().checkLocalPendingStatus(localReport -> {
                    statusReport.postValue(localReport);
                    state.postValue(State.ERROR);
                    statusMessage.postValue(msg);
                });
            }
        });
    }

    public void startUpload() {
        state.setValue(State.LOADING);
        statusMessage.setValue("در حال آماده‌سازی آپلود...");
        progressCurrent.setValue(0);
        progressTotal.setValue(100);
        lastResult.setValue(null);
        statusReport.setValue(null);

        SyncLogger.getInstance().logSyncStart("آپلود");

        SyncManager.getInstance().uploadWithProgress(
            (current, total, message) -> {
                progressCurrent.postValue(current);
                progressTotal.postValue(total);
                statusMessage.postValue(message);
                SyncLogger.getInstance().logInfo("آپلود پیشرفت: " + current + "/" + total + " — " + message);
            },
            result -> {
                lastResult.postValue(result);
                if (result.success) {
                    SyncLogger.getInstance().logUpload("کل", result.totalUploaded(), "موفق");
                    SyncLogger.getInstance().logSyncEnd("آپلود", true);
                    state.postValue(State.SUCCESS);
                    if (result.alreadySynced) {
                        statusMessage.postValue("اطلاعات از قبل همگام بودند — بررسی مجدد وضعیت...");
                    } else {
                        statusMessage.postValue("آپلود با موفقیت انجام شد");
                    }
                    refreshStatusAfterSync();
                } else {
                    SyncLogger.getInstance().logError("آپلود", result.errorMessage != null ? result.errorMessage : "خطای نامشخص");
                    SyncLogger.getInstance().logSyncEnd("آپلود", false);
                    state.postValue(State.ERROR);
                    statusMessage.postValue(result.errorMessage != null ? result.errorMessage : "خطا در آپلود");
                    if (result.needsRelogin) {
                        needsRelogin.postValue(true);
                    }
                }
            }
        );
    }

    public void startDownload() {
        state.setValue(State.LOADING);
        statusMessage.setValue("در حال اتصال به سرور...");
        progressCurrent.setValue(0);
        progressTotal.setValue(7);
        lastResult.setValue(null);
        statusReport.setValue(null);

        SyncLogger.getInstance().logSyncStart("دانلود");

        SyncManager.getInstance().downloadWithProgress(
            (current, total, message) -> {
                progressCurrent.postValue(current);
                progressTotal.postValue(total);
                statusMessage.postValue(message);
                SyncLogger.getInstance().logInfo("دانلود پیشرفت: " + current + "/" + total + " — " + message);
            },
            result -> {
                lastResult.postValue(result);
                if (result.success) {
                    SyncLogger.getInstance().logDownload("کل", result.totalDownloaded(), "موفق");
                    SyncLogger.getInstance().logSyncEnd("دانلود", true);
                    state.postValue(State.SUCCESS);
                    statusMessage.postValue("بارگیری با موفقیت انجام شد — بررسی وضعیت نهایی...");
                    refreshStatusAfterSync();
                } else {
                    SyncLogger.getInstance().logError("دانلود", result.errorMessage != null ? result.errorMessage : "خطای نامشخص");
                    SyncLogger.getInstance().logSyncEnd("دانلود", false);
                    state.postValue(State.ERROR);
                    statusMessage.postValue(result.errorMessage != null ? result.errorMessage : "خطا در بارگیری");
                }
            }
        );
    }

    /** Re-checks local pending counts after an operation to update the status panel. */
    private void refreshStatusAfterSync() {
        SyncManager.getInstance().checkSyncStatus(new SyncManager.SyncStatusCallback() {
            @Override
            public void onResult(SyncManager.SyncStatusReport report) {
                statusReport.postValue(report);
            }
            @Override
            public void onError(String msg) { /* silent — not critical */ }
        });
    }

    public void reset() {
        state.setValue(State.IDLE);
        statusMessage.setValue("");
        progressCurrent.setValue(0);
        progressTotal.setValue(100);
        lastResult.setValue(null);
        statusReport.setValue(null);
    }
}
