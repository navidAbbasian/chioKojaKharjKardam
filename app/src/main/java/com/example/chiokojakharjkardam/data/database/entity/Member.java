package com.example.chiokojakharjkardam.data.database.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "members",
        foreignKeys = @ForeignKey(
                entity = Family.class,
                parentColumns = "id",
                childColumns = "familyId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = @Index("familyId"))
public class Member {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private long familyId;
    private String name;
    private boolean isOwner;
    private String avatarColor;
    private long createdAt;

    public Member(long familyId, String name, boolean isOwner, String avatarColor) {
        this.familyId = familyId;
        this.name = name;
        this.isOwner = isOwner;
        this.avatarColor = avatarColor;
        this.createdAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getFamilyId() {
        return familyId;
    }

    public void setFamilyId(long familyId) {
        this.familyId = familyId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isOwner() {
        return isOwner;
    }

    public void setOwner(boolean owner) {
        isOwner = owner;
    }

    public String getAvatarColor() {
        return avatarColor;
    }

    public void setAvatarColor(String avatarColor) {
        this.avatarColor = avatarColor;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}

