package com.example.chiokojakharjkardam.ui.home;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.example.chiokojakharjkardam.data.database.entity.BankCard;
import com.example.chiokojakharjkardam.data.database.entity.Member;
import com.example.chiokojakharjkardam.data.database.entity.Transaction;
import com.example.chiokojakharjkardam.data.database.entity.TransactionListItem;
import com.example.chiokojakharjkardam.data.repository.BankCardRepository;
import com.example.chiokojakharjkardam.data.repository.MemberRepository;
import com.example.chiokojakharjkardam.data.repository.TransactionRepository;
import com.example.chiokojakharjkardam.utils.PersianDateUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeViewModel extends AndroidViewModel {

    private final TransactionRepository transactionRepository;
    private final BankCardRepository bankCardRepository;

    private final LiveData<Long> totalBalance;
    private final LiveData<Long> monthExpense;
    private final LiveData<Long> monthIncome;
    private final LiveData<List<Transaction>> rawRecentTransactions;
    private final LiveData<List<BankCard>> allCards;
    private final LiveData<List<Member>> allMembers;
    private final MediatorLiveData<List<TransactionListItem>> recentTransactions = new MediatorLiveData<>();

    public HomeViewModel(@NonNull Application application) {
        super(application);
        transactionRepository = new TransactionRepository(application);
        bankCardRepository = new BankCardRepository(application);
        MemberRepository memberRepository = new MemberRepository(application);

        totalBalance = bankCardRepository.getComputedTotalBalance();

        long[] monthRange = PersianDateUtils.getCurrentMonthRange();
        monthExpense = transactionRepository.getTotalExpenseInRange(monthRange[0], monthRange[1]);
        monthIncome = transactionRepository.getTotalIncomeInRange(monthRange[0], monthRange[1]);

        rawRecentTransactions = transactionRepository.getRecentTransactions(5);
        allCards = bankCardRepository.getAllCards();
        allMembers = memberRepository.getAllMembers();

        recentTransactions.addSource(rawRecentTransactions, t -> buildListItems());
        recentTransactions.addSource(allCards, c -> buildListItems());
        recentTransactions.addSource(allMembers, m -> buildListItems());
    }

    private void buildListItems() {
        List<Transaction> txs = rawRecentTransactions.getValue();
        if (txs == null) { recentTransactions.setValue(null); return; }

        Map<Long, BankCard> cardMap = new HashMap<>();
        List<BankCard> cards = allCards.getValue();
        if (cards != null) for (BankCard c : cards) cardMap.put(c.getId(), c);

        Map<Long, String> memberNames = new HashMap<>();
        List<Member> members = allMembers.getValue();
        if (members != null) for (Member m : members) memberNames.put(m.getId(), m.getName());

        List<TransactionListItem> items = new ArrayList<>();
        for (Transaction tx : txs) {
            BankCard card = cardMap.get(tx.getCardId());
            String cardName = card != null ? (card.getBankName() + " - " + card.getCardNumber()) : "";
            String memberName = card != null ? memberNames.get(card.getMemberId()) : null;
            items.add(new TransactionListItem(tx, cardName, memberName != null ? memberName : ""));
        }
        recentTransactions.setValue(items);
    }

    public LiveData<Long> getTotalBalance() { return totalBalance; }
    public LiveData<Long> getMonthExpense() { return monthExpense; }
    public LiveData<Long> getMonthIncome() { return monthIncome; }
    public LiveData<List<TransactionListItem>> getRecentTransactions() { return recentTransactions; }
}
