package com.example.chiokojakharjkardam.data.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.chiokojakharjkardam.data.database.dao.*;
import com.example.chiokojakharjkardam.data.database.entity.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {
        Family.class,
        Member.class,
        BankCard.class,
        Category.class,
        Tag.class,
        Transaction.class,
        TransactionTag.class,
        Bill.class,
        Transfer.class
}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public abstract FamilyDao familyDao();
    public abstract MemberDao memberDao();
    public abstract BankCardDao bankCardDao();
    public abstract CategoryDao categoryDao();
    public abstract TagDao tagDao();
    public abstract TransactionDao transactionDao();
    public abstract TransactionTagDao transactionTagDao();
    public abstract BillDao billDao();
    public abstract TransferDao transferDao();

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "chio_koja_kharj_kardam_db")
                            .addCallback(sRoomDatabaseCallback)
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    public static void closeDatabase() {
        if (INSTANCE != null) {
            if (INSTANCE.isOpen()) {
                INSTANCE.close();
            }
            INSTANCE = null;
        }
    }

    private static final RoomDatabase.Callback sRoomDatabaseCallback = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            databaseWriteExecutor.execute(() -> {
                CategoryDao categoryDao = INSTANCE.categoryDao();

                // دسته‌بندی‌های پیش‌فرض - خرج
                categoryDao.insert(new Category("تغذیه", "", "#4CAF50", Category.TYPE_EXPENSE, true));
                categoryDao.insert(new Category("حمل‌ونقل", "", "#2196F3", Category.TYPE_EXPENSE, true));
                categoryDao.insert(new Category("قبوض", "", "#FF9800", Category.TYPE_EXPENSE, true));
                categoryDao.insert(new Category("مسکن", "", "#9C27B0", Category.TYPE_EXPENSE, true));
                categoryDao.insert(new Category("پوشاک", "", "#E91E63", Category.TYPE_EXPENSE, true));
                categoryDao.insert(new Category("سلامت", "", "#00BCD4", Category.TYPE_EXPENSE, true));
                categoryDao.insert(new Category("تفریح", "", "#FF5722", Category.TYPE_EXPENSE, true));
                categoryDao.insert(new Category("آموزش", "", "#3F51B5", Category.TYPE_EXPENSE, true));

                // دسته‌بندی‌های پیش‌فرض - هردو
                categoryDao.insert(new Category("هدیه", "", "#F44336", Category.TYPE_BOTH, true));
                categoryDao.insert(new Category("سایر", "", "#607D8B", Category.TYPE_BOTH, true));

                // دسته‌بندی‌های پیش‌فرض - درآمد
                categoryDao.insert(new Category("حقوق", "", "#4CAF50", Category.TYPE_INCOME, true));
                categoryDao.insert(new Category("سود", "", "#8BC34A", Category.TYPE_INCOME, true));
            });
        }
    };
}

