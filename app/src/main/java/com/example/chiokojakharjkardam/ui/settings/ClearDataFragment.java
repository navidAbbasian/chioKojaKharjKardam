package com.example.chiokojakharjkardam.ui.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.chiokojakharjkardam.R;
import com.example.chiokojakharjkardam.ui.MainActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class ClearDataFragment extends Fragment {

    private SettingsViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_clear_data, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(SettingsViewModel.class);

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v ->
                requireActivity().getOnBackPressedDispatcher().onBackPressed()
        );

        MaterialCardView cardClearTransactions = view.findViewById(R.id.card_clear_transactions);
        cardClearTransactions.setOnClickListener(v -> showClearTransactionsConfirmDialog());

        MaterialCardView cardClearAll = view.findViewById(R.id.card_clear_all);
        cardClearAll.setOnClickListener(v -> showClearAllConfirmDialog());
    }

    private void showClearTransactionsConfirmDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.clear_transactions_only)
                .setMessage(R.string.clear_transactions_confirm_msg)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    viewModel.clearTransactionsOnly(new SettingsViewModel.ClearDataCallback() {
                        @Override
                        public void onSuccess() {
                            if (isAdded()) {
                                requireActivity().runOnUiThread(() ->
                                        Toast.makeText(requireContext(),
                                                R.string.clear_transactions_success,
                                                Toast.LENGTH_SHORT).show()
                                );
                                requireActivity().runOnUiThread(() ->
                                        requireActivity().getOnBackPressedDispatcher().onBackPressed()
                                );
                            }
                        }

                        @Override
                        public void onError(String error) {
                            if (isAdded()) {
                                requireActivity().runOnUiThread(() ->
                                        Toast.makeText(requireContext(),
                                                R.string.clear_data_error,
                                                Toast.LENGTH_LONG).show()
                                );
                            }
                        }
                    });
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showClearAllConfirmDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.clear_all_data)
                .setMessage(getString(R.string.clear_data_warning) + "\n\n" + getString(R.string.clear_data_confirm))
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    viewModel.clearAllData(new SettingsViewModel.ClearDataCallback() {
                        @Override
                        public void onSuccess() {
                            if (isAdded()) {
                                requireActivity().runOnUiThread(() -> {
                                    new MaterialAlertDialogBuilder(requireContext())
                                            .setTitle(R.string.success)
                                            .setMessage(R.string.clear_data_success)
                                            .setPositiveButton(R.string.yes, (d, w) -> {
                                                Intent intent = new Intent(requireContext(), MainActivity.class);
                                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                startActivity(intent);
                                                requireActivity().finish();
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
                                        Toast.makeText(requireContext(),
                                                R.string.clear_data_error,
                                                Toast.LENGTH_LONG).show()
                                );
                            }
                        }
                    });
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}

