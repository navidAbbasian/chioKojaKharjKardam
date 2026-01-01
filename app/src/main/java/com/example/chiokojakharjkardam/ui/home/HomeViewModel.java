package com.example.chiokojakharjkardam.ui.home;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.chiokojakharjkardam.data.database.entity.Transaction;
import com.example.chiokojakharjkardam.data.repository.BankCardRepository;
import com.example.chiokojakharjkardam.data.repository.TransactionRepository;
import com.example.chiokojakharjkardam.utils.PersianDateUtils;

import java.util.List;

public class HomeViewModel extends AndroidViewModel {

    private final TransactionRepository transactionRepository;
    private final BankCardRepository bankCardRepository;

    private final LiveData<Long> totalBalance;
    private final LiveData<Long> monthExpense;
    private final LiveData<Long> monthIncome;
    private final LiveData<List<Transaction>> recentTransactions;

    public HomeViewModel(@NonNull Application application) {
        super(application);
        transactionRepository = new TransactionRepository(application);
        bankCardRepository = new BankCardRepository(application);

        // محاسبه موجودی کل کارت‌ها
        totalBalance = bankCardRepository.getTotalBalance();

        // محاسبه خرج و درآمد این ماه
        long[] monthRange = PersianDateUtils.getCurrentMonthRange();
        monthExpense = transactionRepository.getTotalExpenseInRange(monthRange[0], monthRange[1]);
        monthIncome = transactionRepository.getTotalIncomeInRange(monthRange[0], monthRange[1]);

        // آخرین ۵ تراکنش
        recentTransactions = transactionRepository.getRecentTransactions(5);
    }

    public LiveData<Long> getTotalBalance() {
        return totalBalance;
    }

    public LiveData<Long> getMonthExpense() {
        return monthExpense;
    }

    public LiveData<Long> getMonthIncome() {
        return monthIncome;
    }

    public LiveData<List<Transaction>> getRecentTransactions() {
        return recentTransactions;
    }
}

