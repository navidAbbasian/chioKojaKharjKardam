package com.example.chiokojakharjkardam.data.database.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "categories")
public class Category {

    public static final int TYPE_EXPENSE = 0; // خرج
    public static final int TYPE_INCOME = 1;  // درآمد
    public static final int TYPE_BOTH = 2;    // هردو

    @PrimaryKey(autoGenerate = true)
    private long id;

    private long familyId;   // Supabase-synced family scope
    private String name;
    private String icon;
    private String color;
    private int type; // 0: خرج, 1: درآمد, 2: هردو
    private boolean isDefault;

    /** Supabase cloud ID; 0 = not yet uploaded */
    private long supabaseId = 0;

    /** 0=synced, 1=new, 2=update */
    private int pendingSync = 0;

    public Category(String name, String icon, String color, int type, boolean isDefault) {
        this.name = name;
        this.icon = icon;
        this.color = color;
        this.type = type;
        this.isDefault = isDefault;
    }

    // Constructor for user-created categories (not default)
    @Ignore
    public Category(String name, String icon, String color, int type) {
        this(name, icon, color, type, false);
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getFamilyId() { return familyId; }
    public void setFamilyId(long familyId) { this.familyId = familyId; }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }

    public long getSupabaseId()         { return supabaseId; }
    public void setSupabaseId(long v)   { this.supabaseId = v; }

    public int getPendingSync()         { return pendingSync; }
    public void setPendingSync(int v)   { this.pendingSync = v; }
}
