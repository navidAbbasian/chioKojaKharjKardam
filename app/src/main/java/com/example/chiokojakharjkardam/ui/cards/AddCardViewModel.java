package com.example.chiokojakharjkardam.ui.cards;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.chiokojakharjkardam.data.database.entity.BankCard;
import com.example.chiokojakharjkardam.data.database.entity.Member;
import com.example.chiokojakharjkardam.data.repository.BankCardRepository;
import com.example.chiokojakharjkardam.data.repository.MemberRepository;

import java.util.List;

public class AddCardViewModel extends AndroidViewModel {

    private final BankCardRepository bankCardRepository;
    private final MemberRepository memberRepository;

    private final LiveData<List<Member>> members;

    public AddCardViewModel(@NonNull Application application) {
        super(application);
        bankCardRepository = new BankCardRepository(application);
        memberRepository = new MemberRepository(application);

        members = memberRepository.getAllMembers();
    }

    public LiveData<List<Member>> getMembers() {
        return members;
    }

    public LiveData<BankCard> getCardById(long id) {
        return bankCardRepository.getCardById(id);
    }

    public void insertCard(BankCard card) {
        bankCardRepository.insert(card);
    }

    public void updateCard(BankCard card) {
        bankCardRepository.update(card);
    }
}

