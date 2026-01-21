package com.example.chiokojakharjkardam.ui.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chiokojakharjkardam.R;
import com.example.chiokojakharjkardam.data.database.entity.Transaction;
import com.example.chiokojakharjkardam.utils.CurrencyUtils;
import com.example.chiokojakharjkardam.utils.PersianDateUtils;
import com.google.android.material.card.MaterialCardView;

public class TransactionAdapter extends ListAdapter<Transaction, TransactionAdapter.TransactionViewHolder> {

    private final OnTransactionClickListener listener;

    public interface OnTransactionClickListener {
        void onTransactionClick(Transaction transaction);
    }

    public TransactionAdapter(OnTransactionClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<Transaction> DIFF_CALLBACK = new DiffUtil.ItemCallback<Transaction>() {
        @Override
        public boolean areItemsTheSame(@NonNull Transaction oldItem, @NonNull Transaction newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Transaction oldItem, @NonNull Transaction newItem) {
            return oldItem.getAmount() == newItem.getAmount()
                    && oldItem.getType() == newItem.getType()
                    && oldItem.getDescription().equals(newItem.getDescription());
        }
    };

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class TransactionViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardView;
        private final ImageView ivIcon;
        private final TextView tvDescription;
        private final TextView tvAmount;
        private final TextView tvDate;

        TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.card_transaction);
            ivIcon = itemView.findViewById(R.id.iv_icon);
            tvDescription = itemView.findViewById(R.id.tv_description);
            tvAmount = itemView.findViewById(R.id.tv_amount);
            tvDate = itemView.findViewById(R.id.tv_date);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onTransactionClick(getItem(position));
                }
            });
        }

        void bind(Transaction transaction) {
            tvDescription.setText(transaction.getDescription());
            tvDate.setText(PersianDateUtils.formatDate(transaction.getDate()));

            if (transaction.getType() == Transaction.TYPE_EXPENSE) {
                tvAmount.setText("-" + CurrencyUtils.formatToman(transaction.getAmount()));
                tvAmount.setTextColor(Color.parseColor("#F44336"));
            } else {
                tvAmount.setText("+" + CurrencyUtils.formatToman(transaction.getAmount()));
                tvAmount.setTextColor(Color.parseColor("#4CAF50"));
            }
        }
    }
}

