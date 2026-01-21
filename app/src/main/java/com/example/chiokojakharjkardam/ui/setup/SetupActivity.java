package com.example.chiokojakharjkardam.ui.setup;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.example.chiokojakharjkardam.R;
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

        setContentView(R.layout.activity_setup);

        familyRepository = new FamilyRepository(getApplication());
        memberRepository = new MemberRepository(getApplication());

        initViews();
        setupListeners();
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

