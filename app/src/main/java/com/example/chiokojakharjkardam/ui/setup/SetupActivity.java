package com.example.chiokojakharjkardam.ui.setup;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.chiokojakharjkardam.R;
import com.example.chiokojakharjkardam.data.database.AppDatabase;
import com.example.chiokojakharjkardam.data.database.entity.Family;
import com.example.chiokojakharjkardam.data.database.entity.Member;
import com.example.chiokojakharjkardam.data.repository.FamilyRepository;
import com.example.chiokojakharjkardam.data.repository.MemberRepository;
import com.example.chiokojakharjkardam.ui.MainActivity;
import com.example.chiokojakharjkardam.utils.Constants;

public class SetupActivity extends AppCompatActivity {

    private EditText etFamilyName;
    private EditText etOwnerName;
    private Button btnStart;
    private View progressBar;

    private FamilyRepository familyRepository;
    private MemberRepository memberRepository;

    private String selectedColor = Constants.MEMBER_COLORS[0];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // فعال‌سازی edge-to-edge برای پشتیبانی از notch
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        setContentView(R.layout.activity_setup);

        // تنظیم padding برای notch
        setupEdgeToEdge();

        familyRepository = new FamilyRepository(getApplication());
        memberRepository = new MemberRepository(getApplication());

        initViews();
        setupListeners();
    }

    private void setupEdgeToEdge() {
        View rootView = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    private void initViews() {
        etFamilyName = findViewById(R.id.et_family_name);
        etOwnerName = findViewById(R.id.et_owner_name);
        btnStart = findViewById(R.id.btn_start);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void setupListeners() {
        btnStart.setOnClickListener(v -> createFamily());
    }

    private void createFamily() {
        String familyName = etFamilyName.getText().toString().trim();
        String ownerName = etOwnerName.getText().toString().trim();

        if (familyName.isEmpty()) {
            etFamilyName.setError("نام خانواده را وارد کنید");
            return;
        }

        if (ownerName.isEmpty()) {
            etOwnerName.setError("نام خود را وارد کنید");
            return;
        }

        showLoading(true);

        // ایجاد خانواده
        Family family = new Family(familyName);
        familyRepository.insertAndGetId(family, familyId -> {
            // ایجاد عضو اصلی (صاحب برنامه)
            Member owner = new Member(familyId, ownerName, true, selectedColor);
            memberRepository.insertAndGetId(owner, memberId -> {
                // ذخیره وضعیت
                runOnUiThread(() -> {
                    saveSetupComplete();
                    showLoading(false);
                    goToMain();
                });
            });
        });
    }

    private void saveSetupComplete() {
        SharedPreferences prefs = getSharedPreferences(Constants.PREF_NAME, MODE_PRIVATE);
        prefs.edit().putBoolean(Constants.PREF_FAMILY_CREATED, true).apply();
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnStart.setEnabled(!show);
        etFamilyName.setEnabled(!show);
        etOwnerName.setEnabled(!show);
    }
}

