package com.example.chiokojakharjkardam.data.database.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;

@Entity(tableName = "transaction_tags",
        primaryKeys = {"transactionId", "tagId"},
        foreignKeys = {
                @ForeignKey(
                        entity = Transaction.class,
                        parentColumns = "id",
                        childColumns = "transactionId",
                        onDelete = ForeignKey.CASCADE
                ),
                @ForeignKey(
                        entity = Tag.class,
                        parentColumns = "id",
                        childColumns = "tagId",
                        onDelete = ForeignKey.CASCADE
                )
        },
        indices = {@Index("transactionId"), @Index("tagId")})
public class TransactionTag {

    private long transactionId;
    private long tagId;

    public TransactionTag(long transactionId, long tagId) {
        this.transactionId = transactionId;
        this.tagId = tagId;
    }

    // Getters and Setters
    public long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(long transactionId) {
        this.transactionId = transactionId;
    }

    public long getTagId() {
        return tagId;
    }

    public void setTagId(long tagId) {
        this.tagId = tagId;
    }
}

