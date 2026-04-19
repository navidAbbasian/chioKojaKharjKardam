package com.example.chiokojakharjkardam.ui.family;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.chiokojakharjkardam.data.database.AppDatabase;
import com.example.chiokojakharjkardam.data.database.entity.Family;
import com.example.chiokojakharjkardam.data.remote.RemoteDataSource;
import com.example.chiokojakharjkardam.data.remote.model.RemoteFamily;
import com.example.chiokojakharjkardam.data.remote.model.RemoteProfile;
import com.example.chiokojakharjkardam.utils.SessionManager;

import java.security.SecureRandom;

public class FamilyViewModel extends AndroidViewModel {

    private static final String TAG = "FamilyViewModel";
    private static final String INVITE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private final MutableLiveData<String> errorMsg    = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading  = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> success    = new MutableLiveData<>();

    /** True if local Room has data that should be migrated to Supabase. */
    private final MutableLiveData<Boolean> hasMigratableData = new MutableLiveData<>(false);

    private final SessionManager session;
    private final RemoteDataSource remote;
    private final AppDatabase db;

    public FamilyViewModel(Application app) {
        super(app);
        session = SessionManager.getInstance();
        remote  = RemoteDataSource.getInstance();
        db      = AppDatabase.getDatabase(app);
        checkLocalData();
    }

    public LiveData<String>  getError()            { return errorMsg; }
    public LiveData<Boolean> getIsLoading()         { return isLoading; }
    public LiveData<Boolean> getSuccess()           { return success; }
    public LiveData<Boolean> getHasMigratableData() { return hasMigratableData; }

    // ──────────────────────────────────────────────────────────────
    // Create family (owner flow)
    // ──────────────────────────────────────────────────────────────
    public void createFamily(String familyName) {
        if (familyName.trim().isEmpty()) {
            errorMsg.setValue("نام خانواده را وارد کنید");
            return;
        }
        isLoading.setValue(true);

        String inviteCode = generateInviteCode();
        String userId     = session.getUserId();
        // نام کاربر را از session بخوان؛ اگر خالی بود از ایمیل استفاده کن
        String ownerName  = (session.getFullName() != null && !session.getFullName().isEmpty())
                ? session.getFullName() : session.getUserEmail();

        RemoteFamily remoteFamily = new RemoteFamily(familyName.trim(), inviteCode, userId);

        remote.createFamily(remoteFamily, new RemoteDataSource.Callback<RemoteFamily>() {
            @Override public void onSuccess(RemoteFamily created) {
                if (created == null || created.id == null) {
                    isLoading.postValue(false);
                    errorMsg.postValue("خطا: اطلاعات خانواده دریافت نشد. دوباره تلاش کنید");
                    return;
                }
                // Upsert profile: set family_id + is_owner + full_name
                // از upsert استفاده می‌کنیم تا حتی اگر trigger هنوز ردیف نساخته باشد کار کند
                RemoteProfile profileUpdate = new RemoteProfile();
                profileUpdate.id          = userId;
                profileUpdate.fullName    = ownerName;
                profileUpdate.email       = session.getUserEmail() != null ? session.getUserEmail() : "";
                profileUpdate.familyId    = created.id;
                profileUpdate.isOwner     = true;
                profileUpdate.avatarColor = com.example.chiokojakharjkardam.utils.Constants.MEMBER_COLORS[0];

                remote.upsertProfile(profileUpdate, new RemoteDataSource.Callback<RemoteProfile>() {
                    @Override public void onSuccess(RemoteProfile updated) {
                        session.saveFamily(created.id, created.name, created.inviteCode, true);
                        session.saveUser(userId, session.getUserEmail(), ownerName);
                        persistFamilyAndSignalSuccess(created.id, created.name, created.inviteCode,
                                userId, ownerName, true);
                    }
                    @Override public void onError(String msg) {
                        // Profile link failed but family WAS created in Supabase.
                        // Save the family to session so the user is not stuck in a
                        // FamilySetupActivity loop on re-launch. The profile update
                        // will be retried silently on the next sync.
                        Log.w(TAG, "upsertProfile failed after createFamily — proceeding anyway: " + msg);
                        session.saveFamily(created.id, created.name, created.inviteCode, true);
                        session.saveUser(userId, session.getUserEmail(), ownerName);
                        // Retry upsert silently in background
                        remote.upsertProfile(profileUpdate, new RemoteDataSource.Callback<RemoteProfile>() {
                            @Override public void onSuccess(RemoteProfile r) { /* silent */ }
                            @Override public void onError(String m) {
                                Log.e(TAG, "upsertProfile retry also failed: " + m);
                            }
                        });
                        persistFamilyAndSignalSuccess(created.id, created.name, created.inviteCode,
                                userId, ownerName, true);
                    }
                });
            }
            @Override public void onError(String msg) {
                isLoading.postValue(false);
                errorMsg.postValue("خطا در ایجاد خانواده: " + msg);
            }
        });
    }

    // ──────────────────────────────────────────────────────────────
    // Join family (member flow)
    // ──────────────────────────────────────────────────────────────
    public void joinFamily(String inviteCode) {
        if (inviteCode.trim().length() != 6) {
            errorMsg.setValue("کد دعوت باید ۶ کاراکتر باشد");
            return;
        }
        isLoading.setValue(true);

        remote.getFamilyByInviteCode(inviteCode.trim().toUpperCase(),
                new RemoteDataSource.Callback<RemoteFamily>() {
                    @Override public void onSuccess(RemoteFamily family) {
                        if (family == null) {
                            isLoading.postValue(false);
                            errorMsg.postValue("خانواده‌ای با این کد یافت نشد");
                            return;
                        }

                        // Upsert profile: set family_id (member, not owner)
                        String memberName = session.getFullName() != null && !session.getFullName().isEmpty()
                                ? session.getFullName() : session.getUserEmail();
                        RemoteProfile memberProfile = new RemoteProfile();
                        memberProfile.id          = session.getUserId();
                        memberProfile.fullName    = memberName;
                        memberProfile.email       = session.getUserEmail() != null ? session.getUserEmail() : "";
                        memberProfile.familyId    = family.id;
                        memberProfile.isOwner     = false;
                        memberProfile.avatarColor = com.example.chiokojakharjkardam.utils.Constants.MEMBER_COLORS[1];

                        remote.upsertProfile(memberProfile, new RemoteDataSource.Callback<RemoteProfile>() {
                            @Override public void onSuccess(RemoteProfile updated) {
                                session.saveFamily(family.id, family.name, family.inviteCode, false);

                                AppDatabase.databaseWriteExecutor.execute(() -> {
                                    db.familyDao().deleteAll();
                                    db.memberDao().deleteAll();
                                    Family f = new Family(family.name);
                                    f.setSupabaseId(family.id);
                                    f.setInviteCode(family.inviteCode);
                                    long localFamilyId = db.familyDao().insert(f);

                                    // Insert current user as a local member so getLocalMemberId()
                                    // works immediately (before the first SyncManager pull).
                                    com.example.chiokojakharjkardam.data.database.entity.Member m =
                                            new com.example.chiokojakharjkardam.data.database.entity.Member(
                                                    localFamilyId, memberName, false,
                                                    com.example.chiokojakharjkardam.utils.Constants.MEMBER_COLORS[1]);
                                    m.setUserId(session.getUserId());
                                    long memberId = db.memberDao().insert(m);
                                    session.saveLocalMemberId(memberId);

                                    // Signal success only after Room writes complete
                                    isLoading.postValue(false);
                                    success.postValue(true);
                                });
                            }
                            @Override public void onError(String msg) {
                                isLoading.postValue(false);
                                errorMsg.postValue("خطا در عضویت: " + msg);
                            }
                        });
                    }
                    @Override public void onError(String msg) {
                        isLoading.postValue(false);
                        errorMsg.postValue("خطا در جستجوی خانواده: " + msg);
                    }
                });
    }

    // ──────────────────────────────────────────────────────────────

    private void checkLocalData() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            boolean hasData = db.bankCardDao().getCardCount() > 0
                    || db.transactionDao().getTransactionCount() > 0;
            hasMigratableData.postValue(hasData);
        });
    }

    /**
     * Writes Family + owner Member to Room, then signals success.
     * Called from both the upsertProfile onSuccess and onError paths of createFamily.
     */
    private void persistFamilyAndSignalSuccess(String supabaseId, String name, String inviteCode,
                                                String userId, String ownerName, boolean isOwner) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            db.familyDao().deleteAll();
            Family f = new Family(name);
            f.setSupabaseId(supabaseId);
            f.setInviteCode(inviteCode);
            long localId = db.familyDao().insert(f);

            com.example.chiokojakharjkardam.data.database.entity.Member m =
                    new com.example.chiokojakharjkardam.data.database.entity.Member(
                            localId, ownerName, isOwner,
                            com.example.chiokojakharjkardam.utils.Constants.MEMBER_COLORS[0]);
            m.setUserId(userId);
            long memberId = db.memberDao().insert(m);
            session.saveLocalMemberId(memberId);

            // Signal success only after Room writes complete
            isLoading.postValue(false);
            success.postValue(true);
        });
    }

    /** Clears the error so the same message can trigger the observer again. */
    public void clearError() { errorMsg.postValue(null); }

    private String generateInviteCode() {
        StringBuilder code = new StringBuilder();
        SecureRandom rng = new SecureRandom();
        for (int i = 0; i < 6; i++)
            code.append(INVITE_CHARS.charAt(rng.nextInt(INVITE_CHARS.length())));
        return code.toString();
    }
}

