package com.example.chiokojakharjkardam.data.remote.model;

import com.google.gson.annotations.SerializedName;

public class AuthResponse {
    @SerializedName("access_token")
    public String accessToken;

    @SerializedName("refresh_token")
    public String refreshToken;

    @SerializedName("expires_in")
    public long expiresIn;

    @SerializedName("expires_at")
    public long expiresAt;

    @SerializedName("user")
    public AuthUser user;

    public static class AuthUser {
        @SerializedName("id")
        public String id;

        @SerializedName("email")
        public String email;
    }
}

