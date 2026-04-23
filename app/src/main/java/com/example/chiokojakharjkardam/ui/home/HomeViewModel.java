package com.example.chiokojakharjkardam.ui.home;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.example.chiokojakharjkardam.data.database.entity.BankCard;
import com.example.chiokojakharjkardam.data.database.entity.Transaction;
import com.example.chiokojakharjkardam.data.database.entity.TransactionListItem;
import com.example.chiokojakharjkardam.data.repository.BankCardRepository;
import com.example.chiokojakharjkardam.data.repository.TransactionRepository;
import com.example.chiokojakharjkardam.utils.PersianDateUtils;
import com.example.chiokojakharjkardam.utils.SessionManager;

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
    private final MediatorLiveData<List<TransactionListItem>> recentTransactions = new MediatorLiveData<>();

    public HomeViewModel(@NonNull Application application) {
        super(application);
        transactionRepository = new TransactionRepository(application);
        bankCardRepository = new BankCardRepository(application);

        totalBalance = bankCardRepository.getComputedTotalBalance();

        long[] monthRange = PersianDateUtils.getCurrentMonthRange();
        monthExpense = transactionRepository.getTotalExpenseInRange(monthRange[0], monthRange[1]);
        monthIncome = transactionRepository.getTotalIncomeInRange(monthRange[0], monthRange[1]);

        rawRecentTransactions = transactionRepository.getRecentTransactions(5);
        allCards = bankCardRepository.getAllCards();

        recentTransactions.addSource(rawRecentTransactions, t -> buildListItems());
        recentTransactions.addSource(allCards, c -> buildListItems());
    }

    private void buildListItems() {
        List<Transaction> txs = rawRecentTransactions.getValue();
        if (txs == null) { recentTransactions.setValue(null); return; }

        Map<Long, BankCard> cardMap = new HashMap<>();
        List<BankCard> cards = allCards.getValue();
        if (cards != null) for (BankCard c : cards) cardMap.put(c.getId(), c);

        List<TransactionListItem> items = new ArrayList<>();
        String currentUser = SessionManager.getInstance().getFullName();
        for (Transaction tx : txs) {
            BankCard card = cardMap.get(tx.getCardId());
            String cardName = card != null ? (card.getBankName() + " - " + card.getCardNumber()) : "";
            items.add(new TransactionListItem(tx, cardName, currentUser != null ? currentUser : ""));
        }
        recentTransactions.setValue(items);
    }

    public LiveData<Long> getTotalBalance() { return totalBalance; }
    public LiveData<Long> getMonthExpense() { return monthExpense; }
    public LiveData<Long> getMonthIncome() { return monthIncome; }
    public LiveData<List<TransactionListItem>> getRecentTransactions() { return recentTransactions; }
}
