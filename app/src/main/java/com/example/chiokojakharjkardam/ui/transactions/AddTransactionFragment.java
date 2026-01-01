package com.example.chiokojakharjkardam.ui.transactions;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

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
import com.example.chiokojakharjkardam.data.database.entity.Tag;
import com.example.chiokojakharjkardam.data.database.entity.Transaction;
import com.example.chiokojakharjkardam.ui.adapters.CategoryAdapter;
import com.example.chiokojakharjkardam.ui.components.PersianDatePickerDialog;
import com.example.chiokojakharjkardam.utils.CurrencyUtils;
import com.example.chiokojakharjkardam.utils.PersianDateUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class AddTransactionFragment extends Fragment {

    private AddTransactionViewModel viewModel;

    private MaterialButtonToggleGroup toggleType;
    private TextInputEditText etAmount;
    private TextInputEditText etDescription;
    private TextInputEditText etDate;
    private RecyclerView rvCategories;
    private Spinner spinnerCard;
    private ChipGroup chipGroupTags;
    private MaterialButton btnSave;

    private CategoryAdapter categoryAdapter;
    private List<BankCard> cardsList = new ArrayList<>();
    private List<Tag> tagsList = new ArrayList<>();
    private Category selectedCategory;
    private long selectedDate = System.currentTimeMillis();
    private List<Long> selectedTagIds = new ArrayList<>();
    private long editTransactionId = -1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_transaction, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(AddTransactionViewModel.class);

        // بررسی حالت ویرایش
        if (getArguments() != null) {
            editTransactionId = getArguments().getLong("transactionId", -1);
        }

        initViews(view);
        setupListeners();
        observeData();

        // تنظیم تاریخ امروز
        updateDateDisplay();
    }

    private void initViews(View view) {
        toggleType = view.findViewById(R.id.toggle_type);
        etAmount = view.findViewById(R.id.et_amount);
        etDescription = view.findViewById(R.id.et_description);
        etDate = view.findViewById(R.id.et_date);
        rvCategories = view.findViewById(R.id.rv_categories);
        spinnerCard = view.findViewById(R.id.spinner_card);
        chipGroupTags = view.findViewById(R.id.chip_group_tags);
        btnSave = view.findViewById(R.id.btn_save);

        // تنظیم RecyclerView دسته‌بندی‌ها
        categoryAdapter = new CategoryAdapter(category -> {
            selectedCategory = category;
            categoryAdapter.setSelectedCategory(category);
        });
        rvCategories.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvCategories.setAdapter(categoryAdapter);
    }

    private void setupListeners() {
        toggleType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                int type = (checkedId == R.id.btn_expense) ? Category.TYPE_EXPENSE : Category.TYPE_INCOME;
                viewModel.loadCategoriesByType(type);
            }
        });

        etDate.setOnClickListener(v -> showDatePicker());

        btnSave.setOnClickListener(v -> saveTransaction());
    }

    private void observeData() {
        // بارگذاری دسته‌بندی‌ها
        viewModel.getCategories().observe(getViewLifecycleOwner(), categories -> {
            if (categories != null) {
                categoryAdapter.submitList(categories);
                if (!categories.isEmpty() && selectedCategory == null) {
                    selectedCategory = categories.get(0);
                    categoryAdapter.setSelectedCategory(selectedCategory);
                }
            }
        });

        // بارگذاری کارت‌ها
        viewModel.getCards().observe(getViewLifecycleOwner(), cards -> {
            if (cards != null) {
                cardsList = cards;
                List<String> cardNames = new ArrayList<>();
                for (BankCard card : cards) {
                    cardNames.add(card.getBankName() + " - " + card.getCardNumber());
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_spinner_item, cardNames);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerCard.setAdapter(adapter);
            }
        });

        // بارگذاری تگ‌ها
        viewModel.getTags().observe(getViewLifecycleOwner(), tags -> {
            if (tags != null) {
                tagsList = tags;
                chipGroupTags.removeAllViews();
                for (Tag tag : tags) {
                    Chip chip = new Chip(requireContext());
                    chip.setText(tag.getName());
                    chip.setCheckable(true);
                    chip.setTag(tag.getId());
                    chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        long tagId = (long) buttonView.getTag();
                        if (isChecked) {
                            if (!selectedTagIds.contains(tagId)) {
                                selectedTagIds.add(tagId);
                            }
                        } else {
                            selectedTagIds.remove(tagId);
                        }
                    });
                    chipGroupTags.addView(chip);
                }
            }
        });

        // نتیجه اعتبارسنجی موجودی
        viewModel.getBalanceValidationResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null) {
                if (result.success) {
                    Toast.makeText(requireContext(), "تراکنش ذخیره شد", Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(requireView()).popBackStack();
                } else {
                    Toast.makeText(requireContext(), result.errorMessage, Toast.LENGTH_LONG).show();
                }
            }
        });

        // بارگذاری دسته‌بندی‌های خرج به صورت پیش‌فرض
        viewModel.loadCategoriesByType(Category.TYPE_EXPENSE);
    }

    private void showDatePicker() {
        PersianDatePickerDialog dialog = PersianDatePickerDialog.fromTimestamp(
                requireContext(),
                selectedDate,
                (year, month, day, timestamp) -> {
                    selectedDate = timestamp;
                    updateDateDisplay();
                }
        );
        dialog.show();
    }

    private void updateDateDisplay() {
        etDate.setText(PersianDateUtils.formatDate(selectedDate));
    }

    private void saveTransaction() {
        String amountStr = etAmount.getText().toString().trim();
        String description = etDescription.getText().toString().trim();

        if (amountStr.isEmpty()) {
            etAmount.setError("مبلغ را وارد کنید");
            return;
        }

        if (selectedCategory == null) {
            Toast.makeText(requireContext(), "دسته‌بندی را انتخاب کنید", Toast.LENGTH_SHORT).show();
            return;
        }

        if (cardsList.isEmpty() || spinnerCard.getSelectedItemPosition() < 0) {
            Toast.makeText(requireContext(), "کارت را انتخاب کنید", Toast.LENGTH_SHORT).show();
            return;
        }

        long amount = CurrencyUtils.parseAmount(amountStr);
        BankCard selectedCard = cardsList.get(spinnerCard.getSelectedItemPosition());
        int type = toggleType.getCheckedButtonId() == R.id.btn_expense
                ? Transaction.TYPE_EXPENSE
                : Transaction.TYPE_INCOME;

        Transaction transaction = new Transaction(
                selectedCard.getId(),
                selectedCategory.getId(),
                amount,
                type,
                description.isEmpty() ? selectedCategory.getName() : description,
                selectedDate
        );

        if (editTransactionId != -1) {
            transaction.setId(editTransactionId);
            viewModel.updateTransaction(transaction, selectedTagIds);
        } else {
            viewModel.insertTransaction(transaction, selectedTagIds);
        }
    }
}

