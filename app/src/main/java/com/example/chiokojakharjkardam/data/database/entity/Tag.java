package com.example.chiokojakharjkardam.data.database.entity;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "tags")
public class Tag {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private long familyId;   // Supabase-synced family scope
    private String name;
    private String color;

    /** Supabase cloud ID; 0 = not yet uploaded */
    private long supabaseId = 0;

    /** 0=synced, 1=new, 2=update */
    private int pendingSync = 0;

    public Tag(String name, String color) {
        this.name = name;
        this.color = color;
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

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public long getSupabaseId()         { return supabaseId; }
    public void setSupabaseId(long v)   { this.supabaseId = v; }

    public int getPendingSync()         { return pendingSync; }
    public void setPendingSync(int v)   { this.pendingSync = v; }
}
