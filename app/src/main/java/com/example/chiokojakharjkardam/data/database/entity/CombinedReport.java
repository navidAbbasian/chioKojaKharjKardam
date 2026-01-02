package com.example.chiokojakharjkardam.data.database.entity;

import androidx.room.Ignore;

/**
 * مدل گزارش ترکیبی بر اساس دسته‌بندی و تگ
 */
public class CombinedReport {

    private long categoryId;
    private String categoryName;
    private String categoryColor;
    private String categoryIcon;
    private long tagId;
    private String tagName;
    private String tagColor;
    private long totalAmount;
    private int transactionCount;

    public CombinedReport() {
    }

    @Ignore
    public CombinedReport(long categoryId, String categoryName, String categoryColor,
                          String categoryIcon, long tagId, String tagName, String tagColor,
                          long totalAmount, int transactionCount) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.categoryColor = categoryColor;
        this.categoryIcon = categoryIcon;
        this.tagId = tagId;
        this.tagName = tagName;
        this.tagColor = tagColor;
        this.totalAmount = totalAmount;
        this.transactionCount = transactionCount;
    }

    public long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getCategoryColor() {
        return categoryColor;
    }

    public void setCategoryColor(String categoryColor) {
        this.categoryColor = categoryColor;
    }

    public String getCategoryIcon() {
        return categoryIcon;
    }

    public void setCategoryIcon(String categoryIcon) {
        this.categoryIcon = categoryIcon;
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

    /**
     * دریافت متن نمایشی گزارش
     */
    public String getDisplayText() {
        StringBuilder sb = new StringBuilder();
        if (categoryName != null && !categoryName.isEmpty()) {
            sb.append(categoryName);
        }
        if (tagName != null && !tagName.isEmpty()) {
            if (sb.length() > 0) {
                sb.append(" + ");
            }
            sb.append(tagName);
        }
        return sb.toString();
    }
}

