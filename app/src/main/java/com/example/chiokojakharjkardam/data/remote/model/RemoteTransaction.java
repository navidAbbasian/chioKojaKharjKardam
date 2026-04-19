package com.example.chiokojakharjkardam.data.remote.model;

import com.example.chiokojakharjkardam.data.database.entity.Transaction;
import com.google.gson.annotations.SerializedName;

public class RemoteTransaction {
    @SerializedName("id")          public String id;
    @SerializedName("family_id")   public String familyId;
    @SerializedName("card_id")     public long cardId;
    @SerializedName("category_id") public Long categoryId;   // nullable
    @SerializedName("amount")      public long amount;
    @SerializedName("type")        public int type;
    @SerializedName("to_card_id")  public Long toCardId;     // nullable
    @SerializedName("description") public String description;
    @SerializedName("date")        public long date;
    @SerializedName("created_at")  public long createdAt;
    @SerializedName("created_by")  public String createdBy;

    public RemoteTransaction() {}

    public RemoteTransaction(Transaction tx, String familyId, String userId) {
        String sid = tx.getSupabaseId();
        this.id = sid != null ? sid : null; // null → omitted from JSON → Supabase auto-assigns UUID
        this.familyId = familyId;
        this.cardId = tx.getCardId();
        this.categoryId = tx.getCategoryId();
        this.amount = tx.getAmount();
        this.type = tx.getType();
        this.toCardId = tx.getToCardId();
        this.description = tx.getDescription();
        this.date = tx.getDate();
        this.createdAt = tx.getCreatedAt();
        this.createdBy = userId;
    }

    public Transaction toEntity() {
        Transaction tx = new Transaction(cardId, categoryId, amount, type, description, date);
        tx.setSupabaseId(id);
        tx.setToCardId(toCardId);
        tx.setCreatedAt(createdAt);
        tx.setPendingSync(Transaction.SYNC_DONE);
        return tx;
    }
}
