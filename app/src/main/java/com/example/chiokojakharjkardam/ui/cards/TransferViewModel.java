package com.example.chiokojakharjkardam.ui.cards;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.chiokojakharjkardam.data.database.entity.BankCard;
import com.example.chiokojakharjkardam.data.database.entity.Transfer;
import com.example.chiokojakharjkardam.data.repository.BankCardRepository;
import com.example.chiokojakharjkardam.data.repository.TransferRepository;

import java.util.List;

public class TransferViewModel extends AndroidViewModel {

    private final BankCardRepository bankCardRepository;
    private final TransferRepository transferRepository;
    private final LiveData<List<BankCard>> allCards;

    public TransferViewModel(@NonNull Application application) {
        super(application);
        bankCardRepository = new BankCardRepository(application);
        transferRepository = new TransferRepository(application);
        allCards = bankCardRepository.getAllCards();
    }

    public LiveData<List<BankCard>> getAllCards() {
        return allCards;
    }

    public void doTransfer(Transfer transfer) {
        transferRepository.doTransfer(transfer);
    }
}

