package com.example.chiokojakharjkardam.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.example.chiokojakharjkardam.R;
import com.example.chiokojakharjkardam.ui.MainActivity;
import com.example.chiokojakharjkardam.ui.family.FamilySetupActivity;
import com.example.chiokojakharjkardam.utils.NetworkMonitor;
import com.example.chiokojakharjkardam.utils.SessionManager;
import com.example.chiokojakharjkardam.utils.SyncManager;

/**
 * Container Activity for Login / Register fragments.
 * Shared AuthViewModel drives navigation decisions.
 */
public class AuthActivity extends AppCompatActivity {

    AuthViewModel viewModel;
    private View offlineBanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SessionManager session = SessionManager.getInstance();
        
        // ── اگر کاربر قبلاً وارد شده، از صفحه ورود رد شو ──────────
        if (session.isLoggedIn()) {
            if (session.hasFamilyId()) {
                // فقط زمانی سینک کن که آنلاین باشیم
                if (NetworkMonitor.getInstance().isOnline()) {
                    SyncManager.getInstance().syncAll();
                }
                startActivity(new Intent(this, MainActivity.class));
            } else {
                // اگر آفلاین است و خانواده ندارد، باید صبر کند تا آنلاین شود
                if (!NetworkMonitor.getInstance().isOnline()) {
                    // نمایش صفحه ورود با پیام آفلاین
                    setContentView(R.layout.activity_auth);
                    setupOfflineBanner();
                    showOfflineNoFamilyMessage();
                    return;
                }
                startActivity(new Intent(this, FamilySetupActivity.class));
            }
            finish();
            return;
        }

        setContentView(R.layout.activity_auth);
        setupOfflineBanner();

        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        viewModel.getNavTarget().observe(this, target -> {
            if (target == null) return;
            if (target == AuthViewModel.NavTarget.MAIN) {
                // Trigger initial sync before entering main screen
                SyncManager.getInstance().syncAll();
                startActivity(new Intent(this, MainActivity.class));
            } else {
                startActivity(new Intent(this, FamilySetupActivity.class));
            }
            finish();
        });

        if (savedInstanceState == null) {
            showFragment(new LoginFragment(), false);
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
    
    private void showOfflineNoFamilyMessage() {
        // کاربر لاگین شده ولی هنوز خانواده نساخته و آفلاین است
        // باید یک پیام نمایش دهیم که صبر کند تا آنلاین شود
        TextView tvMessage = findViewById(R.id.tv_offline_family_message);
        if (tvMessage != null) {
            tvMessage.setVisibility(View.VISIBLE);
        }
        
        // وقتی آنلاین شد، به FamilySetup برود
        NetworkMonitor.getInstance().isConnected().observe(this, online -> {
            if (online && SessionManager.getInstance().isLoggedIn() 
                    && !SessionManager.getInstance().hasFamilyId()) {
                startActivity(new Intent(this, FamilySetupActivity.class));
                finish();
            }
        });
    }

    void showFragment(Fragment fragment, boolean addToBack) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction()
                .replace(R.id.auth_container, fragment);
        if (addToBack) ft.addToBackStack(null);
        ft.commit();
    }
}

