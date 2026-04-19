package com.example.chiokojakharjkardam.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.example.chiokojakharjkardam.R;
import com.example.chiokojakharjkardam.ui.auth.AuthActivity;
import com.example.chiokojakharjkardam.ui.family.FamilySetupActivity;
import com.example.chiokojakharjkardam.utils.NetworkMonitor;
import com.example.chiokojakharjkardam.utils.SessionManager;
import com.example.chiokojakharjkardam.utils.SyncManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;

public class MainActivity extends AppCompatActivity {

    private NavController navController;
    private TextView tvToolbarTitle;
    private View offlineBanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // بررسی وضعیت ورود کاربر
        SessionManager session = SessionManager.getInstance();
        if (!session.isLoggedIn()) {
            startActivity(new Intent(this, AuthActivity.class));
            finish();
            return;
        }
        if (!session.hasFamilyId()) {
            startActivity(new Intent(this, FamilySetupActivity.class));
            finish();
            return;
        }

        // تجدید دوره ۹۰ روزه هر بار که اپ باز می‌شود
        session.refreshLocalSession();

        setContentView(R.layout.activity_main);

        setupToolbar();
        setupNavigation();
        observeNetwork();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh cloud cache every time user returns to the app
        SyncManager.getInstance().syncAll();
    }

    // ── Toolbar ────────────────────────────────────────────────────

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        tvToolbarTitle = findViewById(R.id.toolbar_title);
        offlineBanner  = findViewById(R.id.offline_banner);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayShowTitleEnabled(false);

        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            v.setPadding(v.getPaddingLeft(), bars.top, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), bars.bottom);
            return insets;
        });
    }

    private void observeNetwork() {
        NetworkMonitor.getInstance().isConnected().observe(this, online -> {
            if (offlineBanner == null) return;
            offlineBanner.setVisibility(online ? View.GONE : View.VISIBLE);

            if (online) {
                // وقتی دستگاه آنلاین می‌شود، همگام‌سازی خودکار انجام بده
                SyncManager.getInstance().syncAll();
                
                // بررسی نیاز به احراز هویت مجدد
                if (SessionManager.getInstance().needsReauth()) {
                    View root = findViewById(android.R.id.content);
                    Snackbar.make(root,
                            "برای همگام‌سازی ابری، لطفاً مجدداً وارد شوید",
                            Snackbar.LENGTH_LONG)
                            .setAction("ورود", v -> {
                                startActivity(new Intent(this, AuthActivity.class));
                            })
                            .show();
                }
            }
        });
    }

    /**
     * تنظیم عنوان toolbar
     */
    public void setToolbarTitle(String title) {
        if (tvToolbarTitle != null) tvToolbarTitle.setText(title);
    }

    // ── Navigation ─────────────────────────────────────────────────

    private void setupNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        NavHostFragment navHostFragment = (NavHostFragment)
                getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment == null) return;

        navController = navHostFragment.getNavController();
        NavigationUI.setupWithNavController(bottomNav, navController);

        navController.addOnDestinationChangedListener((ctrl, dest, args) -> {
            CharSequence label = dest.getLabel();
            if (label != null && tvToolbarTitle != null) tvToolbarTitle.setText(label);
        });

        bottomNav.setOnItemSelectedListener(item -> {
            navController.popBackStack(navController.getGraph().getStartDestinationId(), false);
            try { navController.navigate(item.getItemId()); } catch (Exception ignored) {}
            return true;
        });

        bottomNav.setOnItemReselectedListener(item ->
                navController.popBackStack(item.getItemId(), false));
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController.navigateUp() || super.onSupportNavigateUp();
    }
}
