package com.example.chiokojakharjkardam.data.remote.model;

import com.example.chiokojakharjkardam.data.database.entity.Member;
import com.google.gson.annotations.SerializedName;

public class RemoteProfile {
    @SerializedName("id")
    public String id;          // UUID (= auth.users.id)

    @SerializedName("full_name")
    public String fullName;

    @SerializedName("email")
    public String email;

    @SerializedName("family_id")
    public String familyId;   // UUID

    @SerializedName("is_owner")
    public boolean isOwner;

    @SerializedName("avatar_color")
    public String avatarColor;

    public RemoteProfile() {}

    /** Convert to Room Member entity. localFamilyId is the Room auto-increment PK of Family. */
    public Member toMember(long localFamilyId) {
        Member m = new Member(localFamilyId, fullName, isOwner,
                avatarColor != null ? avatarColor : "#3D7A5F");
        m.setUserId(id);
        return m;
    }
}

