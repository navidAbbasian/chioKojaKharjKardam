package com.example.chiokojakharjkardam.data.database.entity;

/**
 * DTO برای نمایش اطلاعات کامل تراکنش در PDF/Excel
 */
public class TransactionDetail {
    private final Transaction transaction;
    private final String categoryName;
    private final String tagNames;
    private final String cardName;
    private final String memberName;

    public TransactionDetail(Transaction transaction, String categoryName, String tagNames) {
        this(transaction, categoryName, tagNames, "-", "-");
    }

    public TransactionDetail(Transaction transaction, String categoryName, String tagNames,
                             String cardName, String memberName) {
        this.transaction = transaction;
        this.categoryName = categoryName;
        this.tagNames = tagNames;
        this.cardName = cardName != null ? cardName : "-";
        this.memberName = memberName != null ? memberName : "-";
    }

    public Transaction getTransaction() { return transaction; }
    public String getCategoryName() { return categoryName != null ? categoryName : "-"; }
    public String getTagNames() { return (tagNames != null && !tagNames.isEmpty()) ? tagNames : "-"; }
    public String getCardName() { return cardName; }
    public String getMemberName() { return memberName; }
}
