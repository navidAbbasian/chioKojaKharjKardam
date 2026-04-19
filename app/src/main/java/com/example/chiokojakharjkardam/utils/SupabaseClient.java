package com.example.chiokojakharjkardam.utils;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.chiokojakharjkardam.data.remote.SupabaseAuthService;
import com.example.chiokojakharjkardam.data.remote.SupabaseRestService;
import com.example.chiokojakharjkardam.data.remote.model.AuthResponse;
import com.google.gson.Gson;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Route;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Singleton that owns the OkHttpClient + Retrofit instances for Supabase.
 *
 * Header injection (apikey + Authorization) is done by SupabaseInterceptor.
 * Expired JWT is silently refreshed inside the interceptor before every call.
 */
public class SupabaseClient {

    private static final String TAG = "SupabaseClient";
    private static volatile SupabaseClient instance;

    private final SupabaseAuthService authService;
    private final SupabaseRestService restService;

    private SupabaseClient() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new SupabaseInterceptor())
                .authenticator(new SupabaseAuthenticator())
                .addInterceptor(logging)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Constants.SUPABASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        authService = retrofit.create(SupabaseAuthService.class);
        restService  = retrofit.create(SupabaseRestService.class);
    }

    public static SupabaseClient getInstance() {
        if (instance == null) {
            synchronized (SupabaseClient.class) {
                if (instance == null) instance = new SupabaseClient();
            }
        }
        return instance;
    }

    public SupabaseAuthService auth() { return authService; }
    public SupabaseRestService rest() { return restService; }

    // ──────────────────────────────────────────────────────────────────────────
    // Interceptor: adds apikey + Bearer token; auto-refreshes expired tokens
    // ──────────────────────────────────────────────────────────────────────────
    private static class SupabaseInterceptor implements Interceptor {
        @NonNull
        @Override
        public Response intercept(@NonNull Chain chain) throws IOException {
            SessionManager session = SessionManager.getInstance();

            // Only attempt proactive refresh when the device is online and token is near expiry
            if (session.isLoggedIn() && session.isTokenExpired()) {
                try {
                    if (NetworkMonitor.getInstance().isOnline()) {
                        refreshTokenSync(session);
                    }
                } catch (Exception ignored) {
                    // NetworkMonitor not yet initialised on very first call — skip silently
                }
            }

            Request.Builder builder = chain.request().newBuilder()
                    .header("apikey", Constants.SUPABASE_ANON_KEY)
                    .header("Content-Type", "application/json");

            String token = session.getAccessToken();
            if (token != null) {
                builder.header("Authorization", "Bearer " + token);
            }

            return chain.proceed(builder.build());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Authenticator: reacts to 401 by refreshing the token and retrying once
    // ──────────────────────────────────────────────────────────────────────────
    private static class SupabaseAuthenticator implements okhttp3.Authenticator {
        @Nullable
        @Override
        public Request authenticate(@Nullable Route route, @NonNull Response response) throws IOException {
            // Prevent infinite retry loop — if already retried once, give up
            if (responseCount(response) >= 2) {
                Log.w(TAG, "401 persists after token refresh — giving up");
                return null;
            }

            SessionManager session = SessionManager.getInstance();
            if (!session.isLoggedIn()) return null;

            Log.d(TAG, "Got 401 — attempting token refresh before retry");
            String tokenBefore = session.getAccessToken();
            refreshTokenSync(session);
            String tokenAfter = session.getAccessToken();

            // If token didn't change, refresh failed — retrying with same invalid token is pointless
            if (tokenAfter == null || tokenAfter.equals(tokenBefore)) {
                Log.w(TAG, "Token refresh failed or produced same token — cannot recover 401");
                session.setNeedsReauth(true);
                return null;
            }

            // Retry the original request with the fresh token
            return response.request().newBuilder()
                    .header("apikey", Constants.SUPABASE_ANON_KEY)
                    .header("Authorization", "Bearer " + tokenAfter)
                    .build();
        }

        private int responseCount(Response response) {
            int count = 1;
            while ((response = response.priorResponse()) != null) count++;
            return count;
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Synchronous token refresh
    // Rules:
    //  • Offline          → skip entirely, keep local session alive
    //  • Online + success → update tokens, renew 90-day local session
    //  • Online + failure → log warning, set needsReauth flag, but NEVER clear
    //                       the local session — user keeps working offline
    // ──────────────────────────────────────────────────────────────────────────
    private static void refreshTokenSync(SessionManager session) {
        try {
            String refreshToken = session.getRefreshToken();
            if (refreshToken == null) return;

            String url = Constants.SUPABASE_URL + "auth/v1/token?grant_type=refresh_token";
            String bodyJson = "{\"refresh_token\":\"" + refreshToken + "\"}";
            RequestBody body = RequestBody.create(bodyJson, MediaType.get("application/json"));

            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .addHeader("apikey", Constants.SUPABASE_ANON_KEY)
                    .build();

            OkHttpClient basicClient = new OkHttpClient.Builder()
                    .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                    .build();

            try (Response response = basicClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String json = response.body().string();
                    AuthResponse auth = new Gson().fromJson(json, AuthResponse.class);
                    if (auth != null && auth.accessToken != null) {
                        session.saveTokens(auth.accessToken, auth.refreshToken, auth.expiresAt);
                        session.refreshLocalSession();
                        session.clearNeedsReauth();
                        Log.d(TAG, "Token refreshed successfully");
                    }
                } else {
                    // Cloud auth failed (e.g. refresh token revoked or expired on Supabase side).
                    // We do NOT clear the local session — the user must keep using the app.
                    // Set a soft flag so the UI can show a non-blocking re-login prompt.
                    session.setNeedsReauth(true);
                    Log.w(TAG, "Token refresh failed HTTP " + response.code()
                            + " — local session preserved, cloud re-auth flagged");
                }
            }
        } catch (Exception e) {
            // Any I/O error is treated as transient — keep session alive
            Log.e(TAG, "Token refresh exception (keeping session): " + e.getMessage());
        }
    }
}
