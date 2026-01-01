package com.example.chiokojakharjkardam.ui.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chiokojakharjkardam.R;
import com.google.android.material.card.MaterialCardView;

public class ColorAdapter extends RecyclerView.Adapter<ColorAdapter.ColorViewHolder> {

    private final String[] colors;
    private final OnColorSelectedListener listener;
    private int selectedPosition = 0;

    public interface OnColorSelectedListener {
        void onColorSelected(String color);
    }

    public ColorAdapter(String[] colors, OnColorSelectedListener listener) {
        this.colors = colors;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ColorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_color, parent, false);
        return new ColorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ColorViewHolder holder, int position) {
        holder.bind(colors[position], position == selectedPosition);
    }

    @Override
    public int getItemCount() {
        return colors.length;
    }

    class ColorViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardView;

        ColorViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.card_color);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    int oldPosition = selectedPosition;
                    selectedPosition = position;
                    notifyItemChanged(oldPosition);
                    notifyItemChanged(selectedPosition);
                    if (listener != null) {
                        listener.onColorSelected(colors[position]);
                    }
                }
            });
        }

        void bind(String color, boolean isSelected) {
            try {
                cardView.setCardBackgroundColor(Color.parseColor(color));
            } catch (Exception e) {
                cardView.setCardBackgroundColor(Color.GRAY);
            }

            if (isSelected) {
                cardView.setStrokeWidth(4);
            } else {
                cardView.setStrokeWidth(0);
            }
        }
    }
}

