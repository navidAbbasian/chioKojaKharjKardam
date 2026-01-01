package com.example.chiokojakharjkardam.ui.transactions;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.chiokojakharjkardam.R;
import com.example.chiokojakharjkardam.data.database.entity.Transaction;
import com.example.chiokojakharjkardam.utils.CurrencyUtils;
import com.example.chiokojakharjkardam.utils.PersianDateUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class TransactionDetailFragment extends Fragment {

    private TransactionDetailViewModel viewModel;

    private TextView tvAmount;
    private TextView tvType;
    private TextView tvDescription;
    private TextView tvCategory;
    private TextView tvCard;
    private TextView tvDate;
    private MaterialButton btnEdit;
    private MaterialButton btnDelete;

    private long transactionId;
    private Transaction currentTransaction;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_transaction_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(TransactionDetailViewModel.class);

        if (getArguments() != null) {
            transactionId = getArguments().getLong("transactionId", -1);
        }

        initViews(view);
        setupListeners();
        loadTransaction();
    }

    private void initViews(View view) {
        tvAmount = view.findViewById(R.id.tv_amount);
        tvType = view.findViewById(R.id.tv_type);
        tvDescription = view.findViewById(R.id.tv_description);
        tvCategory = view.findViewById(R.id.tv_category);
        tvCard = view.findViewById(R.id.tv_card);
        tvDate = view.findViewById(R.id.tv_date);
        btnEdit = view.findViewById(R.id.btn_edit);
        btnDelete = view.findViewById(R.id.btn_delete);
    }

    private void setupListeners() {
        btnEdit.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putLong("transactionId", transactionId);
            Navigation.findNavController(v).navigate(R.id.addTransactionFragment, args);
        });

        btnDelete.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.delete)
                    .setMessage(R.string.confirm_delete)
                    .setPositiveButton(R.string.yes, (dialog, which) -> {
                        if (currentTransaction != null) {
                            viewModel.deleteTransaction(currentTransaction);
                            Navigation.findNavController(requireView()).popBackStack();
                        }
                    })
                    .setNegativeButton(R.string.no, null)
                    .show();
        });
    }

    private void loadTransaction() {
        viewModel.getTransactionById(transactionId).observe(getViewLifecycleOwner(), transaction -> {
            if (transaction != null) {
                currentTransaction = transaction;
                displayTransaction(transaction);
            }
        });
    }

    private void displayTransaction(Transaction transaction) {
        String amountText = CurrencyUtils.formatToman(transaction.getAmount());
        if (transaction.getType() == Transaction.TYPE_EXPENSE) {
            tvAmount.setText("-" + amountText);
            tvAmount.setTextColor(Color.parseColor("#F44336"));
            tvType.setText("خرج");
        } else {
            tvAmount.setText("+" + amountText);
            tvAmount.setTextColor(Color.parseColor("#4CAF50"));
            tvType.setText("درآمد");
        }

        tvDescription.setText(transaction.getDescription());
        tvDate.setText(PersianDateUtils.formatDate(transaction.getDate()));

        // TODO: Load category and card names
        tvCategory.setText("-");
        tvCard.setText("-");
    }
}

