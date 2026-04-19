package com.example.chiokojakharjkardam.data.remote.model;

import com.example.chiokojakharjkardam.data.database.entity.Tag;
import com.google.gson.annotations.SerializedName;

public class RemoteTag {
    @SerializedName("id")       public Long id;
    @SerializedName("family_id") public String familyId;
    @SerializedName("name")     public String name;
    @SerializedName("color")    public String color;

    public RemoteTag() {}

    public RemoteTag(Tag t, String familyId) {
        long sid = t.getSupabaseId();
        this.id = sid > 0 ? sid : null; // null → Supabase auto-assigns
        this.familyId = familyId;
        this.name = t.getName();
        this.color = t.getColor();
    }

    public Tag toEntity(long localFamilyId) {
        Tag t = new Tag(name, color);
        t.setSupabaseId(id != null ? id : 0);
        t.setFamilyId(localFamilyId);
        t.setPendingSync(0);
        return t;
    }
}
