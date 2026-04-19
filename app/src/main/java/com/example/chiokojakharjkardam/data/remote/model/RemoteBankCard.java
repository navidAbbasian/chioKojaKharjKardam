package com.example.chiokojakharjkardam.data.remote.model;

import com.example.chiokojakharjkardam.data.database.entity.BankCard;
import com.google.gson.annotations.SerializedName;

public class RemoteBankCard {
    @SerializedName("id")
    public Long id;

    @SerializedName("family_id")
    public String familyId;

    @SerializedName("member_id")
    public String memberId;   // UUID of owning auth user

    @SerializedName("bank_name")
    public String bankName;

    @SerializedName("card_number")
    public String cardNumber;

    @SerializedName("card_holder_name")
    public String cardHolderName;

    @SerializedName("balance")
    public long balance;

    @SerializedName("initial_balance")
    public Long initialBalance;

    @SerializedName("color")
    public String color;

    public RemoteBankCard() {}

    public RemoteBankCard(BankCard card, String familyId, String memberUserId) {
        long sid = card.getSupabaseId();
        this.id = sid > 0 ? sid : null;
        this.familyId = familyId;
        this.memberId = memberUserId;
        this.bankName = card.getBankName();
        this.cardNumber = card.getCardNumber();
        this.cardHolderName = card.getCardHolderName();
        this.balance = card.getBalance();
        this.initialBalance = card.getInitialBalance();
        this.color = card.getColor();
    }

    /** localMemberId = Room Member.id of the owner */
    public BankCard toEntity(long localMemberId) {
        BankCard card = new BankCard(localMemberId, bankName, cardNumber,
                cardHolderName, balance, color);
        card.setSupabaseId(id != null ? id : 0);
        card.setPendingSync(0);
        // If remote has initialBalance, use it; otherwise fall back to balance
        card.setInitialBalance(initialBalance != null ? initialBalance : balance);
        return card;
    }
}
