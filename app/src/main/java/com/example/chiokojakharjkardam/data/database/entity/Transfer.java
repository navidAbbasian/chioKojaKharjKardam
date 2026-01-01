package com.example.chiokojakharjkardam.data.database.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "transfers",
        foreignKeys = {
                @ForeignKey(
                        entity = BankCard.class,
                        parentColumns = "id",
                        childColumns = "fromCardId",
                        onDelete = ForeignKey.CASCADE
                ),
                @ForeignKey(
                        entity = BankCard.class,
                        parentColumns = "id",
                        childColumns = "toCardId",
                        onDelete = ForeignKey.CASCADE
                )
        },
        indices = {@Index("fromCardId"), @Index("toCardId")})
public class Transfer {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private long fromCardId;
    private long toCardId;
    private long amount;
    private String description;
    private long date;
    private long createdAt;

    public Transfer(long fromCardId, long toCardId, long amount,
                    String description, long date) {
        this.fromCardId = fromCardId;
        this.toCardId = toCardId;
        this.amount = amount;
        this.description = description;
        this.date = date;
        this.createdAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getFromCardId() {
        return fromCardId;
    }

    public void setFromCardId(long fromCardId) {
        this.fromCardId = fromCardId;
    }

    public long getToCardId() {
        return toCardId;
    }

    public void setToCardId(long toCardId) {
        this.toCardId = toCardId;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}

