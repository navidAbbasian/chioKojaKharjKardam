package com.example.chiokojakharjkardam.data.remote.model;

import com.example.chiokojakharjkardam.data.database.entity.TransactionTag;
import com.google.gson.annotations.SerializedName;

public class RemoteTransactionTag {
    @SerializedName("transaction_id") public String transactionId;
    @SerializedName("tag_id")         public long tagId;

    public RemoteTransactionTag() {}

    public RemoteTransactionTag(String transactionId, long tagId) {
        this.transactionId = transactionId;
        this.tagId = tagId;
    }

    /** Converts to local Room entity — caller supplies the local Room IDs. */
    public TransactionTag toEntity(long localTransactionId, long localTagId) {
        return new TransactionTag(localTransactionId, localTagId);
    }
}

