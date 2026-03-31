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
import com.example.chiokojakharjkardam.utils.ThousandSeparatorTextWatcher;
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
    private Spinner spinnerToCard;
    private View tvToCardLabel;
    private ChipGroup chipGroupTags;
    private MaterialButton btnSave;

    private CategoryAdapter categoryAdapter;
    private List<BankCard> cardsList = new ArrayList<>();
    private List<Tag> tagsList = new ArrayList<>();
    private Category selectedCategory;
    private long selectedDate = System.currentTimeMillis();
    private List<Long> selectedTagIds = new ArrayList<>();
    private long editTransactionId = -1;

    // تراکنشی که در حال ویرایش است
    private Transaction pendingEditTransaction = null;
    // آیا فرم ویرایش بارگذاری شده است
    private boolean editFormPopulated = false;

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

        // اگر در حالت ویرایش هستیم، تراکنش را بارگذاری کن
        if (editTransactionId != -1) {
            viewModel.loadTransactionForEdit(editTransactionId);
        } else {
            // تنظیم تاریخ امروز فقط برای حالت افزودن
            updateDateDisplay();
        }
    }

    private void initViews(View view) {
        toggleType = view.findViewById(R.id.toggle_type);
        etAmount = view.findViewById(R.id.et_amount);
        etDescription = view.findViewById(R.id.et_description);
        etDate = view.findViewById(R.id.et_date);
        rvCategories = view.findViewById(R.id.rv_categories);
        spinnerCard = view.findViewById(R.id.spinner_card);
        spinnerToCard = view.findViewById(R.id.spinner_to_card);
        tvToCardLabel = view.findViewById(R.id.tv_to_card_label);
        chipGroupTags = view.findViewById(R.id.chip_group_tags);
        btnSave = view.findViewById(R.id.btn_save);

        // تنظیم RecyclerView دسته‌بندی‌ها
        categoryAdapter = new CategoryAdapter(category -> {
            selectedCategory = category;
            categoryAdapter.setSelectedCategory(category);
        });
        rvCategories.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvCategories.setAdapter(categoryAdapter);

        // جداکننده هزارگان برای فیلد مبلغ
        etAmount.addTextChangedListener(new ThousandSeparatorTextWatcher(etAmount));
    }

    private void setupListeners() {
        toggleType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            // فقط در صورتی که در حال پر کردن فرم ویرایش نیستیم، دسته‌بندی ریست شود
            if (!editFormPopulated) {
                selectedCategory = null;
            }
            if (checkedId == R.id.btn_transfer) {
                tvToCardLabel.setVisibility(View.VISIBLE);
                spinnerToCard.setVisibility(View.VISIBLE);
                viewModel.loadAllCategories();
            } else {
                tvToCardLabel.setVisibility(View.GONE);
                spinnerToCard.setVisibility(View.GONE);
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
                if (pendingEditTransaction != null && pendingEditTransaction.getCategoryId() != null) {
                    // انتخاب دسته‌بندی مربوط به تراکنش در حال ویرایش
                    for (Category cat : categories) {
                        if (cat.getId() == pendingEditTransaction.getCategoryId()) {
                            selectedCategory = cat;
                            categoryAdapter.setSelectedCategory(cat);
                            break;
                        }
                    }
                } else if (!categories.isEmpty() && selectedCategory == null && pendingEditTransaction == null) {
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

                // کارت مقصد هم همین لیست رو داره
                ArrayAdapter<String> adapterTo = new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_spinner_item, cardNames);
                adapterTo.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerToCard.setAdapter(adapterTo);

                if (pendingEditTransaction != null) {
                    // انتخاب کارت مبدا تراکنش در حال ویرایش
                    for (int i = 0; i < cards.size(); i++) {
                        if (cards.get(i).getId() == pendingEditTransaction.getCardId()) {
                            spinnerCard.setSelection(i);
                            break;
                        }
                    }
                    // انتخاب کارت مقصد برای انتقال
                    if (pendingEditTransaction.getToCardId() != null) {
                        for (int i = 0; i < cards.size(); i++) {
                            if (cards.get(i).getId() == pendingEditTransaction.getToCardId()) {
                                spinnerToCard.setSelection(i);
                                break;
                            }
                        }
                    }
                } else {
                    // پیش‌فرض کارت مقصد: کارت دوم (اگر وجود داشته باشد)
                    if (cards.size() > 1) spinnerToCard.setSelection(1);
                }
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
                    // اگر تگ در لیست تگ‌های تراکنش در حال ویرایش است، تیک بزن
                    if (selectedTagIds.contains(tag.getId())) {
                        chip.setChecked(true);
                    }
                    chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        long tagId = (long) buttonView.getTag();
                        if (isChecked) {
                            if (!selectedTagIds.contains(tagId)) selectedTagIds.add(tagId);
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
                    Toast.makeText(requireContext(), R.string.transaction_saved, Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(requireView()).popBackStack();
                } else {
                    Toast.makeText(requireContext(), result.errorMessage, Toast.LENGTH_LONG).show();
                }
            }
        });

        // بارگذاری تراکنش برای ویرایش
        viewModel.getEditTransaction().observe(getViewLifecycleOwner(), transaction -> {
            if (transaction != null) {
                pendingEditTransaction = transaction;
                populateEditForm(transaction);
            }
        });

        // بارگذاری تگ‌های تراکنش برای ویرایش
        viewModel.getEditTransactionTagIds().observe(getViewLifecycleOwner(), tagIds -> {
            if (tagIds != null) {
                selectedTagIds.clear();
                selectedTagIds.addAll(tagIds);
                // به‌روزرسانی وضعیت چیپ‌های موجود
                for (int i = 0; i < chipGroupTags.getChildCount(); i++) {
                    View child = chipGroupTags.getChildAt(i);
                    if (child instanceof Chip) {
                        Chip chip = (Chip) child;
                        long chipTagId = (long) chip.getTag();
                        chip.setChecked(tagIds.contains(chipTagId));
                    }
                }
            }
        });

        // بارگذاری دسته‌بندی‌های خرج به صورت پیش‌فرض (فقط در حالت افزودن)
        if (editTransactionId == -1) {
            viewModel.loadCategoriesByType(Category.TYPE_EXPENSE);
        }
    }

    /**
     * پر کردن فرم با اطلاعات تراکنش موجود
     */
    private void populateEditForm(Transaction transaction) {
        editFormPopulated = true;

        // مقدار - با جداکننده هزارگان
        ThousandSeparatorTextWatcher.setFormattedAmount(etAmount, transaction.getAmount());

        // توضیحات
        etDescription.setText(transaction.getDescription());

        // تاریخ
        selectedDate = transaction.getDate();
        updateDateDisplay();

        // نوع تراکنش - تنظیم toggle (این خودش loadCategories رو صدا می‌زنه)
        if (transaction.getType() == Transaction.TYPE_INCOME) {
            toggleType.check(R.id.btn_income);
        } else if (transaction.getType() == Transaction.TYPE_TRANSFER) {
            toggleType.check(R.id.btn_transfer);
        } else {
            toggleType.check(R.id.btn_expense);
        }

        editFormPopulated = false;
    }

    private void showDatePicker() {
        PersianDatePickerDialog dialog = PersianDatePickerDialog.fromTimestamp(
                requireContext(), selectedDate,
                (year, month, day, timestamp) -> {
                    selectedDate = timestamp;
                    updateDateDisplay();
                });
        dialog.show();
    }

    private void updateDateDisplay() {
        etDate.setText(PersianDateUtils.formatDate(selectedDate));
    }

    private void saveTransaction() {
        String amountStr = etAmount.getText() != null ? etAmount.getText().toString().trim() : "";
        String description = etDescription.getText() != null ? etDescription.getText().toString().trim() : "";
        int checkedId = toggleType.getCheckedButtonId();

        if (amountStr.isEmpty()) {
            etAmount.setError(getString(R.string.amount_required));
            return;
        }

        if (cardsList.isEmpty() || spinnerCard.getSelectedItemPosition() < 0) {
            Toast.makeText(requireContext(), R.string.select_card_error, Toast.LENGTH_SHORT).show();
            return;
        }

        long amount = CurrencyUtils.parseAmount(amountStr);
        BankCard sourceCard = cardsList.get(spinnerCard.getSelectedItemPosition());

        if (checkedId == R.id.btn_transfer) {
            // ─── کارت به کارت ───
            if (cardsList.size() < 2) {
                Toast.makeText(requireContext(), R.string.need_two_cards, Toast.LENGTH_SHORT).show();
                return;
            }
            int toPos = spinnerToCard.getSelectedItemPosition();
            if (toPos < 0 || toPos >= cardsList.size()) {
                Toast.makeText(requireContext(), R.string.select_destination_card, Toast.LENGTH_SHORT).show();
                return;
            }
            BankCard destCard = cardsList.get(toPos);
            if (sourceCard.getId() == destCard.getId()) {
                Toast.makeText(requireContext(), R.string.same_card_error, Toast.LENGTH_SHORT).show();
                return;
            }

            Long categoryId = selectedCategory != null ? selectedCategory.getId() : null;
            Transaction transaction = new Transaction(
                    sourceCard.getId(), categoryId, amount,
                    Transaction.TYPE_TRANSFER,
                    description.isEmpty() ? getString(R.string.transfer_type) : description,
                    selectedDate
            );
            transaction.setToCardId(destCard.getId());

            if (editTransactionId != -1) {
                transaction.setId(editTransactionId);
                viewModel.updateTransaction(transaction, selectedTagIds);
            } else {
                viewModel.insertTransaction(transaction, selectedTagIds);
            }

        } else {
            // ─── خرج یا درآمد ───
            if (selectedCategory == null) {
                Toast.makeText(requireContext(), R.string.select_category_error, Toast.LENGTH_SHORT).show();
                return;
            }

            int type = (checkedId == R.id.btn_expense) ? Transaction.TYPE_EXPENSE : Transaction.TYPE_INCOME;
            Transaction transaction = new Transaction(
                    sourceCard.getId(), selectedCategory.getId(), amount, type,
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
}
