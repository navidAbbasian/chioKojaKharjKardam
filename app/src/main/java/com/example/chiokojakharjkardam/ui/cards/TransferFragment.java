package com.example.chiokojakharjkardam.ui.cards;

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

import com.example.chiokojakharjkardam.R;
import com.example.chiokojakharjkardam.data.database.entity.BankCard;
import com.example.chiokojakharjkardam.data.database.entity.Transfer;
import com.example.chiokojakharjkardam.utils.CurrencyUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class TransferFragment extends Fragment {

    private TransferViewModel viewModel;

    private Spinner spinnerFromCard;
    private Spinner spinnerToCard;
    private TextInputEditText etAmount;
    private TextInputEditText etDescription;
    private MaterialButton btnTransfer;

    private List<BankCard> cardsList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_transfer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(TransferViewModel.class);

        initViews(view);
        setupListeners();
        observeData();
    }

    private void initViews(View view) {
        spinnerFromCard = view.findViewById(R.id.spinner_from_card);
        spinnerToCard = view.findViewById(R.id.spinner_to_card);
        etAmount = view.findViewById(R.id.et_amount);
        etDescription = view.findViewById(R.id.et_description);
        btnTransfer = view.findViewById(R.id.btn_transfer);
    }

    private void setupListeners() {
        btnTransfer.setOnClickListener(v -> doTransfer());
    }

    private void observeData() {
        viewModel.getAllCards().observe(getViewLifecycleOwner(), cards -> {
            if (cards != null && cards.size() >= 2) {
                cardsList = cards;
                List<String> cardNames = new ArrayList<>();
                for (BankCard card : cards) {
                    cardNames.add(card.getBankName() + " - " + card.getCardNumber());
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_spinner_item, cardNames);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerFromCard.setAdapter(adapter);
                spinnerToCard.setAdapter(adapter);

                if (cards.size() > 1) {
                    spinnerToCard.setSelection(1);
                }
            } else {
                Toast.makeText(requireContext(), "حداقل ۲ کارت برای انتقال نیاز است", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void doTransfer() {
        String amountStr = etAmount.getText().toString().trim();
        String description = etDescription.getText().toString().trim();

        if (amountStr.isEmpty()) {
            etAmount.setError("مبلغ را وارد کنید");
            return;
        }

        int fromIndex = spinnerFromCard.getSelectedItemPosition();
        int toIndex = spinnerToCard.getSelectedItemPosition();

        if (fromIndex == toIndex) {
            Toast.makeText(requireContext(), "کارت مبدأ و مقصد نمی‌توانند یکی باشند", Toast.LENGTH_SHORT).show();
            return;
        }

        if (fromIndex < 0 || toIndex < 0 || cardsList.isEmpty()) {
            Toast.makeText(requireContext(), "کارت‌ها را انتخاب کنید", Toast.LENGTH_SHORT).show();
            return;
        }

        long amount = CurrencyUtils.parseAmount(amountStr);
        BankCard fromCard = cardsList.get(fromIndex);
        BankCard toCard = cardsList.get(toIndex);

        if (fromCard.getBalance() < amount) {
            Toast.makeText(requireContext(), "موجودی کارت مبدأ کافی نیست", Toast.LENGTH_SHORT).show();
            return;
        }

        Transfer transfer = new Transfer(
                fromCard.getId(),
                toCard.getId(),
                amount,
                description.isEmpty() ? "انتقال موجودی" : description,
                System.currentTimeMillis()
        );

        viewModel.doTransfer(transfer);

        Toast.makeText(requireContext(), R.string.transfer_success, Toast.LENGTH_SHORT).show();
        Navigation.findNavController(requireView()).popBackStack();
    }
}

