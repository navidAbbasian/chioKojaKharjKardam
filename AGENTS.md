# AGENTS.md — ChioKojaKharjKardam

Persian-language family expense-tracking Android app. **Supabase-backed** — offline-first with optional cloud sync; no Firebase.

---

## Architecture

**MVVM + Room**, single `app/` module, written entirely in **Java** (Gradle uses Kotlin DSL).

```
UI (Fragment/Activity)
  └─ ViewModel  (androidx.lifecycle)
       └─ Repository  (data/repository/)
            └─ DAO  (data/database/dao/)
                 └─ Room DB  (AppDatabase, chio_koja_kharj_kardam_db)
```

- `App.java` — Application subclass; initialises `SessionManager`, `NetworkMonitor`, `SyncManager`, notification channel, and saved theme on startup (in that order).
- `AuthActivity` — Login/Register container; hosts `LoginFragment` and `RegisterFragment`; shared `AuthViewModel` drives navigation.
- `FamilySetupActivity` — Post-auth step to create or join a family (`CreateFamilyFragment` / `JoinFamilyFragment`); offers to migrate existing local data to Supabase via `DataMigrationManager`.
- `MainActivity` — single Activity; on `onCreate` checks `SessionManager.isLoggedIn()` and `session.hasFamilyId()` and redirects to `AuthActivity` / `FamilySetupActivity` when needed; calls `SyncManager.syncAll()` on every `onResume`; shows an offline banner via `NetworkMonitor`.
- `SetupActivity` — **legacy**, offline-only onboarding (still present in `ui/setup/`; superseded by the auth flow above).
- Navigation is handled by `NavController` + `BottomNavigationView`; all screens are Fragments defined in `res/navigation/nav_graph.xml`.

---

## Key Packages

| Package | Contents |
|---------|----------|
| `data/database/entity/` | Room entities: `Family`, `Member`, `BankCard`, `Category`, `Tag`, `Transaction`, `TransactionTag`, `Bill`, `Transfer`, `PendingDelete`; query result POJOs: `TransactionDetail`, `CategoryReport`, `TagReport`, `CombinedReport`; display DTO: `TransactionListItem` (transaction + `cardName` + `memberName`, used by `TransactionAdapter`) |
| `data/database/dao/` | One DAO per entity, including `PendingDeleteDao` |
| `data/repository/` | One Repository per domain area; constructed with `Application` |
| `data/remote/` | Supabase layer: `SupabaseAuthService` (Retrofit interface for `auth/v1`), `SupabaseRestService` (Retrofit interface for `rest/v1`), `RemoteDataSource` (high-level operations), `model/` (remote DTOs — `RemoteFamily`, `RemoteBankCard`, `RemoteProfile`, `RemoteTransactionTag`, etc.) |
| `ui/<screen>/` | One subfolder per screen, each containing `*Fragment` + `*ViewModel`; includes `auth/`, `family/`, `reports/`, `sync/` (`CloudSyncFragment` + `CloudSyncViewModel` — manual sync UI with progress), `settings/` (includes `ClearDataFragment`), `transactions/` (includes `TransactionDetailFragment` + `TransactionDetailViewModel`) |
| `ui/adapters/` | Shared RecyclerView adapters |
| `ui/components/` | `PersianDatePickerDialog` — reusable custom Jalali date picker |
| `utils/` | Stateless helpers (see below); also contains `DataMigrationManager`, `BillReminderReceiver`, `SyncLogger` (thread-safe singleton; appends to `{filesDir}/sync_log.txt`, rotates at 2 MB) |

---

## Critical Conventions

### Database writes
All writes must go through `AppDatabase.databaseWriteExecutor` (4-thread pool). Never write on the main thread.

```java
AppDatabase.databaseWriteExecutor.execute(() -> { dao.insert(entity); });
```

Repositories that modify card balances (e.g. `TransactionRepository.insertWithBalanceUpdate`) bundle balance-check + insert + balance-update atomically on the executor.

### Transaction types
```java
Transaction.TYPE_EXPENSE  = 0   // خرج
Transaction.TYPE_INCOME   = 1   // درآمد
Transaction.TYPE_TRANSFER = 2   // کارت به کارت (requires toCardId)
```

**Offline sync status** (`pendingSync` field — present on `Transaction`, `BankCard`, `Category`, `Tag`):
```java
Transaction.SYNC_DONE   = 0   // synced with Supabase
Transaction.SYNC_NEW    = 1   // created offline, needs upload
Transaction.SYNC_UPDATE = 2   // edited offline, needs push
```
Set `pendingSync = SYNC_NEW` on local insert; set `SYNC_UPDATE` on local edit when offline. After successful upload set it back to `SYNC_DONE`.

### Category types
```java
Category.TYPE_EXPENSE = 0  // خرج
Category.TYPE_INCOME  = 1  // درآمد
Category.TYPE_BOTH    = 2  // هردو (valid for both expense and income)
```

### Currency
- Stored internally as **Rial** (`long`).
- Displayed as **Toman** (`amount / 10`) via `CurrencyUtils.formatAmountWithToman()`.
- User inputs use `ThousandSeparatorTextWatcher` for live comma formatting.
- Parse user input with `CurrencyUtils.parseAmount()` (handles Persian digits).
- To pre-populate an amount field (e.g. edit mode): `ThousandSeparatorTextWatcher.setFormattedAmount(editText, amountInRial)` — handles formatting and cursor position.

### Dates — Jalali (Shamsi) calendar only
All dates are stored as Unix milliseconds. Display and input always use the Jalali calendar:
- Convert: `PersianDateUtils.gregorianToJalali()` / `jalaliToGregorian()`
- Format for display: `PersianDateUtils.formatDate(millis)` → e.g. `۱۴۰۳/۰۱/۱۵`
- Persian digit conversion: `PersianDateUtils.toPersianDigits(string)`
- Date picker: `PersianDatePickerDialog` (`ui/components/`)

### Colors
Predefined palettes live in `Constants.java`: `MEMBER_COLORS`, `CARD_COLORS`, `TAG_COLORS`. Use these arrays as the canonical source for color pickers (see `ColorAdapter`).

### SharedPreferences
Key constants: `Constants.PREF_NAME`, `PREF_FIRST_RUN`, `PREF_FAMILY_CREATED`, `PREF_DARK_MODE`.
`SessionManager` uses its own separate `"supabase_session"` SharedPreferences file — do **not** mix it with `Constants.PREF_NAME`.

### Dark/Light theme
`ThemeManager` wraps `AppCompatDelegate.setDefaultNightMode()`. Call `themeManager.applySavedTheme()` in `App.onCreate` (already done).

---

## Supabase Integration

`SupabaseClient` is a singleton that owns the Retrofit/OkHttp instances. It injects `apikey` + `Authorization: Bearer <token>` headers on every request via `SupabaseInterceptor`, which also silently refreshes an expired JWT before the call proceeds.

- `Constants.SUPABASE_URL` and `Constants.SUPABASE_ANON_KEY` are the only config values needed.
- `SupabaseAuthService` targets `auth/v1/` endpoints (`/signup`, `/token`).
- `SupabaseRestService` targets `rest/v1/` PostgREST endpoints; all filter params use the `"eq.VALUE"` prefix (e.g., `@Query("family_id") String filter` → pass `"eq.UUID"`).
- Use `Prefer: return=representation` header on `POST`/`PATCH` to get the record back in the response body.
- RLS-protected tables that need to be queried before the user has a `family_id` (e.g., join-by-invite-code) must use a `SECURITY DEFINER` RPC function (see `SupabaseRestService.getFamilyByInviteCode`).
- `RemoteDataSource` is a singleton façade over `SupabaseRestService`; always use it from repositories and `SyncManager` — never call the Retrofit interfaces directly.

### Sync

`SyncManager` performs a **two-phase sync** on every `syncAll()` call:

- **Phase 1 — upload pending local changes** (`uploadPending`): processes `PendingDelete` tombstones first (calls Supabase DELETE), then uploads `SYNC_NEW` and `SYNC_UPDATE` records in dependency order: categories → tags → bank_cards → transactions.
- **Phase 2 — pull from Supabase** (replaces local Room cache atomically per table):

```
syncMembers → syncBankCards → syncCategories → syncTags
  → syncTransactions → syncBills → syncTransfers
```

Call `SyncManager.getInstance().syncAll()` after auth and on every `Activity.onResume`. Sync is a no-op when offline — UI reads from Room.

`SyncManager` also exposes granular APIs used by `CloudSyncFragment`/`CloudSyncViewModel`:
- `checkSyncStatus(SyncStatusCallback)` — counts pending uploads/downloads without executing them; callback delivers a `SyncStatusReport`.
- `uploadWithProgress(SyncProgressCallback, SyncResultCallback)` — upload-only phase with incremental progress; delivers `SyncResult`.
- `downloadWithProgress(SyncProgressCallback, SyncResultCallback)` — download-only phase with incremental progress; delivers `SyncResult`.
- `SyncResult.needsRelogin` — set to `true` when a 401 is encountered; observers in `CloudSyncViewModel` redirect to `AuthActivity`.
- A `manualSyncInProgress` flag prevents concurrent manual + automatic sync runs.

`NetworkMonitor` wraps `ConnectivityManager.NetworkCallback`. Observe `NetworkMonitor.getInstance().isConnected()` (a `LiveData<Boolean>`) in Activities/Fragments for the offline banner. Use `NetworkMonitor.getInstance().isOnline()` for synchronous offline checks in repository/executor code.

### Auth flow

```
AuthActivity (LoginFragment / RegisterFragment)
  └─ on success → hasFamilyId? → MainActivity
                              → FamilySetupActivity (CreateFamily / JoinFamily)
                                   └─ DataMigrationManager (optional one-time upload)
                                        └─ MainActivity
```

`DataMigrationManager` (`utils/DataMigrationManager.java`) uploads existing local Room data to Supabase in FK-dependency order. It is invoked once from `FamilySetupActivity` when the owner has pre-existing local data.

---

## PDF Export

`PdfExportManager.exportTransactionsToPdf(context, uri, details, title, dateRange, callback)` writes a multi-page A4-Landscape PDF to a user-chosen `Uri` via `ActivityResultContracts.StartActivityForResult`. Column layout (width in pt): row# 30 | date 90 | description 175 | category 130 | tags 155 | type 60 | amount 130. Triggered from `ReportsFragment`.

`ReportsViewModel` exposes filter constants used by `ReportsFragment`:
```java
// Grouping
GROUP_BY_CATEGORY = 0 | GROUP_BY_TAG = 1 | GROUP_BY_COMBINED = 2

// Transaction type filter
TRANSACTION_TYPE_EXPENSE = 0 | TRANSACTION_TYPE_INCOME = 1
TRANSACTION_TYPE_ALL = 2     | TRANSACTION_TYPE_TRANSFER = 3

// Date range
DATE_RANGE_THIS_MONTH = 0 | DATE_RANGE_LAST_3_MONTHS = 1
DATE_RANGE_LAST_YEAR = 2  | DATE_RANGE_CUSTOM = 3
```

---

## Bill Reminders
`BillReminderScheduler` uses `AlarmManager.setExactAndAllowWhileIdle`. On Android 12+, falls back to inexact if `canScheduleExactAlarms()` is false. `BootReceiver` re-schedules active bills after device reboot. `BillReminderReceiver` handles the fired alarm intent and calls `NotificationHelper.showBillReminderNotification()` with the bill's id, title, amount, and days-left extras.

---

## Backup & Restore
`BackupManager` copies the raw SQLite file. It calls `AppDatabase.closeDatabase()` first to flush WAL and then reopens on next access.

---

## DB Migrations
`AppDatabase` version = **6**. Registered migrations:
- `MIGRATION_1_2` — adds `toCardId` column to `transactions`.
- `MIGRATION_2_3` — adds Supabase sync fields: `supabaseId` + `inviteCode` to `families`; `userId` to `members`; `familyId` to `categories` and `tags`.
- `MIGRATION_3_4` — offline-first support: adds `supabaseId` + `pendingSync` to `transactions`, `bank_cards`, `categories`, `tags`; creates the `pending_deletes` tombstone table. Existing rows get `supabaseId = id` so they are treated as already synced.
- `MIGRATION_4_5` — converts `supabaseId` from INTEGER to TEXT in `transactions` and `pending_deletes` (recreates both tables).
- `MIGRATION_5_6` — adds `initialBalance` column to `bank_cards` (seeded from existing `balance`); deduplicates `members` by `userId` and adds a unique index on `members.userId`.

Always add a named migration object and register it in `Room.databaseBuilder(...).addMigrations(...)`.

---

## Build & Test

```bash
./gradlew assembleDebug          # build debug APK
./gradlew installDebug           # build + install on connected device/emulator
./gradlew test                   # JVM unit tests
./gradlew connectedAndroidTest   # instrumented tests (requires device/emulator)
```

Dependencies are version-catalogued in `gradle/libs.versions.toml`. Add new dependencies there, then reference via `libs.<alias>` in `app/build.gradle.kts`.

---

## Key Files for Reference
- `data/database/AppDatabase.java` — entity list, version, migrations, executor
- `data/repository/TransactionRepository.java` — canonical example of balance-aware write
- `data/remote/RemoteDataSource.java` — canonical example of PostgREST calls and Callback pattern
- `utils/SupabaseClient.java` — Retrofit singleton, interceptor, token refresh logic
- `utils/SessionManager.java` — JWT + user/family metadata persistence
- `utils/SyncManager.java` — full sync orchestration (chain of per-table pulls)
- `utils/Constants.java` — all app-wide constants (colors, banks, prefs keys, Supabase URL/key)
- `utils/PersianDateUtils.java` — complete Jalali ↔ Gregorian conversion logic
- `utils/CurrencyUtils.java` — Rial/Toman formatting
- `res/navigation/nav_graph.xml` — all destinations and actions

