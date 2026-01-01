package com.example.chiokojakharjkardam.data.database.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "bills",
        foreignKeys = @ForeignKey(
                entity = BankCard.class,
                parentColumns = "id",
                childColumns = "cardId",
                onDelete = ForeignKey.SET_NULL
        ),
        indices = @Index("cardId"))
public class Bill {

    public static final int RECURRING_NONE = 0;    // تکراری نیست
    public static final int RECURRING_MONTHLY = 1;  // ماهانه
    public static final int RECURRING_YEARLY = 2;   // سالانه

    @PrimaryKey(autoGenerate = true)
    private long id;

    private String title;
    private long amount;
    private long dueDate;
    private boolean isRecurring;
    private int recurringType;
    private boolean isPaid;
    private Long cardId; // nullable - کارت پیش‌فرض
    private int notifyBefore; // چند روز قبل یادآوری
    private long createdAt;

    public Bill(String title, long amount, long dueDate, boolean isRecurring,
                int recurringType, Long cardId, int notifyBefore) {
        this.title = title;
        this.amount = amount;
        this.dueDate = dueDate;
        this.isRecurring = isRecurring;
        this.recurringType = recurringType;
        this.isPaid = false;
        this.cardId = cardId;
        this.notifyBefore = notifyBefore;
        this.createdAt = System.currentTimeMillis();
    }

    @Ignore
    public Bill(String title, long amount, long dueDate, boolean isRecurring,
                int recurringType, int notifyBefore) {
        this(title, amount, dueDate, isRecurring, recurringType, null, notifyBefore);
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public long getDueDate() {
        return dueDate;
    }

    public void setDueDate(long dueDate) {
        this.dueDate = dueDate;
    }

    public boolean isRecurring() {
        return isRecurring;
    }

    public void setRecurring(boolean recurring) {
        isRecurring = recurring;
    }

    public int getRecurringType() {
        return recurringType;
    }

    public void setRecurringType(int recurringType) {
        this.recurringType = recurringType;
    }

    public boolean isPaid() {
        return isPaid;
    }

    public void setPaid(boolean paid) {
        isPaid = paid;
    }

    public Long getCardId() {
        return cardId;
    }

    public void setCardId(Long cardId) {
        this.cardId = cardId;
    }

    public int getNotifyBefore() {
        return notifyBefore;
    }

    public void setNotifyBefore(int notifyBefore) {
        this.notifyBefore = notifyBefore;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}

