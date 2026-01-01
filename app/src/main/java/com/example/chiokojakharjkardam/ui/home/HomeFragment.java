package com.example.chiokojakharjkardam.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chiokojakharjkardam.R;
import com.example.chiokojakharjkardam.ui.adapters.TransactionAdapter;
import com.example.chiokojakharjkardam.utils.CurrencyUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class HomeFragment extends Fragment {

    private HomeViewModel viewModel;
    private TextView tvTotalBalance;
    private TextView tvMonthExpense;
    private TextView tvMonthIncome;
    private RecyclerView rvRecentTransactions;
    private LinearLayout layoutEmpty;
    private TransactionAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        initViews(view);
        setupRecyclerView();
        setupListeners(view);
        observeData();
    }

    private void initViews(View view) {
        tvTotalBalance = view.findViewById(R.id.tv_total_balance);
        tvMonthExpense = view.findViewById(R.id.tv_month_expense);
        tvMonthIncome = view.findViewById(R.id.tv_month_income);
        rvRecentTransactions = view.findViewById(R.id.rv_recent_transactions);
        layoutEmpty = view.findViewById(R.id.layout_empty);
    }

    private void setupRecyclerView() {
        adapter = new TransactionAdapter(transaction -> {
            // کلیک روی تراکنش - رفتن به صفحه جزئیات
            Bundle args = new Bundle();
            args.putLong("transactionId", transaction.getId());
            Navigation.findNavController(requireView())
                    .navigate(R.id.transactionDetailFragment, args);
        });
        rvRecentTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvRecentTransactions.setAdapter(adapter);
    }

    private void setupListeners(View view) {
        FloatingActionButton fabAdd = view.findViewById(R.id.fab_add_transaction);
        fabAdd.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.addTransactionFragment);
        });

        MaterialButton btnSeeAll = view.findViewById(R.id.btn_see_all);
        btnSeeAll.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.transactionsFragment);
        });
    }

    private void observeData() {
        viewModel.getTotalBalance().observe(getViewLifecycleOwner(), balance -> {
            if (balance != null) {
                tvTotalBalance.setText(CurrencyUtils.formatToman(balance));
            }
        });

        viewModel.getMonthExpense().observe(getViewLifecycleOwner(), expense -> {
            if (expense != null) {
                tvMonthExpense.setText(CurrencyUtils.formatToman(expense));
            }
        });

        viewModel.getMonthIncome().observe(getViewLifecycleOwner(), income -> {
            if (income != null) {
                tvMonthIncome.setText(CurrencyUtils.formatToman(income));
            }
        });

        viewModel.getRecentTransactions().observe(getViewLifecycleOwner(), transactions -> {
            if (transactions != null && !transactions.isEmpty()) {
                adapter.submitList(transactions);
                rvRecentTransactions.setVisibility(View.VISIBLE);
                layoutEmpty.setVisibility(View.GONE);
            } else {
                rvRecentTransactions.setVisibility(View.GONE);
                layoutEmpty.setVisibility(View.VISIBLE);
            }
        });
    }
}

