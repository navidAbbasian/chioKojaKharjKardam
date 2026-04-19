package com.example.chiokojakharjkardam.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Persists the Supabase session (tokens + user/family metadata) in SharedPreferences.
 * Must be initialised once via SessionManager.init(context) in App.onCreate().
 */
public class SessionManager {

    private static final String PREF_NAME     = "supabase_session";
    private static final String KEY_TOKEN     = "access_token";
    private static final String KEY_REFRESH   = "refresh_token";
    private static final String KEY_EXPIRES_AT = "expires_at";
    private static final String KEY_USER_ID   = "user_id";
    private static final String KEY_EMAIL     = "email";
    private static final String KEY_FULL_NAME = "full_name";
    private static final String KEY_FAMILY_ID = "family_id";
    private static final String KEY_IS_OWNER  = "is_owner";
    private static final String KEY_FAMILY_NAME = "family_name";
    private static final String KEY_INVITE_CODE = "invite_code";
    private static final String KEY_LOCAL_MEMBER_ID = "local_member_id";
    /** Millis — user stays logged in locally for this long after last app open. */
    private static final String KEY_LOCAL_SESSION_EXPIRY = "local_session_expiry";
    private static final long LOCAL_SESSION_DURATION_MS = 90L * 24 * 60 * 60 * 1000; // 90 days

    /**
     * Soft flag: cloud token could not be refreshed (refresh token expired on Supabase).
     * The local session is kept alive; UI can show a non-blocking prompt to re-login
     * when the user is online and wants to sync.
     */
    private static final String KEY_NEEDS_REAUTH = "needs_cloud_reauth";

    private static volatile SessionManager instance;
    private final SharedPreferences prefs;

    private SessionManager(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static void init(Context context) {
        if (instance == null) {
            synchronized (SessionManager.class) {
                if (instance == null) instance = new SessionManager(context);
            }
        }
    }

    public static SessionManager getInstance() {
        if (instance == null) throw new IllegalStateException("SessionManager not initialised");
        return instance;
    }

    // ── Auth ──────────────────────────────────────────────────────────────────

    /**
     * Returns true when an access token is stored locally.
     * The token may be cloud-expired but the user is still considered
     * locally authenticated — the interceptor refreshes it silently
     * when online, and never clears the session on failure.
     * Explicit logout via clearSession() is the only way to return false.
     */
    public boolean isLoggedIn() {
        return getAccessToken() != null;
    }

    /**
     * Records the current time as the last-active timestamp (informational).
     * No longer used as a gating check in isLoggedIn().
     */
    public void refreshLocalSession() {
        prefs.edit()
                .putLong(KEY_LOCAL_SESSION_EXPIRY,
                        System.currentTimeMillis() + LOCAL_SESSION_DURATION_MS)
                .apply();
    }

    /**
     * Returns true if the stored token is within 5 minutes of expiry.
     * expiresAt is Unix seconds from Supabase.
     */
    public boolean isTokenExpired() {
        long expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0);
        if (expiresAt == 0) return false;
        long bufferMs = 5 * 60 * 1000L; // 5 minutes
        return expiresAt * 1000L < System.currentTimeMillis() + bufferMs;
    }

    /**
     * Saves auth tokens. Uses apply() — called from both background (token refresh)
     * and main-thread Retrofit callbacks. apply() is safe because subsequent
     * operations always involve at least one network round-trip, providing
     * ample time for the async write to complete before the token is next needed.
     */
    public void saveTokens(String accessToken, String refreshToken, long expiresAt) {
        prefs.edit()
                .putString(KEY_TOKEN, accessToken)
                .putString(KEY_REFRESH, refreshToken)
                .putLong(KEY_EXPIRES_AT, expiresAt)
                .apply();
    }

    /** Uses apply() for the same reason as saveTokens(). */
    public void saveUser(String userId, String email, String fullName) {
        prefs.edit()
                .putString(KEY_USER_ID, userId)
                .putString(KEY_EMAIL, email)
                .putString(KEY_FULL_NAME, fullName)
                .apply();
    }

    /** Uses apply() for the same reason as saveTokens(). */
    public void saveFamily(String familyId, String familyName, String inviteCode, boolean isOwner) {
        prefs.edit()
                .putString(KEY_FAMILY_ID, familyId)
                .putString(KEY_FAMILY_NAME, familyName)
                .putString(KEY_INVITE_CODE, inviteCode)
                .putBoolean(KEY_IS_OWNER, isOwner)
                .apply();
    }

    /** Stores the Room auto-increment id of the local Member row for the current user. */
    public void saveLocalMemberId(long localMemberId) {
        prefs.edit().putLong(KEY_LOCAL_MEMBER_ID, localMemberId).apply();
    }

    public void clearSession() {
        prefs.edit().clear().apply();
    }

    /**
     * True when the cloud refresh token has expired and the user needs to re-login
     * to restore Supabase sync. The local session (Room data) is still valid.
     */
    public boolean needsReauth() {
        return prefs.getBoolean(KEY_NEEDS_REAUTH, false);
    }

    public void setNeedsReauth(boolean value) {
        prefs.edit().putBoolean(KEY_NEEDS_REAUTH, value).apply();
    }

    public void clearNeedsReauth() {
        prefs.edit().remove(KEY_NEEDS_REAUTH).apply();
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public String getAccessToken()  { return prefs.getString(KEY_TOKEN, null); }
    public String getRefreshToken() { return prefs.getString(KEY_REFRESH, null); }
    public String getUserId()       { return prefs.getString(KEY_USER_ID, null); }
    public String getUserEmail()    { return prefs.getString(KEY_EMAIL, null); }
    public String getFullName()     { return prefs.getString(KEY_FULL_NAME, null); }
    public String getFamilyId()     { return prefs.getString(KEY_FAMILY_ID, null); }
    public String getFamilyName()   { return prefs.getString(KEY_FAMILY_NAME, null); }
    public String getInviteCode()   { return prefs.getString(KEY_INVITE_CODE, null); }
    public boolean isOwner()        { return prefs.getBoolean(KEY_IS_OWNER, false); }
    public long getLocalMemberId()  { return prefs.getLong(KEY_LOCAL_MEMBER_ID, -1); }

    /** True if user is logged in AND has joined/created a family. */
    public boolean hasFamilyId() {
        return isLoggedIn() && getFamilyId() != null;
    }

    // Filter helper for PostgREST: "eq.{value}"
    public String familyFilter() { return "eq." + getFamilyId(); }
    public String userFilter()   { return "eq." + getUserId(); }
}
