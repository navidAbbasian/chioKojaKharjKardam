package com.example.chiokojakharjkardam.ui.settings;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.chiokojakharjkardam.R;
import com.example.chiokojakharjkardam.ui.auth.AuthActivity;
import com.example.chiokojakharjkardam.utils.BackupManager;
import com.example.chiokojakharjkardam.utils.ThemeManager;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.Calendar;

public class SettingsFragment extends Fragment {

    private SettingsViewModel viewModel;
    private TextView tvFamilyName;
    private TextView tvUserName;
    private TextView tvUserEmail;
    private TextView tvInviteCode;
    private MaterialCardView cardInviteCode;
    private ThemeManager themeManager;
    private MaterialSwitch switchTheme;
    private TextView tvCurrentTheme;

    private final ActivityResultLauncher<Intent> createBackupLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) performBackup(uri);
                }
            }
    );

    private final ActivityResultLauncher<Intent> restoreBackupLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) performRestore(uri);
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(SettingsViewModel.class);
        initViews(view);
        setupListeners(view);
        observeData();
    }

    private void initViews(View view) {
        tvFamilyName  = view.findViewById(R.id.tv_family_name);
        tvUserName    = view.findViewById(R.id.tv_user_name);
        tvUserEmail   = view.findViewById(R.id.tv_user_email);
        tvInviteCode  = view.findViewById(R.id.tv_invite_code);
        cardInviteCode = view.findViewById(R.id.card_invite_code);

        themeManager  = new ThemeManager(requireContext());
        switchTheme   = view.findViewById(R.id.switch_theme);
        tvCurrentTheme = view.findViewById(R.id.tv_current_theme);

        // Account info
        String name = viewModel.getUserFullName();
        String email = viewModel.getUserEmail();
        if (tvUserName  != null) tvUserName.setText(name  != null ? name  : "");
        if (tvUserEmail != null) tvUserEmail.setText(email != null ? email : "");

        // Invite code — visible only for family owner
        String inviteCode = viewModel.getInviteCode();
        if (viewModel.isOwner() && inviteCode != null && !inviteCode.isEmpty()) {
            cardInviteCode.setVisibility(View.VISIBLE);
            tvInviteCode.setText(inviteCode);
        }

        updateThemeUI(themeManager.isDarkMode());
    }

    private void setupListeners(View view) {
        MaterialCardView cardFamily = view.findViewById(R.id.card_family);
        cardFamily.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.membersFragment));

        MaterialCardView cardCategories = view.findViewById(R.id.card_categories);
        cardCategories.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.categoriesFragment));

        MaterialCardView cardTags = view.findViewById(R.id.card_tags);
        cardTags.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.tagsFragment));

        MaterialCardView cardReports = view.findViewById(R.id.card_reports);
        cardReports.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.reportsFragment));

        MaterialCardView cardBackup = view.findViewById(R.id.card_backup);
        cardBackup.setOnClickListener(v -> showBackupDialog());

        MaterialCardView cardRestore = view.findViewById(R.id.card_restore);
        cardRestore.setOnClickListener(v -> showRestoreDialog());

        MaterialCardView cardClearData = view.findViewById(R.id.card_clear_data);
        cardClearData.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_settings_to_clearData));

        MaterialCardView cardAbout = view.findViewById(R.id.card_about);
        cardAbout.setOnClickListener(v -> showAboutDialog());

        // Invite code — tap to copy
        if (cardInviteCode != null) {
            cardInviteCode.setOnClickListener(v -> copyInviteCode());
        }

        // Sync now
        MaterialCardView cardSync = view.findViewById(R.id.card_sync);
        if (cardSync != null) {
            cardSync.setOnClickListener(v ->
                    Navigation.findNavController(v).navigate(R.id.action_settings_to_cloudSync));
        }

        // Logout
        MaterialCardView cardLogout = view.findViewById(R.id.card_logout);
        if (cardLogout != null) {
            cardLogout.setOnClickListener(v -> showLogoutDialog());
        }

        switchTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
            themeManager.setDarkMode(isChecked);
            updateThemeUI(isChecked);
        });
    }

    private void copyInviteCode() {
        String code = viewModel.getInviteCode();
        if (code == null || code.isEmpty()) return;
        ClipboardManager clipboard = (ClipboardManager)
                requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("invite_code", code);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(requireContext(), "کد دعوت کپی شد: " + code, Toast.LENGTH_SHORT).show();
    }

    private void showLogoutDialog() {
        boolean isOnline = com.example.chiokojakharjkardam.utils.NetworkMonitor
                .getInstance().isOnline();
        
        // بررسی وجود داده‌های سینک نشده
        com.example.chiokojakharjkardam.utils.SyncManager.getInstance()
                .hasPendingDataSync(hasPendingData -> {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        String message;
                        if (!isOnline && hasPendingData) {
                            // آفلاین با داده‌های سینک نشده - هشدار جدی
                            message = "⚠️ هشدار مهم!\n\n" +
                                    "دستگاه شما آفلاین است و اطلاعاتی دارید که هنوز با ابر همگام نشده‌اند.\n\n" +
                                    "اگر الان خارج شوید، این اطلاعات برای همیشه از دست می‌روند!\n\n" +
                                    "توصیه: ابتدا به اینترنت متصل شوید.";
                        } else if (!isOnline) {
                            // آفلاین بدون داده‌های سینک نشده
                            message = "دستگاه آفلاین است.\nآیا می‌خواهید خارج شوید؟";
                        } else if (hasPendingData) {
                            // آنلاین با داده‌های سینک نشده
                            message = "اطلاعات همگام‌نشده شما ابتدا به ابر ارسال می‌شود، سپس از دستگاه پاک خواهد شد.\n\nآیا مطمئن هستید؟";
                        } else {
                            // آنلاین بدون داده‌های سینک نشده
                            message = "آیا مطمئن هستید که می‌خواهید خارج شوید؟\nاطلاعات محلی پاک خواهد شد.";
                        }
                        
                        new MaterialAlertDialogBuilder(requireContext())
                                .setTitle("خروج از حساب")
                                .setMessage(message)
                                .setPositiveButton("خروج", (d, w) -> performLogout())
                                .setNegativeButton(R.string.cancel, null)
                                .show();
                    });
                });
    }

    private void performLogout() {
        Toast.makeText(requireContext(), "در حال آپلود اطلاعات و خروج…", Toast.LENGTH_LONG).show();
        viewModel.logout(new SettingsViewModel.ClearDataCallback() {
            @Override public void onSuccess() {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    Intent intent = new Intent(requireActivity(), AuthActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                });
            }
            @Override public void onError(String error) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "خطا: " + error, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void showBackupDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.backup_title)
                .setMessage(R.string.backup_description)
                .setPositiveButton(R.string.backup, (dialog, which) -> {
                    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("application/octet-stream");
                    intent.putExtra(Intent.EXTRA_TITLE, BackupManager.generateBackupFileName());
                    createBackupLauncher.launch(intent);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showRestoreDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.restore_title)
                .setMessage(getString(R.string.restore_description) + "\n\n" + getString(R.string.restore_warning))
                .setPositiveButton(R.string.select_backup_file, (dialog, which) -> {
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("*/*");
                    restoreBackupLauncher.launch(intent);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void performBackup(Uri uri) {
        BackupManager.createBackup(requireContext(), uri, new BackupManager.BackupCallback() {
            @Override public void onSuccess(String message) {
                if (isAdded()) requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show());
            }
            @Override public void onError(String error) {
                if (isAdded()) requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show());
            }
        });
    }

    private void performRestore(Uri uri) {
        BackupManager.restoreBackup(requireContext(), uri, new BackupManager.BackupCallback() {
            @Override public void onSuccess(String message) {
                if (isAdded()) requireActivity().runOnUiThread(() ->
                        new MaterialAlertDialogBuilder(requireContext())
                                .setTitle(R.string.success)
                                .setMessage(message)
                                .setPositiveButton(R.string.yes, (dialog, which) ->
                                        requireActivity().finishAffinity())
                                .setCancelable(false)
                                .show());
            }
            @Override public void onError(String error) {
                if (isAdded()) requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show());
            }
        });
    }

    private void showAboutDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_about, null);
        TextView tvVersion = dialogView.findViewById(R.id.tv_version);
        try {
            PackageInfo pInfo = requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0);
            tvVersion.setText(getString(R.string.version_format, pInfo.versionName));
        } catch (PackageManager.NameNotFoundException e) {
            tvVersion.setText(getString(R.string.version_format, "1.0.0"));
        }
        TextView tvCopyright = dialogView.findViewById(R.id.tv_copyright);
        tvCopyright.setText(getString(R.string.copyright_format,
                Calendar.getInstance().get(Calendar.YEAR)));

        new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setPositiveButton(R.string.close, null)
                .show();
    }

    private void updateThemeUI(boolean isDarkMode) {
        switchTheme.setChecked(isDarkMode);
        tvCurrentTheme.setText(isDarkMode ? R.string.dark_mode : R.string.light_mode);
    }

    private void observeData() {
        viewModel.getFamily().observe(getViewLifecycleOwner(), family -> {
            if (family != null) {
                tvFamilyName.setText(family.getName());
            } else {
                tvFamilyName.setText(R.string.no_family_yet);
            }
        });
    }
}

