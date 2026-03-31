package com.example.chiokojakharjkardam.ui.transactions;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.chiokojakharjkardam.data.database.AppDatabase;
import com.example.chiokojakharjkardam.data.database.entity.BankCard;
import com.example.chiokojakharjkardam.data.database.entity.Category;
import com.example.chiokojakharjkardam.data.database.entity.Tag;
import com.example.chiokojakharjkardam.data.database.entity.Transaction;
import com.example.chiokojakharjkardam.data.repository.BankCardRepository;
import com.example.chiokojakharjkardam.data.repository.CategoryRepository;
import com.example.chiokojakharjkardam.data.repository.TagRepository;
import com.example.chiokojakharjkardam.data.repository.TransactionRepository;

import java.util.List;

public class AddTransactionViewModel extends AndroidViewModel {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final BankCardRepository bankCardRepository;
    private final TagRepository tagRepository;
    private final AppDatabase db;

    private final MutableLiveData<List<Category>> categories = new MutableLiveData<>();
    private final LiveData<List<BankCard>> cards;
    private final LiveData<List<Tag>> tags;

    private final MutableLiveData<Transaction> editTransaction = new MutableLiveData<>();
    private final MutableLiveData<List<Long>> editTransactionTagIds = new MutableLiveData<>();

    // نتیجه اعتبارسنجی موجودی
    private final MutableLiveData<BalanceValidationResult> balanceValidationResult = new MutableLiveData<>();

    public AddTransactionViewModel(@NonNull Application application) {
        super(application);
        transactionRepository = new TransactionRepository(application);
        categoryRepository = new CategoryRepository(application);
        bankCardRepository = new BankCardRepository(application);
        tagRepository = new TagRepository(application);
        db = AppDatabase.getDatabase(application);

        cards = bankCardRepository.getAllCards();
        tags = tagRepository.getAllTags();
    }

    public void loadCategoriesByType(int type) {
        categoryRepository.getCategoriesByType(type).observeForever(categories::setValue);
    }

    public void loadAllCategories() {
        categoryRepository.getAllCategories().observeForever(categories::setValue);
    }

    public LiveData<List<Category>> getCategories() {
        return categories;
    }

    public LiveData<List<BankCard>> getCards() {
        return cards;
    }

    public LiveData<List<Tag>> getTags() {
        return tags;
    }

    public LiveData<BalanceValidationResult> getBalanceValidationResult() {
        return balanceValidationResult;
    }

    public LiveData<Transaction> getEditTransaction() {
        return editTransaction;
    }

    public LiveData<List<Long>> getEditTransactionTagIds() {
        return editTransactionTagIds;
    }

    public void loadTransactionForEdit(long transactionId) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            Transaction t = db.transactionDao().getTransactionByIdSync(transactionId);
            editTransaction.postValue(t);
            List<Long> tagIds = db.transactionTagDao().getTagIdsByTransaction(transactionId);
            editTransactionTagIds.postValue(tagIds);
        });
    }

    public void insertTransaction(Transaction transaction, List<Long> tagIds) {
        transactionRepository.insertWithBalanceUpdate(transaction, tagIds, id -> {
            balanceValidationResult.postValue(new BalanceValidationResult(true, null));
        }, errorMessage -> {
            balanceValidationResult.postValue(new BalanceValidationResult(false, errorMessage));
        });
    }

    public void updateTransaction(Transaction transaction, List<Long> tagIds) {
        transactionRepository.update(transaction, tagIds, () -> {
            balanceValidationResult.postValue(new BalanceValidationResult(true, null));
        }, errorMessage -> {
            balanceValidationResult.postValue(new BalanceValidationResult(false, errorMessage));
        });
    }

    /**
     * کلاس برای نگهداری نتیجه اعتبارسنجی موجودی
     */
    public static class BalanceValidationResult {
        public final boolean success;
        public final String errorMessage;

        public BalanceValidationResult(boolean success, String errorMessage) {
            this.success = success;
            this.errorMessage = errorMessage;
        }
    }
}
