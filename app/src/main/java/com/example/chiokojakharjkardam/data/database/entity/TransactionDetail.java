package com.example.chiokojakharjkardam.data.database.entity;

/**
 * DTO برای نمایش اطلاعات کامل تراکنش در PDF (شامل نام دسته‌بندی و تگ‌ها)
 */
public class TransactionDetail {
    private final Transaction transaction;
    private final String categoryName;
    private final String tagNames; // تگ‌ها با کاما جدا شده

    public TransactionDetail(Transaction transaction, String categoryName, String tagNames) {
        this.transaction = transaction;
        this.categoryName = categoryName;
        this.tagNames = tagNames;
    }

    public Transaction getTransaction() { return transaction; }
    public String getCategoryName() { return categoryName != null ? categoryName : "-"; }
    public String getTagNames() { return (tagNames != null && !tagNames.isEmpty()) ? tagNames : "-"; }
}

