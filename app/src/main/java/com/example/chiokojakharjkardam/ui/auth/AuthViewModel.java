package com.example.chiokojakharjkardam.ui.auth;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.chiokojakharjkardam.data.database.AppDatabase;
import com.example.chiokojakharjkardam.data.database.entity.Family;
import com.example.chiokojakharjkardam.data.database.entity.Member;
import com.example.chiokojakharjkardam.data.remote.RemoteDataSource;
import com.example.chiokojakharjkardam.data.remote.model.AuthRequest;
import com.example.chiokojakharjkardam.data.remote.model.AuthResponse;
import com.example.chiokojakharjkardam.data.remote.model.RemoteFamily;
import com.example.chiokojakharjkardam.data.remote.model.RemoteProfile;
import com.example.chiokojakharjkardam.utils.Constants;
import com.example.chiokojakharjkardam.utils.SessionManager;
import com.example.chiokojakharjkardam.utils.SupabaseClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthViewModel extends AndroidViewModel {

    private static final String TAG = "AuthViewModel";

    public enum NavTarget { FAMILY_SETUP, MAIN }

    private final MutableLiveData<String> errorMsg   = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<NavTarget> navTarget = new MutableLiveData<>();

    private final SessionManager session;
    private final Application app;

    public AuthViewModel(Application app) {
        super(app);
        this.app = app;
        session = SessionManager.getInstance();
    }

    public LiveData<String>    getError()      { return errorMsg; }
    public LiveData<Boolean>   getIsLoading()  { return isLoading; }
    public LiveData<NavTarget> getNavTarget()  { return navTarget; }

    /** Clears the error so the same message can trigger the observer again. */
    public void clearError() { errorMsg.setValue(null); }

    // ──────────────────────────────────────────────────────────────
    public void signIn(String email, String password) {
        if (email.isEmpty() || password.isEmpty()) {
            errorMsg.setValue("ایمیل و رمز عبور را وارد کنید");
            return;
        }
        isLoading.setValue(true);
        SupabaseClient.getInstance().auth()
                .signIn("password", AuthRequest.forLogin(email, password))
                .enqueue(new Callback<AuthResponse>() {
                    @Override public void onResponse(Call<AuthResponse> c, Response<AuthResponse> r) {
                        if (r.isSuccessful() && r.body() != null) {
                            handleAuthResponse(r.body(), null);
                        } else {
                            isLoading.postValue(false);
                            errorMsg.postValue("ایمیل یا رمز عبور اشتباه است");
                        }
                    }
                    @Override public void onFailure(Call<AuthResponse> c, Throwable t) {
                        isLoading.postValue(false);
                        errorMsg.postValue("خطای شبکه: " + t.getMessage());
                    }
                });
    }

    // ──────────────────────────────────────────────────────────────
    public void signUp(String fullName, String email, String password) {
        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            errorMsg.setValue("تمام فیلدها را پر کنید");
            return;
        }
        if (password.length() < 6) {
            errorMsg.setValue("رمز عبور باید حداقل ۶ کاراکتر باشد");
            return;
        }
        isLoading.setValue(true);
        SupabaseClient.getInstance().auth()
                .signUp(AuthRequest.forSignUp(email, password, fullName))
                .enqueue(new Callback<AuthResponse>() {
                    @Override public void onResponse(Call<AuthResponse> c, Response<AuthResponse> r) {
                        if (r.isSuccessful() && r.body() != null) {
                            // fullName را از فرم ثبت‌نام مستقیم پاس می‌دهیم
                            handleAuthResponse(r.body(), fullName);
                        } else {
                            isLoading.postValue(false);
                            errorMsg.postValue("خطا در ثبت‌نام. شاید این ایمیل قبلاً استفاده شده");
                        }
                    }
                    @Override public void onFailure(Call<AuthResponse> c, Throwable t) {
                        isLoading.postValue(false);
                        errorMsg.postValue("خطای شبکه: " + t.getMessage());
                    }
                });
    }

    // ──────────────────────────────────────────────────────────────
    /**
     * @param pendingFullName نام وارد‌شده در فرم — null برای login، غیر‌null برای signup
     */
    private void handleAuthResponse(AuthResponse auth, String pendingFullName) {
        if (auth.accessToken == null || auth.user == null) {
            isLoading.postValue(false);
            errorMsg.postValue("پاسخ نامعتبر از سرور");
            return;
        }
        session.saveTokens(auth.accessToken, auth.refreshToken, auth.expiresAt);
        String initialName = (pendingFullName != null && !pendingFullName.isEmpty())
                ? pendingFullName : "";
        session.saveUser(auth.user.id, auth.user.email, initialName);
        session.refreshLocalSession();
        session.clearNeedsReauth();

        final boolean isSignUp = (pendingFullName != null);

        RemoteDataSource.getInstance().getProfile(auth.user.id,
                new RemoteDataSource.Callback<RemoteProfile>() {
                    @Override public void onSuccess(RemoteProfile profile) {
                        String fullName;
                        if (profile != null && profile.fullName != null && !profile.fullName.isEmpty()) {
                            fullName = profile.fullName;
                        } else if (pendingFullName != null && !pendingFullName.isEmpty()) {
                            fullName = pendingFullName;
                        } else {
                            fullName = auth.user.email;
                        }
                        session.saveUser(auth.user.id, auth.user.email, fullName);

                        if (profile != null && profile.familyId != null) {
                            fetchFamilyAndNavigate(profile.familyId, profile.isOwner);
                        } else {
                            // پروفایل بدون family_id: کاربر جدید یا هنوز خانواده‌ای نساخته
                            isLoading.postValue(false);
                            navTarget.postValue(NavTarget.FAMILY_SETUP);
                        }
                    }
                    @Override public void onError(String msg) {
                        Log.w(TAG, "Profile fetch failed (isSignUp=" + isSignUp + "): " + msg);

                        if (isSignUp) {
                            // ثبت‌نام: پروفایل ممکن است هنوز از طریق trigger ساخته نشده باشد
                            // نام فرم را نگه می‌داریم و به setup می‌فرستیم
                            if (pendingFullName != null && !pendingFullName.isEmpty()) {
                                session.saveUser(auth.user.id, auth.user.email, pendingFullName);
                            }
                            isLoading.postValue(false);
                            navTarget.postValue(NavTarget.FAMILY_SETUP);
                        } else {
                            // ورود: پروفایل باید وجود داشته باشد — خطای شبکه/سرور
                            // یک‌بار مجدد تلاش می‌کنیم
                            retryGetProfile(auth.user.id, msg);
                        }
                    }
                });
    }

    /** یک‌بار retry برای getProfile در صورت خطا در login */
    private void retryGetProfile(String userId, String originalError) {
        RemoteDataSource.getInstance().getProfile(userId,
                new RemoteDataSource.Callback<RemoteProfile>() {
                    @Override public void onSuccess(RemoteProfile profile) {
                        String fullName = (profile != null && profile.fullName != null && !profile.fullName.isEmpty())
                                ? profile.fullName : session.getUserEmail();
                        session.saveUser(userId, session.getUserEmail(), fullName);

                        if (profile != null && profile.familyId != null) {
                            fetchFamilyAndNavigate(profile.familyId, profile.isOwner);
                        } else {
                            isLoading.postValue(false);
                            navTarget.postValue(NavTarget.FAMILY_SETUP);
                        }
                    }
                    @Override public void onError(String msg) {
                        Log.e(TAG, "Profile retry also failed: " + msg);
                        // بعد از دو بار شکست، خطا نشان بده
                        isLoading.postValue(false);
                        errorMsg.postValue("خطا در بارگذاری اطلاعات حساب. لطفاً دوباره تلاش کنید.");
                    }
                });
    }

    private void fetchFamilyAndNavigate(String familyId, boolean isOwner) {
        RemoteDataSource.getInstance().getFamilyById(familyId,
                new RemoteDataSource.Callback<RemoteFamily>() {
                    @Override public void onSuccess(RemoteFamily fam) {
                        if (fam != null) {
                            session.saveFamily(fam.id, fam.name, fam.inviteCode, isOwner);
                            // ذخیره Family و Member در Room و سپس navigate
                            persistFamilyAndMemberToRoom(fam.id, fam.name, fam.inviteCode, isOwner, () -> {
                                isLoading.postValue(false);
                                navTarget.postValue(NavTarget.MAIN);
                            });
                        } else {
                            session.saveFamily(familyId, "", "", isOwner);
                            persistFamilyAndMemberToRoom(familyId, "", "", isOwner, () -> {
                                isLoading.postValue(false);
                                navTarget.postValue(NavTarget.MAIN);
                            });
                        }
                    }
                    @Override public void onError(String msg) {
                        session.saveFamily(familyId, "", "", isOwner);
                        persistFamilyAndMemberToRoom(familyId, "", "", isOwner, () -> {
                            Log.w(TAG, "fetchFamilyAndNavigate error (saved fallback): " + msg);
                            isLoading.postValue(false);
                            navTarget.postValue(NavTarget.MAIN);
                        });
                    }
                });
    }

    /**
     * ذخیره Family و Member کاربر جاری در Room تا SyncManager بتواند
     * اطلاعات را از Supabase دانلود کند.
     */
    private void persistFamilyAndMemberToRoom(String supabaseFamilyId, String familyName, 
                                               String inviteCode, boolean isOwner, Runnable onDone) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getDatabase(app);
                
                // پاک کردن داده‌های قبلی (برای جلوگیری از تداخل)
                db.familyDao().deleteAll();
                db.memberDao().deleteAll();
                
                // ایجاد Family
                Family family = new Family(familyName != null && !familyName.isEmpty() 
                        ? familyName : "خانواده");
                family.setSupabaseId(supabaseFamilyId);
                family.setInviteCode(inviteCode);
                long localFamilyId = db.familyDao().insert(family);
                
                // ایجاد Member برای کاربر جاری
                String memberName = session.getFullName() != null && !session.getFullName().isEmpty()
                        ? session.getFullName() : session.getUserEmail();
                Member member = new Member(localFamilyId, memberName, isOwner, Constants.MEMBER_COLORS[0]);
                member.setUserId(session.getUserId());
                long memberId = db.memberDao().insert(member);
                
                // ذخیره localMemberId در session
                session.saveLocalMemberId(memberId);
                
                Log.d(TAG, "Family and Member saved to Room: familyId=" + localFamilyId + ", memberId=" + memberId);
            } catch (Exception e) {
                Log.e(TAG, "Error saving family/member to Room: " + e.getMessage());
            }
            
            // فراخوانی callback بعد از اتمام
            if (onDone != null) onDone.run();
        });
    }
}

