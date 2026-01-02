package com.example.chiokojakharjkardam.data.database.entity;

import androidx.room.Ignore;

/**
 * مدل گزارش بر اساس دسته‌بندی
 */
public class CategoryReport {

    private long categoryId;
    private String categoryName;
    private String categoryColor;
    private String categoryIcon;
    private long totalAmount;
    private int transactionCount;

    public CategoryReport() {
    }

    @Ignore
    public CategoryReport(long categoryId, String categoryName, String categoryColor,
                          String categoryIcon, long totalAmount, int transactionCount) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.categoryColor = categoryColor;
        this.categoryIcon = categoryIcon;
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

