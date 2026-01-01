package com.example.chiokojakharjkardam.ui.settings;

import android.app.Activity;
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
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.chiokojakharjkardam.R;
import com.example.chiokojakharjkardam.utils.BackupManager;
import com.example.chiokojakharjkardam.utils.ThemeManager;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.Calendar;

public class SettingsFragment extends Fragment {

    private SettingsViewModel viewModel;
    private TextView tvFamilyName;
    private ThemeManager themeManager;
    private MaterialSwitch switchTheme;
    private TextView tvCurrentTheme;
    private TextView tvThemeIcon;

    // برای انتخاب محل ذخیره پشتیبان
    private final ActivityResultLauncher<Intent> createBackupLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        performBackup(uri);
                    }
                }
            }
    );

    // برای انتخاب فایل پشتیبان برای بازیابی
    private final ActivityResultLauncher<Intent> restoreBackupLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        performRestore(uri);
                    }
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
        tvFamilyName = view.findViewById(R.id.tv_family_name);

        // تنظیمات تم
        themeManager = new ThemeManager(requireContext());
        switchTheme = view.findViewById(R.id.switch_theme);
        tvCurrentTheme = view.findViewById(R.id.tv_current_theme);
        tvThemeIcon = view.findViewById(R.id.tv_theme_icon);

        // تنظیم وضعیت اولیه سوییچ تم
        updateThemeUI(themeManager.isDarkMode());
    }

    private void setupListeners(View view) {
        MaterialCardView cardFamily = view.findViewById(R.id.card_family);
        cardFamily.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.membersFragment);
        });

        MaterialCardView cardCategories = view.findViewById(R.id.card_categories);
        cardCategories.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.categoriesFragment);
        });

        MaterialCardView cardTags = view.findViewById(R.id.card_tags);
        cardTags.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.tagsFragment);
        });

        MaterialCardView cardBackup = view.findViewById(R.id.card_backup);
        cardBackup.setOnClickListener(v -> showBackupDialog());

        MaterialCardView cardRestore = view.findViewById(R.id.card_restore);
        cardRestore.setOnClickListener(v -> showRestoreDialog());

        MaterialCardView cardAbout = view.findViewById(R.id.card_about);
        cardAbout.setOnClickListener(v -> showAboutDialog());

        // تنظیم listener برای سوییچ تم
        switchTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
            themeManager.setDarkMode(isChecked);
            updateThemeUI(isChecked);
        });
    }

    private void showBackupDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.backup_title)
                .setMessage(R.string.backup_description)
                .setPositiveButton(R.string.backup, (dialog, which) -> {
                    // باز کردن فایل منیجر برای انتخاب محل ذخیره
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
                    // باز کردن فایل منیجر برای انتخاب فایل پشتیبان
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
            @Override
            public void onSuccess(String message) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                    );
                }
            }

            @Override
            public void onError(String error) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
                    );
                }
            }
        });
    }

    private void performRestore(Uri uri) {
        BackupManager.restoreBackup(requireContext(), uri, new BackupManager.BackupCallback() {
            @Override
            public void onSuccess(String message) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        new MaterialAlertDialogBuilder(requireContext())
                                .setTitle(R.string.success)
                                .setMessage(message)
                                .setPositiveButton(R.string.yes, (dialog, which) -> {
                                    // راه‌اندازی مجدد برنامه
                                    requireActivity().finishAffinity();
                                })
                                .setCancelable(false)
                                .show();
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
                    );
                }
            }
        });
    }

    private void showAboutDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_about, null);

        // تنظیم نسخه
        TextView tvVersion = dialogView.findViewById(R.id.tv_version);
        try {
            PackageInfo pInfo = requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0);
            String version = pInfo.versionName;
            tvVersion.setText(getString(R.string.version_format, version));
        } catch (PackageManager.NameNotFoundException e) {
            tvVersion.setText(getString(R.string.version_format, "1.0.0"));
        }

        // تنظیم کپی‌رایت
        TextView tvCopyright = dialogView.findViewById(R.id.tv_copyright);
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        tvCopyright.setText(getString(R.string.copyright_format, currentYear));

        new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setPositiveButton(R.string.close, null)
                .show();
    }

    private void updateThemeUI(boolean isDarkMode) {
        switchTheme.setChecked(isDarkMode);
        if (isDarkMode) {
            tvCurrentTheme.setText(R.string.dark_mode);
            tvThemeIcon.setText("🌙");
        } else {
            tvCurrentTheme.setText(R.string.light_mode);
            tvThemeIcon.setText("☀️");
        }
    }

    private void observeData() {
        viewModel.getFamily().observe(getViewLifecycleOwner(), family -> {
            if (family != null) {
                tvFamilyName.setText(family.getName());
            }
        });
    }
}

