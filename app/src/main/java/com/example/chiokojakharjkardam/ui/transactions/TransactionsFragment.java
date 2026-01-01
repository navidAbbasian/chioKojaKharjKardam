package com.example.chiokojakharjkardam.ui.transactions;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chiokojakharjkardam.R;
import com.example.chiokojakharjkardam.data.database.entity.BankCard;
import com.example.chiokojakharjkardam.data.database.entity.Category;
import com.example.chiokojakharjkardam.ui.adapters.TransactionAdapter;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class TransactionsFragment extends Fragment {

    private TransactionsViewModel viewModel;
    private RecyclerView rvTransactions;
    private LinearLayout layoutEmpty;
    private TransactionAdapter adapter;
    private Chip chipAll, chipExpense, chipIncome, chipFilter;

    private List<Category> categoriesList = new ArrayList<>();
    private List<BankCard> cardsList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_transactions, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(TransactionsViewModel.class);

        initViews(view);
        setupRecyclerView();
        setupListeners(view);
        observeData();
    }

    private void initViews(View view) {
        rvTransactions = view.findViewById(R.id.rv_transactions);
        layoutEmpty = view.findViewById(R.id.layout_empty);
        chipAll = view.findViewById(R.id.chip_all);
        chipExpense = view.findViewById(R.id.chip_expense);
        chipIncome = view.findViewById(R.id.chip_income);
        chipFilter = view.findViewById(R.id.chip_filter);
    }

    private void setupRecyclerView() {
        adapter = new TransactionAdapter(transaction -> {
            Bundle args = new Bundle();
            args.putLong("transactionId", transaction.getId());
            Navigation.findNavController(requireView())
                    .navigate(R.id.transactionDetailFragment, args);
        });
        rvTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvTransactions.setAdapter(adapter);
    }

    private void setupListeners(View view) {
        FloatingActionButton fabAdd = view.findViewById(R.id.fab_add_transaction);
        fabAdd.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.addTransactionFragment);
        });

        chipAll.setOnClickListener(v -> viewModel.setFilter(TransactionsViewModel.FILTER_ALL));
        chipExpense.setOnClickListener(v -> viewModel.setFilter(TransactionsViewModel.FILTER_EXPENSE));
        chipIncome.setOnClickListener(v -> viewModel.setFilter(TransactionsViewModel.FILTER_INCOME));

        if (chipFilter != null) {
            chipFilter.setOnClickListener(v -> showFilterDialog());
        }
    }

    private void observeData() {
        viewModel.getFilteredTransactions().observe(getViewLifecycleOwner(), transactions -> {
            if (transactions != null && !transactions.isEmpty()) {
                adapter.submitList(transactions);
                rvTransactions.setVisibility(View.VISIBLE);
                layoutEmpty.setVisibility(View.GONE);
            } else {
                rvTransactions.setVisibility(View.GONE);
                layoutEmpty.setVisibility(View.VISIBLE);
            }
        });

        viewModel.getAllCategories().observe(getViewLifecycleOwner(), categories -> {
            if (categories != null) {
                categoriesList = categories;
            }
        });

        viewModel.getAllCards().observe(getViewLifecycleOwner(), cards -> {
            if (cards != null) {
                cardsList = cards;
            }
        });
    }

    private void showFilterDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_filter_transactions, null);

        Spinner spinnerCard = dialogView.findViewById(R.id.spinner_card_filter);
        Spinner spinnerCategory = dialogView.findViewById(R.id.spinner_category_filter);

        // کارت‌ها
        List<String> cardNames = new ArrayList<>();
        cardNames.add("همه کارت‌ها");
        for (BankCard card : cardsList) {
            cardNames.add(card.getBankName() + " - " + card.getCardNumber());
        }
        ArrayAdapter<String> cardAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, cardNames);
        cardAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCard.setAdapter(cardAdapter);

        // دسته‌بندی‌ها
        List<String> categoryNames = new ArrayList<>();
        categoryNames.add("همه دسته‌بندی‌ها");
        for (Category category : categoriesList) {
            categoryNames.add(category.getIcon() + " " + category.getName());
        }
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, categoryNames);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);

        dialogView.findViewById(R.id.btn_clear_filter).setOnClickListener(v -> {
            spinnerCard.setSelection(0);
            spinnerCategory.setSelection(0);
        });

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("فیلتر پیشرفته")
                .setView(dialogView)
                .setPositiveButton("اعمال", (dialog, which) -> {
                    // فیلتر کارت
                    int cardPos = spinnerCard.getSelectedItemPosition();
                    if (cardPos > 0) {
                        viewModel.setCardFilter(cardsList.get(cardPos - 1).getId());
                    } else {
                        viewModel.setCardFilter(-1);
                    }

                    // فیلتر دسته‌بندی
                    int catPos = spinnerCategory.getSelectedItemPosition();
                    if (catPos > 0) {
                        viewModel.setCategoryFilter(categoriesList.get(catPos - 1).getId());
                    } else {
                        viewModel.setCategoryFilter(-1);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .setNeutralButton("پاک کردن فیلتر", (dialog, which) -> {
                    viewModel.clearFilters();
                })
                .show();
    }
}

