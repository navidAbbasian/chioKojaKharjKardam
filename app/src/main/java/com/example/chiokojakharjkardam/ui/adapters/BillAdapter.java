package com.example.chiokojakharjkardam.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chiokojakharjkardam.R;
import com.example.chiokojakharjkardam.data.database.entity.Bill;
import com.example.chiokojakharjkardam.utils.CurrencyUtils;
import com.example.chiokojakharjkardam.utils.PersianDateUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

public class BillAdapter extends ListAdapter<Bill, BillAdapter.BillViewHolder> {

    private final OnBillClickListener clickListener;
    private final OnBillPayListener payListener;

    public interface OnBillClickListener {
        void onBillClick(Bill bill);
    }

    public interface OnBillPayListener {
        void onBillPay(Bill bill);
    }

    public BillAdapter(OnBillClickListener clickListener, OnBillPayListener payListener) {
        super(DIFF_CALLBACK);
        this.clickListener = clickListener;
        this.payListener = payListener;
    }

    private static final DiffUtil.ItemCallback<Bill> DIFF_CALLBACK = new DiffUtil.ItemCallback<Bill>() {
        @Override
        public boolean areItemsTheSame(@NonNull Bill oldItem, @NonNull Bill newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Bill oldItem, @NonNull Bill newItem) {
            return oldItem.getAmount() == newItem.getAmount()
                    && oldItem.getTitle().equals(newItem.getTitle())
                    && oldItem.isPaid() == newItem.isPaid();
        }
    };

    @NonNull
    @Override
    public BillViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_bill, parent, false);
        return new BillViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BillViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class BillViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardView;
        private final TextView tvTitle;
        private final TextView tvAmount;
        private final TextView tvDueDate;
        private final MaterialButton btnPay;

        BillViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.card_bill);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvAmount = itemView.findViewById(R.id.tv_amount);
            tvDueDate = itemView.findViewById(R.id.tv_due_date);
            btnPay = itemView.findViewById(R.id.btn_pay);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && clickListener != null) {
                    clickListener.onBillClick(getItem(position));
                }
            });

            btnPay.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && payListener != null) {
                    payListener.onBillPay(getItem(position));
                }
            });
        }

        void bind(Bill bill) {
            tvTitle.setText(bill.getTitle());
            tvAmount.setText(CurrencyUtils.formatToman(bill.getAmount()));
            tvDueDate.setText(PersianDateUtils.formatDate(bill.getDueDate()));

            if (bill.isPaid()) {
                btnPay.setVisibility(View.GONE);
            } else {
                btnPay.setVisibility(View.VISIBLE);
            }
        }
    }
}

