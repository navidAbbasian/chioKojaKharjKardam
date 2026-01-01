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
import com.example.chiokojakharjkardam.data.database.entity.Member;
import com.google.android.material.card.MaterialCardView;

public class MemberAdapter extends ListAdapter<Member, MemberAdapter.MemberViewHolder> {

    private final OnMemberClickListener listener;

    public interface OnMemberClickListener {
        void onMemberClick(Member member);
    }

    public MemberAdapter(OnMemberClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<Member> DIFF_CALLBACK = new DiffUtil.ItemCallback<Member>() {
        @Override
        public boolean areItemsTheSame(@NonNull Member oldItem, @NonNull Member newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Member oldItem, @NonNull Member newItem) {
            return oldItem.getName().equals(newItem.getName())
                    && oldItem.isOwner() == newItem.isOwner();
        }
    };

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_member, parent, false);
        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class MemberViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardView;
        private final View avatarView;
        private final TextView tvInitial;
        private final TextView tvName;
        private final TextView tvBadge;

        MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.card_member);
            avatarView = itemView.findViewById(R.id.view_avatar);
            tvInitial = itemView.findViewById(R.id.tv_initial);
            tvName = itemView.findViewById(R.id.tv_name);
            tvBadge = itemView.findViewById(R.id.tv_badge);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onMemberClick(getItem(position));
                }
            });
        }

        void bind(Member member) {
            tvName.setText(member.getName());

            // اولین حرف نام
            String initial = member.getName().isEmpty() ? "?" : member.getName().substring(0, 1);
            tvInitial.setText(initial);

            // رنگ آواتار
            try {
                avatarView.setBackgroundColor(Color.parseColor(member.getAvatarColor()));
            } catch (Exception e) {
                avatarView.setBackgroundColor(Color.GRAY);
            }

            // نمایش برچسب صاحب برنامه
            if (member.isOwner()) {
                tvBadge.setVisibility(View.VISIBLE);
            } else {
                tvBadge.setVisibility(View.GONE);
            }
        }
    }
}

