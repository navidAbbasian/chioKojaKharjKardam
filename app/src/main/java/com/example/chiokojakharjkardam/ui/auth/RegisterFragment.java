package com.example.chiokojakharjkardam.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.chiokojakharjkardam.R;
import com.example.chiokojakharjkardam.utils.NetworkMonitor;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

public class RegisterFragment extends Fragment {

    private TextInputEditText etFullName, etEmail, etPassword;
    private MaterialButton btnRegister;
    private LinearProgressIndicator progress;
    private AuthViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_register, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);

        etFullName  = view.findViewById(R.id.et_full_name);
        etEmail     = view.findViewById(R.id.et_email);
        etPassword  = view.findViewById(R.id.et_password);
        btnRegister = view.findViewById(R.id.btn_register);
        progress    = view.findViewById(R.id.progress);

        btnRegister.setOnClickListener(v -> {
            // بررسی اتصال اینترنت قبل از ثبت‌نام
            if (!NetworkMonitor.getInstance().isOnline()) {
                Snackbar.make(view, "برای ثبت‌نام، اتصال اینترنت لازم است", Snackbar.LENGTH_LONG).show();
                return;
            }
            
            String name  = etFullName.getText() != null ? etFullName.getText().toString().trim() : "";
            String email = etEmail.getText()    != null ? etEmail.getText().toString().trim()    : "";
            String pass  = etPassword.getText() != null ? etPassword.getText().toString()        : "";
            viewModel.signUp(name, email, pass);
        });

        TextView tvLogin = view.findViewById(R.id.tv_go_login);
        tvLogin.setOnClickListener(v -> requireActivity().onBackPressed());

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            progress.setVisibility(loading ? View.VISIBLE : View.GONE);
            btnRegister.setEnabled(!loading);
        });

        viewModel.getError().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.isEmpty()) {
                Snackbar.make(view, msg, Snackbar.LENGTH_LONG).show();
                viewModel.clearError();
            }
        });
    }
}

