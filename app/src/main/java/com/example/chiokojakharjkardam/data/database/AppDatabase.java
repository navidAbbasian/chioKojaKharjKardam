package com.example.chiokojakharjkardam.data.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.chiokojakharjkardam.data.database.dao.*;
import com.example.chiokojakharjkardam.data.database.entity.*;
import com.example.chiokojakharjkardam.data.database.dao.PendingDeleteDao;

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
        Transfer.class,
        PendingDelete.class
}, version = 6, exportSchema = false)
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
    public abstract PendingDeleteDao pendingDeleteDao();

    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE transactions ADD COLUMN toCardId INTEGER");
        }
    };

    /** v2 → v3: adds Supabase sync fields to families, members, categories, tags */
    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Family — remote UUID + invite code
            database.execSQL("ALTER TABLE families ADD COLUMN supabaseId TEXT");
            database.execSQL("ALTER TABLE families ADD COLUMN inviteCode TEXT");
            // Member — link to Supabase auth user
            database.execSQL("ALTER TABLE members ADD COLUMN userId TEXT");
            // Category + Tag — family scope (default 0; updated on first sync)
            database.execSQL("ALTER TABLE categories ADD COLUMN familyId INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE tags ADD COLUMN familyId INTEGER NOT NULL DEFAULT 0");
        }
    };

    /**
     * v3 → v4: offline-first support.
     * Adds supabaseId + pendingSync to transactions, bank_cards, categories, tags.
     * For existing rows (previously created online) supabaseId is set equal to id
     * (the old convention where local id == Supabase id), so they are treated as synced.
     * Creates the pending_deletes tombstone table.
     */
    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // transactions
            database.execSQL("ALTER TABLE transactions ADD COLUMN supabaseId INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE transactions ADD COLUMN pendingSync INTEGER NOT NULL DEFAULT 0");
            database.execSQL("UPDATE transactions SET supabaseId = id WHERE id > 0");

            // bank_cards
            database.execSQL("ALTER TABLE bank_cards ADD COLUMN supabaseId INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE bank_cards ADD COLUMN pendingSync INTEGER NOT NULL DEFAULT 0");
            database.execSQL("UPDATE bank_cards SET supabaseId = id WHERE id > 0");

            // categories
            database.execSQL("ALTER TABLE categories ADD COLUMN supabaseId INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE categories ADD COLUMN pendingSync INTEGER NOT NULL DEFAULT 0");
            database.execSQL("UPDATE categories SET supabaseId = id WHERE id > 0");

            // tags
            database.execSQL("ALTER TABLE tags ADD COLUMN supabaseId INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE tags ADD COLUMN pendingSync INTEGER NOT NULL DEFAULT 0");
            database.execSQL("UPDATE tags SET supabaseId = id WHERE id > 0");

            // tombstone table for offline deletes
            database.execSQL("CREATE TABLE IF NOT EXISTS `pending_deletes` " +
                    "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`entityType` TEXT, " +
                    "`supabaseId` INTEGER NOT NULL)");
        }
    };

    /**
     * v4 → v5: Transaction supabaseId changes from INTEGER to TEXT (UUID support).
     * PendingDelete supabaseId also changes to TEXT to accommodate UUID transaction IDs.
     */
    static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Recreate transactions table with supabaseId as TEXT
            database.execSQL("CREATE TABLE IF NOT EXISTS `transactions_new` " +
                    "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`cardId` INTEGER NOT NULL, " +
                    "`categoryId` INTEGER, " +
                    "`amount` INTEGER NOT NULL, " +
                    "`type` INTEGER NOT NULL, " +
                    "`toCardId` INTEGER, " +
                    "`description` TEXT, " +
                    "`date` INTEGER NOT NULL, " +
                    "`createdAt` INTEGER NOT NULL, " +
                    "`supabaseId` TEXT, " +
                    "`pendingSync` INTEGER NOT NULL DEFAULT 0, " +
                    "FOREIGN KEY(`cardId`) REFERENCES `bank_cards`(`id`) ON DELETE CASCADE, " +
                    "FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON DELETE SET NULL)");

            database.execSQL("INSERT INTO transactions_new " +
                    "(id, cardId, categoryId, amount, type, toCardId, description, date, createdAt, supabaseId, pendingSync) " +
                    "SELECT id, cardId, categoryId, amount, type, toCardId, description, date, createdAt, " +
                    "CASE WHEN supabaseId > 0 THEN CAST(supabaseId AS TEXT) ELSE NULL END, pendingSync " +
                    "FROM transactions");

            database.execSQL("DROP TABLE transactions");
            database.execSQL("ALTER TABLE transactions_new RENAME TO transactions");

            // Recreate indices
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_cardId` ON `transactions` (`cardId`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_categoryId` ON `transactions` (`categoryId`)");

            // Recreate pending_deletes with supabaseId as TEXT
            database.execSQL("CREATE TABLE IF NOT EXISTS `pending_deletes_new` " +
                    "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`entityType` TEXT, " +
                    "`supabaseId` TEXT)");

            database.execSQL("INSERT INTO pending_deletes_new (id, entityType, supabaseId) " +
                    "SELECT id, entityType, CAST(supabaseId AS TEXT) FROM pending_deletes");

            database.execSQL("DROP TABLE pending_deletes");
            database.execSQL("ALTER TABLE pending_deletes_new RENAME TO pending_deletes");
        }
    };

    /**
     * v5 → v6: Add initialBalance to bank_cards for balance recalculation.
     * Add unique index on members.userId to prevent duplicate members.
     */
    static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Add initialBalance column; default to current balance
            database.execSQL("ALTER TABLE bank_cards ADD COLUMN initialBalance INTEGER NOT NULL DEFAULT 0");
            database.execSQL("UPDATE bank_cards SET initialBalance = balance");

            // Deduplicate members by userId before creating unique index
            // Keep the member with the lowest id for each userId
            database.execSQL("DELETE FROM members WHERE id NOT IN " +
                    "(SELECT MIN(id) FROM members GROUP BY userId)");

            // Create unique index on userId (allows NULL duplicates in SQLite)
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_members_userId` ON `members` (`userId`)");
        }
    };

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "chio_koja_kharj_kardam_db")
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
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
