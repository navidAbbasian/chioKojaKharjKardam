package com.example.chiokojakharjkardam.data.database.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "transactions",
        foreignKeys = {
                @ForeignKey(
                        entity = BankCard.class,
                        parentColumns = "id",
                        childColumns = "cardId",
                        onDelete = ForeignKey.CASCADE
                ),
                @ForeignKey(
                        entity = Category.class,
                        parentColumns = "id",
                        childColumns = "categoryId",
                        onDelete = ForeignKey.SET_NULL
                )
        },
        indices = {@Index("cardId"), @Index("categoryId")})
public class Transaction {

    public static final int TYPE_EXPENSE = 0; // خرج
    public static final int TYPE_INCOME = 1;  // درآمد

    @PrimaryKey(autoGenerate = true)
    private long id;

    private long cardId;
    private Long categoryId; // nullable
    private long amount;
    private int type; // 0: خرج, 1: درآمد
    private String description;
    private long date; // تاریخ تراکنش
    private long createdAt;

    public Transaction(long cardId, Long categoryId, long amount,
                       int type, String description, long date) {
        this.cardId = cardId;
        this.categoryId = categoryId;
        this.amount = amount;
        this.type = type;
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

    public long getCardId() {
        return cardId;
    }

    public void setCardId(long cardId) {
        this.cardId = cardId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
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

