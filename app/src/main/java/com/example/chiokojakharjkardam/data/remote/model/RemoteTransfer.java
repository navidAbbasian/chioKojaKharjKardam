package com.example.chiokojakharjkardam.data.remote.model;

import com.example.chiokojakharjkardam.data.database.entity.Transfer;
import com.google.gson.annotations.SerializedName;

public class RemoteTransfer {
    @SerializedName("id")           public Long id;
    @SerializedName("family_id")    public String familyId;
    @SerializedName("from_card_id") public long fromCardId;
    @SerializedName("to_card_id")   public long toCardId;
    @SerializedName("amount")       public long amount;
    @SerializedName("description")  public String description;
    @SerializedName("date")         public long date;
    @SerializedName("created_at")   public long createdAt;
    @SerializedName("created_by")   public String createdBy;

    public RemoteTransfer() {}

    public RemoteTransfer(Transfer t, String familyId, String userId) {
        this.id = t.getId();
        this.familyId = familyId;
        this.fromCardId = t.getFromCardId();
        this.toCardId = t.getToCardId();
        this.amount = t.getAmount();
        this.description = t.getDescription();
        this.date = t.getDate();
        this.createdAt = t.getCreatedAt();
        this.createdBy = userId;
    }

    public Transfer toEntity() {
        Transfer t = new Transfer(fromCardId, toCardId, amount, description, date);
        t.setId(id);
        t.setCreatedAt(createdAt);
        return t;
    }
}

