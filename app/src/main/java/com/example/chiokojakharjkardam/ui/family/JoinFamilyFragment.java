package com.example.chiokojakharjkardam.ui.family;

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

public class JoinFamilyFragment extends Fragment {

    private TextInputEditText etInviteCode;
    private MaterialButton btnJoin;
    private LinearProgressIndicator progress;
    private FamilyViewModel viewModel;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_join_family, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(FamilyViewModel.class);

        etInviteCode = view.findViewById(R.id.et_invite_code);
        btnJoin      = view.findViewById(R.id.btn_join_family);
        progress     = view.findViewById(R.id.progress);

        btnJoin.setOnClickListener(v -> {
            // بررسی اتصال اینترنت قبل از پیوستن به خانواده
            if (!NetworkMonitor.getInstance().isOnline()) {
                Snackbar.make(view, "برای پیوستن به خانواده، اتصال اینترنت لازم است", Snackbar.LENGTH_LONG).show();
                return;
            }
            
            String code = etInviteCode.getText() != null
                    ? etInviteCode.getText().toString().trim().toUpperCase() : "";
            viewModel.joinFamily(code);
        });

        TextView tvCreate = view.findViewById(R.id.tv_go_create);
        tvCreate.setOnClickListener(v -> requireActivity().onBackPressed());

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            progress.setVisibility(loading ? View.VISIBLE : View.GONE);
            btnJoin.setEnabled(!loading);
        });

        viewModel.getError().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.isEmpty()) {
                Snackbar.make(view, msg, Snackbar.LENGTH_LONG).show();
                viewModel.clearError();
            }
        });
    }
}

