package com.example.chiokojakharjkardam.data.database.entity;

import androidx.room.Ignore;

/**
 * مدل گزارش بر اساس تگ
 */
public class TagReport {

    private long tagId;
    private String tagName;
    private String tagColor;
    private long totalAmount;
    private int transactionCount;

    public TagReport() {
    }

    @Ignore
    public TagReport(long tagId, String tagName, String tagColor,
                     long totalAmount, int transactionCount) {
        this.tagId = tagId;
        this.tagName = tagName;
        this.tagColor = tagColor;
        this.totalAmount = totalAmount;
        this.transactionCount = transactionCount;
    }

    public long getTagId() {
        return tagId;
    }

    public void setTagId(long tagId) {
        this.tagId = tagId;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public String getTagColor() {
        return tagColor;
    }

    public void setTagColor(String tagColor) {
        this.tagColor = tagColor;
    }

    public long getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(long totalAmount) {
        this.totalAmount = totalAmount;
    }

    public int getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(int transactionCount) {
        this.transactionCount = transactionCount;
    }
}

