package com.example.chiokojakharjkardam.ui.cards;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Spinner;
import android.widget.TextView;
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
import com.example.chiokojakharjkardam.data.database.entity.Member;
import com.example.chiokojakharjkardam.ui.adapters.ColorAdapter;
import com.example.chiokojakharjkardam.utils.Constants;
import com.example.chiokojakharjkardam.utils.CurrencyUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class AddCardFragment extends Fragment {

    private AddCardViewModel viewModel;

    private MaterialCardView cardPreview;
    private TextView tvPreviewBank;
    private TextView tvPreviewNumber;
    private TextView tvPreviewHolder;
    private TextView tvPreviewBalance;
    private Spinner spinnerMember;
    private AutoCompleteTextView etBankName;
    private TextInputEditText etCardNumber;
    private TextInputEditText etCardHolder;
    private TextInputEditText etBalance;
    private RecyclerView rvColors;
    private MaterialButton btnSave;

    private List<Member> membersList = new ArrayList<>();
    private String selectedColor = Constants.CARD_COLORS[0];
    private long editCardId = -1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_card, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(AddCardViewModel.class);

        if (getArguments() != null) {
            editCardId = getArguments().getLong("cardId", -1);
        }

        initViews(view);
        setupListeners();
        observeData();
    }

    private void initViews(View view) {
        cardPreview = view.findViewById(R.id.card_preview);
        tvPreviewBank = view.findViewById(R.id.tv_preview_bank);
        tvPreviewNumber = view.findViewById(R.id.tv_preview_number);
        tvPreviewHolder = view.findViewById(R.id.tv_preview_holder);
        tvPreviewBalance = view.findViewById(R.id.tv_preview_balance);
        spinnerMember = view.findViewById(R.id.spinner_member);
        etBankName = view.findViewById(R.id.et_bank_name);
        etCardNumber = view.findViewById(R.id.et_card_number);
        etCardHolder = view.findViewById(R.id.et_card_holder);
        etBalance = view.findViewById(R.id.et_balance);
        rvColors = view.findViewById(R.id.rv_colors);
        btnSave = view.findViewById(R.id.btn_save);

        // تنظیم لیست بانک‌ها
        ArrayAdapter<String> bankAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, Constants.BANK_NAMES);
        etBankName.setAdapter(bankAdapter);

        // تنظیم انتخاب رنگ
        ColorAdapter colorAdapter = new ColorAdapter(Constants.CARD_COLORS, color -> {
            selectedColor = color;
            updatePreview();
        });
        rvColors.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvColors.setAdapter(colorAdapter);
    }

    private void setupListeners() {
        TextWatcher previewWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updatePreview();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        etBankName.addTextChangedListener(previewWatcher);
        etCardNumber.addTextChangedListener(previewWatcher);
        etCardHolder.addTextChangedListener(previewWatcher);
        etBalance.addTextChangedListener(previewWatcher);

        btnSave.setOnClickListener(v -> saveCard());
    }

    private void updatePreview() {
        String bankName = etBankName.getText().toString().trim();
        String cardNumber = etCardNumber.getText().toString().trim();
        String cardHolder = etCardHolder.getText().toString().trim();
        String balance = etBalance.getText().toString().trim();

        tvPreviewBank.setText(bankName.isEmpty() ? "نام بانک" : bankName);
        tvPreviewNumber.setText(cardNumber.isEmpty() ? "**** ****" : "**** " + cardNumber);
        tvPreviewHolder.setText(cardHolder.isEmpty() ? "نام صاحب کارت" : cardHolder);

        long balanceAmount = CurrencyUtils.parseAmount(balance);
        tvPreviewBalance.setText(CurrencyUtils.formatToman(balanceAmount));

        try {
            cardPreview.setCardBackgroundColor(Color.parseColor(selectedColor));
        } catch (Exception e) {
            cardPreview.setCardBackgroundColor(Color.parseColor("#1A237E"));
        }
    }

    private void observeData() {
        viewModel.getMembers().observe(getViewLifecycleOwner(), members -> {
            if (members != null) {
                membersList = members;
                List<String> memberNames = new ArrayList<>();
                for (Member member : members) {
                    memberNames.add(member.getName());
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_spinner_item, memberNames);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerMember.setAdapter(adapter);
            }
        });

        // اگر در حالت ویرایش هستیم، کارت را بارگذاری کن
        if (editCardId != -1) {
            viewModel.getCardById(editCardId).observe(getViewLifecycleOwner(), card -> {
                if (card != null) {
                    etBankName.setText(card.getBankName());
                    etCardNumber.setText(card.getCardNumber());
                    etCardHolder.setText(card.getCardHolderName());
                    etBalance.setText(String.valueOf(card.getBalance()));
                    selectedColor = card.getColor();
                    updatePreview();
                }
            });
        }
    }

    private void saveCard() {
        String bankName = etBankName.getText().toString().trim();
        String cardNumber = etCardNumber.getText().toString().trim();
        String cardHolder = etCardHolder.getText().toString().trim();
        String balanceStr = etBalance.getText().toString().trim();

        if (bankName.isEmpty()) {
            etBankName.setError("نام بانک را وارد کنید");
            return;
        }

        if (cardNumber.isEmpty() || cardNumber.length() != 4) {
            etCardNumber.setError("۴ رقم آخر کارت را وارد کنید");
            return;
        }

        if (cardHolder.isEmpty()) {
            etCardHolder.setError("نام صاحب کارت را وارد کنید");
            return;
        }

        if (membersList.isEmpty() || spinnerMember.getSelectedItemPosition() < 0) {
            Toast.makeText(requireContext(), "عضو را انتخاب کنید", Toast.LENGTH_SHORT).show();
            return;
        }

        long balance = CurrencyUtils.parseAmount(balanceStr);
        Member selectedMember = membersList.get(spinnerMember.getSelectedItemPosition());

        BankCard card = new BankCard(
                selectedMember.getId(),
                bankName,
                cardNumber,
                cardHolder,
                balance,
                selectedColor
        );

        if (editCardId != -1) {
            card.setId(editCardId);
            viewModel.updateCard(card);
        } else {
            viewModel.insertCard(card);
        }

        Toast.makeText(requireContext(), "کارت ذخیره شد", Toast.LENGTH_SHORT).show();
        Navigation.findNavController(requireView()).popBackStack();
    }
}

