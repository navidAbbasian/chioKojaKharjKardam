package com.example.chiokojakharjkardam.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.example.chiokojakharjkardam.R;
import com.example.chiokojakharjkardam.ui.setup.SetupActivity;
import com.example.chiokojakharjkardam.utils.Constants;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private NavController navController;
    private TextView tvToolbarTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // بررسی اولین اجرا
        if (isFirstRun()) {
            startActivity(new Intent(this, SetupActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        setupToolbar();
        setupNavigation();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        tvToolbarTitle = findViewById(R.id.toolbar_title);
        setSupportActionBar(toolbar);

        // مخفی کردن عنوان پیش‌فرض
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // تنظیم padding برای notch
        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars());
            v.setPadding(v.getPaddingLeft(), insets.top, v.getPaddingRight(), v.getPaddingBottom());
            return windowInsets;
        });

        // تنظیم padding برای navigation bar (پایین)
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), insets.bottom);
            return windowInsets;
        });
    }

    /**
     * تنظیم عنوان toolbar
     */
    public void setToolbarTitle(String title) {
        if (tvToolbarTitle != null) {
            tvToolbarTitle.setText(title);
        }
    }

    private boolean isFirstRun() {
        SharedPreferences prefs = getSharedPreferences(Constants.PREF_NAME, MODE_PRIVATE);
        return !prefs.getBoolean(Constants.PREF_FAMILY_CREATED, false);
    }

    private void setupNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
            NavigationUI.setupWithNavController(bottomNav, navController);

            // تغییر عنوان toolbar با تغییر صفحه
            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                CharSequence label = destination.getLabel();
                if (label != null && tvToolbarTitle != null) {
                    tvToolbarTitle.setText(label);
                }
            });

            // با کلیک روی هر تب، back stack پاک شود و به صفحه اصلی آن تب برگردد
            bottomNav.setOnItemSelectedListener(item -> {
                // پاک کردن back stack و navigate به destination انتخاب شده
                navController.popBackStack(navController.getGraph().getStartDestinationId(), false);

                try {
                    navController.navigate(item.getItemId());
                } catch (Exception e) {
                    // اگر همین destination است، مشکلی نیست
                }
                return true;
            });

            // برای جلوگیری از مشکل reselect
            bottomNav.setOnItemReselectedListener(item -> {
                // وقتی روی همان تب کلیک می‌شود، به root آن تب برگرد
                navController.popBackStack(item.getItemId(), false);
            });
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController.navigateUp() || super.onSupportNavigateUp();
    }
}

