package com.example.chiokojakharjkardam.ui.sync;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.chiokojakharjkardam.R;
import com.example.chiokojakharjkardam.ui.auth.AuthActivity;
import com.example.chiokojakharjkardam.utils.NetworkMonitor;
import com.example.chiokojakharjkardam.utils.SessionManager;
import com.example.chiokojakharjkardam.utils.SyncManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;

public class CloudSyncFragment extends Fragment {

    private CloudSyncViewModel viewModel;

    private MaterialButton btnCheckStatus;
    private MaterialButton btnUpload;
    private MaterialButton btnDownload;

    private MaterialCardView cardProgress;
    private LinearProgressIndicator progressBar;
    private TextView tvProgressTitle;
    private TextView tvProgressMessage;
    private TextView tvProgressPercent;

    private MaterialCardView cardResult;
    private ImageView ivResultIcon;
    private TextView tvResultTitle;
    private TextView tvResultDetail;

    private View cardStatusResult;
    private TextView tvStatusUploadCount;
    private TextView tvStatusDownloadCount;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cloud_sync, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(CloudSyncViewModel.class);

        initViews(view);
        setupListeners();
        observeViewModel();
    }

    private void initViews(View view) {
        btnCheckStatus = view.findViewById(R.id.btn_check_status);
        btnUpload = view.findViewById(R.id.btn_upload);
        btnDownload = view.findViewById(R.id.btn_download);

        cardProgress = view.findViewById(R.id.card_progress);
        progressBar = view.findViewById(R.id.progress_bar);
        tvProgressTitle = view.findViewById(R.id.tv_progress_title);
        tvProgressMessage = view.findViewById(R.id.tv_progress_message);
        tvProgressPercent = view.findViewById(R.id.tv_progress_percent);

        cardResult = view.findViewById(R.id.card_result);
        ivResultIcon = view.findViewById(R.id.iv_result_icon);
        tvResultTitle = view.findViewById(R.id.tv_result_title);
        tvResultDetail = view.findViewById(R.id.tv_result_detail);

        cardStatusResult = view.findViewById(R.id.layout_status_result);
        tvStatusUploadCount = view.findViewById(R.id.tv_status_upload_count);
        tvStatusDownloadCount = view.findViewById(R.id.tv_status_download_count);
    }

    private void setupListeners() {
        btnCheckStatus.setOnClickListener(v -> {
            if (!checkOnline()) return;
            viewModel.checkStatus();
        });

        btnUpload.setOnClickListener(v -> {
            if (!checkOnline()) return;
            viewModel.startUpload();
        });

        btnDownload.setOnClickListener(v -> {
            if (!checkOnline()) return;
            viewModel.startDownload();
        });
    }

    private boolean checkOnline() {
        if (!NetworkMonitor.getInstance().isOnline()) {
            showOfflineError();
            return false;
        }
        return true;
    }

    private void showOfflineError() {
        cardResult.setVisibility(View.VISIBLE);
        cardProgress.setVisibility(View.GONE);
        ivResultIcon.setImageResource(R.drawable.ic_cloud_offline);
        ivResultIcon.setColorFilter(requireContext().getColor(android.R.color.holo_red_dark));
        tvResultTitle.setText("دستگاه آفلاین است");
        tvResultTitle.setTextColor(requireContext().getColor(android.R.color.holo_red_dark));
        tvResultDetail.setText("برای همگام‌سازی با ابر، ابتدا به اینترنت متصل شوید.");
    }

    private void observeViewModel() {
        viewModel.getState().observe(getViewLifecycleOwner(), state -> {
            switch (state) {
                case IDLE:
                    setButtonsEnabled(true);
                    cardProgress.setVisibility(View.GONE);
                    break;
                case LOADING:
                    setButtonsEnabled(false);
                    cardProgress.setVisibility(View.VISIBLE);
                    cardResult.setVisibility(View.GONE);
                    cardStatusResult.setVisibility(View.GONE);
                    tvProgressTitle.setText("در حال انجام عملیات...");
                    break;
                case SUCCESS:
                    setButtonsEnabled(true);
                    cardProgress.setVisibility(View.GONE);
                    break;
                case ERROR:
                    setButtonsEnabled(true);
                    cardProgress.setVisibility(View.GONE);
                    // Show the result card with the error message
                    if (viewModel.getLastResult().getValue() == null) {
                        cardResult.setVisibility(View.VISIBLE);
                        ivResultIcon.setImageResource(R.drawable.ic_cloud_offline);
                        ivResultIcon.setColorFilter(requireContext().getColor(android.R.color.holo_red_dark));
                        tvResultTitle.setText("خطا در عملیات");
                        tvResultTitle.setTextColor(requireContext().getColor(android.R.color.holo_red_dark));
                        String errMsg = viewModel.getStatusMessage().getValue();
                        tvResultDetail.setText(errMsg != null ? errMsg : "خطای ناشناخته");
                    }
                    // Status panel (local counts) is shown via statusReport observer
                    break;
            }
        });

        viewModel.getProgressCurrent().observe(getViewLifecycleOwner(), current -> {
            Integer total = viewModel.getProgressTotal().getValue();
            if (total != null && total > 0) {
                int pct = (int) ((current * 100f) / total);
                progressBar.setProgressCompat(pct, true);
                tvProgressPercent.setText(pct + "%");
            }
        });

        viewModel.getProgressTotal().observe(getViewLifecycleOwner(), total -> {
            Integer current = viewModel.getProgressCurrent().getValue();
            if (current != null && total != null && total > 0) {
                int pct = (int) ((current * 100f) / total);
                progressBar.setProgressCompat(pct, true);
                tvProgressPercent.setText(pct + "%");
            }
        });

        viewModel.getStatusMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.isEmpty()) {
                tvProgressMessage.setText(msg);
            }
        });

        viewModel.getLastResult().observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;
            cardResult.setVisibility(View.VISIBLE);

            if (result.success) {
                ivResultIcon.setImageResource(R.drawable.ic_cloud_online);
                ivResultIcon.setColorFilter(requireContext().getColor(android.R.color.holo_green_dark));
                tvResultTitle.setTextColor(requireContext().getColor(android.R.color.holo_green_dark));

                if (result.alreadySynced) {
                    tvResultTitle.setText("اطلاعات از قبل همگام بودند ✓");
                    tvResultDetail.setText(
                        "تمام اطلاعات این دستگاه قبلاً در ابر ثبت شده بودند.\n" +
                        "احتمالاً همگام‌سازی خودکار قبل از این عملیات انجام شده بود.\n\n" +
                        "برای مشاهده وضعیت دقیق، دکمه «بررسی وضعیت» را بزنید."
                    );
                } else {
                    tvResultTitle.setText("عملیات با موفقیت انجام شد ✓");
                    tvResultDetail.setText(buildSuccessDetail(result));
                }
            } else {
                ivResultIcon.setImageResource(R.drawable.ic_cloud_offline);
                ivResultIcon.setColorFilter(requireContext().getColor(android.R.color.holo_red_dark));
                tvResultTitle.setText("خطا در انجام عملیات");
                tvResultTitle.setTextColor(requireContext().getColor(android.R.color.holo_red_dark));
                tvResultDetail.setText(result.errorMessage != null ? result.errorMessage : "خطای ناشناخته");

                if (result.needsRelogin && getView() != null) {
                    showReloginDialog();
                }
            }
        });

        viewModel.getStatusReport().observe(getViewLifecycleOwner(), report -> {
            if (report == null) return;
            cardStatusResult.setVisibility(View.VISIBLE);

            // Upload status
            StringBuilder uploadSb = new StringBuilder("📤 موارد آماده آپلود:\n");
            if (report.totalPendingUpload() == 0) {
                uploadSb.append("   همه اطلاعات آپلود شده‌اند ✓");
            } else {
                if (report.pendingUploadCategories > 0)
                    uploadSb.append("   • ").append(report.pendingUploadCategories).append(" دسته‌بندی\n");
                if (report.pendingUploadTags > 0)
                    uploadSb.append("   • ").append(report.pendingUploadTags).append(" برچسب\n");
                if (report.pendingUploadCards > 0)
                    uploadSb.append("   • ").append(report.pendingUploadCards).append(" کارت بانکی\n");
                if (report.pendingUploadTransactions > 0)
                    uploadSb.append("   • ").append(report.pendingUploadTransactions).append(" تراکنش\n");
                if (report.pendingDeletes > 0)
                    uploadSb.append("   • ").append(report.pendingDeletes).append(" مورد حذف‌شده");
            }
            tvStatusUploadCount.setText(uploadSb.toString().trim());

            // Download status
            StringBuilder downloadSb = new StringBuilder("📥 موارد آماده دانلود:\n");
            if (report.totalPendingDownload() == 0) {
                downloadSb.append("   همه اطلاعات ابر در دستگاه موجود است ✓");
            } else {
                if (report.remoteOnlyCategories > 0)
                    downloadSb.append("   • ").append(report.remoteOnlyCategories).append(" دسته‌بندی\n");
                if (report.remoteOnlyTags > 0)
                    downloadSb.append("   • ").append(report.remoteOnlyTags).append(" برچسب\n");
                if (report.remoteOnlyCards > 0)
                    downloadSb.append("   • ").append(report.remoteOnlyCards).append(" کارت بانکی\n");
                if (report.remoteOnlyTransactions > 0)
                    downloadSb.append("   • ").append(report.remoteOnlyTransactions).append(" تراکنش");
            }
            tvStatusDownloadCount.setText(downloadSb.toString().trim());
        });
    }

    private void showReloginDialog() {
        if (getContext() == null) return;
        new AlertDialog.Builder(requireContext())
                .setTitle("نشست منقضی شده")
                .setMessage("نشست شما در سرور منقضی شده است.\nبرای ادامه همگام‌سازی باید مجدداً وارد شوید.\n\nاطلاعات محلی شما حفظ خواهد شد.")
                .setPositiveButton("ورود مجدد", (d, w) -> {
                    SessionManager.getInstance().clearNeedsReauth();
                    Intent intent = new Intent(requireContext(), AuthActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .setNegativeButton("بعداً", (d, w) -> d.dismiss())
                .setCancelable(false)
                .show();
    }

    private String buildSuccessDetail(SyncManager.SyncResult result) {
        if (result.totalUploaded() == 0 && result.totalDownloaded() == 0) {
            return "هیچ تغییری برای انتقال وجود نداشت.\nاطلاعات دستگاه و ابر یکسان هستند ✓";
        }
        StringBuilder sb = new StringBuilder();
        if (result.totalUploaded() > 0) {
            sb.append("✅ آپلود شد:\n");
            if (result.uploadedCategories > 0) sb.append("  • ").append(result.uploadedCategories).append(" دسته‌بندی\n");
            if (result.uploadedTags > 0) sb.append("  • ").append(result.uploadedTags).append(" برچسب\n");
            if (result.uploadedCards > 0) sb.append("  • ").append(result.uploadedCards).append(" کارت بانکی\n");
            if (result.uploadedTransactions > 0) sb.append("  • ").append(result.uploadedTransactions).append(" تراکنش\n");
            if (result.uploadedDeletes > 0) sb.append("  • ").append(result.uploadedDeletes).append(" مورد از ابر حذف شد\n");
        }
        if (result.totalDownloaded() > 0) {
            if (sb.length() > 0) sb.append("\n");
            sb.append("✅ دانلود شد:\n");
            if (result.downloadedCategories > 0) sb.append("  • ").append(result.downloadedCategories).append(" دسته‌بندی\n");
            if (result.downloadedTags > 0) sb.append("  • ").append(result.downloadedTags).append(" برچسب\n");
            if (result.downloadedCards > 0) sb.append("  • ").append(result.downloadedCards).append(" کارت بانکی\n");
            if (result.downloadedTransactions > 0) sb.append("  • ").append(result.downloadedTransactions).append(" تراکنش");
        }
        return sb.toString().trim();
    }

    private void setButtonsEnabled(boolean enabled) {
        btnCheckStatus.setEnabled(enabled);
        btnUpload.setEnabled(enabled);
        btnDownload.setEnabled(enabled);
    }
}
