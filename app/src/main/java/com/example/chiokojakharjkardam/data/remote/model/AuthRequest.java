package com.example.chiokojakharjkardam.data.remote.model;

import com.google.gson.annotations.SerializedName;
import java.util.Map;

public class AuthRequest {
    @SerializedName("email")
    public String email;

    @SerializedName("password")
    public String password;

    /** Used for signup — sets raw_user_meta_data */
    @SerializedName("data")
    public Map<String, String> data;

    public static AuthRequest forLogin(String email, String password) {
        AuthRequest r = new AuthRequest();
        r.email = email;
        r.password = password;
        return r;
    }

    public static AuthRequest forSignUp(String email, String password, String fullName) {
        AuthRequest r = new AuthRequest();
        r.email = email;
        r.password = password;
        r.data = new java.util.HashMap<>();
        r.data.put("full_name", fullName);
        return r;
    }
}

