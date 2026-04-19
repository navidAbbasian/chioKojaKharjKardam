package com.example.chiokojakharjkardam.data.remote;

import com.example.chiokojakharjkardam.data.remote.model.AuthRequest;
import com.example.chiokojakharjkardam.data.remote.model.AuthResponse;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface SupabaseAuthService {

    /** Register new user */
    @POST("auth/v1/signup")
    Call<AuthResponse> signUp(@Body AuthRequest request);

    /** Login with email + password (grant_type=password) */
    @POST("auth/v1/token")
    Call<AuthResponse> signIn(
            @Query("grant_type") String grantType,
            @Body AuthRequest request);

    /** Refresh expired access token */
    @POST("auth/v1/token")
    Call<AuthResponse> refreshToken(
            @Query("grant_type") String grantType,
            @Body Map<String, String> body);
}

