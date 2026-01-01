package com.example.chiokojakharjkardam.data.database.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "bank_cards",
        foreignKeys = @ForeignKey(
                entity = Member.class,
                parentColumns = "id",
                childColumns = "memberId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = @Index("memberId"))
public class BankCard {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private long memberId;
    private String bankName;
    private String cardNumber; // آخرین ۴ رقم
    private String cardHolderName;
    private long balance; // موجودی به ریال
    private String color;
    private long createdAt;

    public BankCard(long memberId, String bankName, String cardNumber,
                    String cardHolderName, long balance, String color) {
        this.memberId = memberId;
        this.bankName = bankName;
        this.cardNumber = cardNumber;
        this.cardHolderName = cardHolderName;
        this.balance = balance;
        this.color = color;
        this.createdAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getMemberId() {
        return memberId;
    }

    public void setMemberId(long memberId) {
        this.memberId = memberId;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getCardHolderName() {
        return cardHolderName;
    }

    public void setCardHolderName(String cardHolderName) {
        this.cardHolderName = cardHolderName;
    }

    public long getBalance() {
        return balance;
    }

    public void setBalance(long balance) {
        this.balance = balance;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}

