package com.example.chiokojakharjkardam.ui.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chiokojakharjkardam.R;
import com.example.chiokojakharjkardam.data.database.entity.BankCard;
import com.example.chiokojakharjkardam.utils.CurrencyUtils;
import com.google.android.material.card.MaterialCardView;

public class CardAdapter extends ListAdapter<BankCard, CardAdapter.CardViewHolder> {

    private final OnCardClickListener listener;

    public interface OnCardClickListener {
        void onCardClick(BankCard card);
    }

    public CardAdapter(OnCardClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<BankCard> DIFF_CALLBACK = new DiffUtil.ItemCallback<BankCard>() {
        @Override
        public boolean areItemsTheSame(@NonNull BankCard oldItem, @NonNull BankCard newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull BankCard oldItem, @NonNull BankCard newItem) {
            return oldItem.getBalance() == newItem.getBalance()
                    && oldItem.getBankName().equals(newItem.getBankName())
                    && oldItem.getCardNumber().equals(newItem.getCardNumber());
        }
    };

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_card, parent, false);
        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class CardViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardView;
        private final TextView tvBankName;
        private final TextView tvCardNumber;
        private final TextView tvBalance;
        private final TextView tvCardHolder;

        CardViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.card_bank);
            tvBankName = itemView.findViewById(R.id.tv_bank_name);
            tvCardNumber = itemView.findViewById(R.id.tv_card_number);
            tvBalance = itemView.findViewById(R.id.tv_balance);
            tvCardHolder = itemView.findViewById(R.id.tv_card_holder);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onCardClick(getItem(position));
                }
            });
        }

        void bind(BankCard card) {
            tvBankName.setText(card.getBankName());
            tvCardNumber.setText("**** " + card.getCardNumber());
            tvBalance.setText(CurrencyUtils.formatToman(card.getBalance()));
            tvCardHolder.setText(card.getCardHolderName());

            try {
                cardView.setCardBackgroundColor(Color.parseColor(card.getColor()));
            } catch (Exception e) {
                cardView.setCardBackgroundColor(Color.parseColor("#2196F3"));
            }
        }
    }
}

