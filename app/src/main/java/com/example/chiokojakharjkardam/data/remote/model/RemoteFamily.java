package com.example.chiokojakharjkardam.data.remote.model;

import com.example.chiokojakharjkardam.data.database.entity.Family;
import com.google.gson.annotations.SerializedName;

public class RemoteFamily {
    @SerializedName("id")
    public String id;          // UUID

    @SerializedName("name")
    public String name;

    @SerializedName("invite_code")
    public String inviteCode;

    @SerializedName("created_by")
    public String createdBy;   // UUID

    public RemoteFamily() {}

    public RemoteFamily(String name, String inviteCode, String createdBy) {
        this.name = name;
        this.inviteCode = inviteCode;
        this.createdBy = createdBy;
    }

    public Family toEntity() {
        Family f = new Family(name);
        f.setSupabaseId(id);
        f.setInviteCode(inviteCode);
        return f;
    }
}

