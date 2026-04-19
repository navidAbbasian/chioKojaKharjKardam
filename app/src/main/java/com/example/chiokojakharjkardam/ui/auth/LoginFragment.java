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

public class LoginFragment extends Fragment {

    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin;
    private LinearProgressIndicator progress;
    private AuthViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);

        etEmail    = view.findViewById(R.id.et_email);
        etPassword = view.findViewById(R.id.et_password);
        btnLogin   = view.findViewById(R.id.btn_login);
        progress   = view.findViewById(R.id.progress);

        btnLogin.setOnClickListener(v -> {
            // بررسی اتصال اینترنت قبل از ورود
            if (!NetworkMonitor.getInstance().isOnline()) {
                Snackbar.make(view, "برای ورود به حساب، اتصال اینترنت لازم است", Snackbar.LENGTH_LONG).show();
                return;
            }
            
            String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
            String pass  = etPassword.getText() != null ? etPassword.getText().toString() : "";
            viewModel.signIn(email, pass);
        });

        TextView tvRegister = view.findViewById(R.id.tv_go_register);
        tvRegister.setOnClickListener(v ->
                ((AuthActivity) requireActivity()).showFragment(new RegisterFragment(), true));

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            progress.setVisibility(loading ? View.VISIBLE : View.GONE);
            btnLogin.setEnabled(!loading);
        });

        viewModel.getError().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.isEmpty()) {
                Snackbar.make(view, msg, Snackbar.LENGTH_LONG).show();
                viewModel.clearError();
            }
        });
    }
}

