package com.example.chiokojakharjkardam.ui.family;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.example.chiokojakharjkardam.R;
import com.example.chiokojakharjkardam.ui.MainActivity;
import com.example.chiokojakharjkardam.utils.DataMigrationManager;
import com.example.chiokojakharjkardam.utils.NetworkMonitor;
import com.example.chiokojakharjkardam.utils.SyncManager;

/**
 * Container for CreateFamilyFragment and JoinFamilyFragment.
 * After family is created/joined it checks for migratable local data,
 * shows a migration dialog if needed, then launches MainActivity.
 */
public class FamilySetupActivity extends AppCompatActivity {

    FamilyViewModel viewModel;
    private DataMigrationManager migrationManager;
    private View offlineBanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_family_setup);

        setupOfflineBanner();
        
        migrationManager = new DataMigrationManager(getApplication());
        viewModel = new ViewModelProvider(this).get(FamilyViewModel.class);

        viewModel.getSuccess().observe(this, ok -> {
            if (ok == null || !ok) return;
            Boolean hasData = viewModel.getHasMigratableData().getValue();
            if (Boolean.TRUE.equals(hasData)) {
                showMigrationDialog();
            } else {
                goToMain();
            }
        });

        if (savedInstanceState == null) {
            showFragment(new CreateFamilyFragment(), false);
        }
    }
    
    private void setupOfflineBanner() {
        offlineBanner = findViewById(R.id.offline_banner);
        if (offlineBanner != null) {
            NetworkMonitor.getInstance().isConnected().observe(this, online -> {
                offlineBanner.setVisibility(online ? View.GONE : View.VISIBLE);
            });
        }
    }

    void showFragment(Fragment fragment, boolean addToBack) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction()
                .replace(R.id.family_setup_container, fragment);
        if (addToBack) ft.addToBackStack(null);
        ft.commit();
    }

    private void showMigrationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("انتقال اطلاعات")
                .setMessage("اطلاعات قبلی شما در دستگاه پیدا شد.\nآیا می‌خواهید آن‌ها را به فضای ابری منتقل کنید؟")
                .setPositiveButton("بله، انتقال بده", (d, w) -> runMigration())
                .setNegativeButton("نه، رد کن", (d, w) -> goToMain())
                .setCancelable(false)
                .show();
    }

    private void runMigration() {
        migrationManager.migrateLocalDataToCloud(new DataMigrationManager.MigrationCallback() {
            @Override public void onProgress(String msg) {
                runOnUiThread(() -> {}); // optionally show progress
            }
            @Override public void onComplete() {
                SyncManager.getInstance().syncAll();
                goToMain();
            }
            @Override public void onError(String msg) {
                goToMain(); // still proceed even on migration error
            }
        });
    }

    private void goToMain() {
        SyncManager.getInstance().syncAll();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}

