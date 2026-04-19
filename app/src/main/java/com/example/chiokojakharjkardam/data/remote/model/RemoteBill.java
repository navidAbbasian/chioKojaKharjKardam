package com.example.chiokojakharjkardam.data.remote.model;

import com.example.chiokojakharjkardam.data.database.entity.Bill;
import com.google.gson.annotations.SerializedName;

public class RemoteBill {
    @SerializedName("id")             public Long id;
    @SerializedName("family_id")      public String familyId;
    @SerializedName("title")          public String title;
    @SerializedName("amount")         public long amount;
    @SerializedName("due_date")       public long dueDate;
    @SerializedName("is_recurring")   public boolean isRecurring;
    @SerializedName("recurring_type") public int recurringType;
    @SerializedName("is_paid")        public boolean isPaid;
    @SerializedName("card_id")        public Long cardId;     // nullable
    @SerializedName("notify_before")  public int notifyBefore;
    @SerializedName("created_at")     public long createdAt;
    @SerializedName("created_by")     public String createdBy;

    public RemoteBill() {}

    public RemoteBill(Bill b, String familyId, String userId) {
        this.id = b.getId();
        this.familyId = familyId;
        this.title = b.getTitle();
        this.amount = b.getAmount();
        this.dueDate = b.getDueDate();
        this.isRecurring = b.isRecurring();
        this.recurringType = b.getRecurringType();
        this.isPaid = b.isPaid();
        this.cardId = b.getCardId();
        this.notifyBefore = b.getNotifyBefore();
        this.createdAt = b.getCreatedAt();
        this.createdBy = userId;
    }

    public Bill toEntity() {
        Bill b = new Bill(title, amount, dueDate, isRecurring, recurringType, cardId, notifyBefore);
        b.setId(id);
        b.setPaid(isPaid);
        b.setCreatedAt(createdAt);
        return b;
    }
}

