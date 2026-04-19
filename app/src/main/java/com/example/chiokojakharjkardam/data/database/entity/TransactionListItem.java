package com.example.chiokojakharjkardam.data.database.entity;

/**
 * DTO enriched transaction for list display.
 * Contains the Transaction plus related card/member names.
 */
public class TransactionListItem {
    private final Transaction transaction;
    private final String cardName;        // e.g. "ملی - 1234"
    private final String memberName;      // نام عضو سازنده

    public TransactionListItem(Transaction transaction, String cardName, String memberName) {
        this.transaction = transaction;
        this.cardName = cardName;
        this.memberName = memberName;
    }

    public Transaction getTransaction() { return transaction; }
    public String getCardName() { return cardName != null ? cardName : ""; }
    public String getMemberName() { return memberName != null ? memberName : ""; }

    /**
     * Builds a subtitle like "ملی - 1234 • علی"
     */
    public String getSubtitle() {
        StringBuilder sb = new StringBuilder();
        if (cardName != null && !cardName.isEmpty()) sb.append(cardName);
        if (memberName != null && !memberName.isEmpty()) {
            if (sb.length() > 0) sb.append(" • ");
            sb.append(memberName);
        }
        return sb.toString();
    }
}

