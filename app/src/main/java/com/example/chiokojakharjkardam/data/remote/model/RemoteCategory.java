package com.example.chiokojakharjkardam.data.remote.model;

import com.example.chiokojakharjkardam.data.database.entity.Category;
import com.google.gson.annotations.SerializedName;

public class RemoteCategory {
    @SerializedName("id")       public Long id;
    @SerializedName("family_id") public String familyId;
    @SerializedName("name")     public String name;
    @SerializedName("icon")     public String icon;
    @SerializedName("color")    public String color;
    @SerializedName("type")     public int type;

    public RemoteCategory() {}

    public RemoteCategory(Category c, String familyId) {
        long sid = c.getSupabaseId();
        this.id = sid > 0 ? sid : null; // null → Supabase auto-assigns; omitted from JSON by Gson
        this.familyId = familyId;
        this.name = c.getName();
        this.icon = c.getIcon();
        this.color = c.getColor();
        this.type = c.getType();
    }

    public Category toEntity(long localFamilyId) {
        Category c = new Category(name, icon, color, type);
        c.setSupabaseId(id != null ? id : 0);   // store Supabase id; Room generates local id
        c.setFamilyId(localFamilyId);
        c.setPendingSync(0);
        return c;
    }
}
