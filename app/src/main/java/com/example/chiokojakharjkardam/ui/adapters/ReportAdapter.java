package com.example.chiokojakharjkardam.ui.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chiokojakharjkardam.R;
import com.example.chiokojakharjkardam.data.database.entity.CategoryReport;
import com.example.chiokojakharjkardam.data.database.entity.CombinedReport;
import com.example.chiokojakharjkardam.data.database.entity.TagReport;
import com.google.android.material.chip.Chip;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ReportViewHolder> {

    public static final int TYPE_CATEGORY = 0;
    public static final int TYPE_TAG = 1;
    public static final int TYPE_COMBINED = 2;

    private final Context context;
    private final List<Object> items = new ArrayList<>();
    private int currentType = TYPE_CATEGORY;
    private final NumberFormat numberFormat;

    public ReportAdapter(Context context) {
        this.context = context;
        this.numberFormat = NumberFormat.getNumberInstance(new Locale("fa", "IR"));
    }

    public void setCategoryReports(List<CategoryReport> reports) {
        items.clear();
        if (reports != null) {
            items.addAll(reports);
        }
        currentType = TYPE_CATEGORY;
        notifyDataSetChanged();
    }

    public void setTagReports(List<TagReport> reports) {
        items.clear();
        if (reports != null) {
            items.addAll(reports);
        }
        currentType = TYPE_TAG;
        notifyDataSetChanged();
    }

    public void setCombinedReports(List<CombinedReport> reports) {
        items.clear();
        if (reports != null) {
            items.addAll(reports);
        }
        currentType = TYPE_COMBINED;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_report, parent, false);
        return new ReportViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
        Object item = items.get(position);

        switch (currentType) {
            case TYPE_CATEGORY:
                bindCategoryReport(holder, (CategoryReport) item);
                break;
            case TYPE_TAG:
                bindTagReport(holder, (TagReport) item);
                break;
            case TYPE_COMBINED:
                bindCombinedReport(holder, (CombinedReport) item);
                break;
        }
    }

    private void bindCategoryReport(ReportViewHolder holder, CategoryReport report) {
        // نام
        String name = report.getCategoryName();
        if (name == null || name.isEmpty()) {
            name = context.getString(R.string.no_category);
        }
        holder.tvName.setText(name);

        // مبلغ
        holder.tvAmount.setText(numberFormat.format(report.getTotalAmount()));

        // تعداد تراکنش
        holder.tvTransactionCount.setText(
                context.getString(R.string.transaction_count_format, report.getTransactionCount()));

        // رنگ
        setBackgroundColor(holder.viewColorBg, report.getCategoryColor());

        // آیکون پیش‌فرض
        holder.ivIcon.setImageResource(R.drawable.ic_folder);

        // مخفی کردن تگ
        holder.layoutTags.setVisibility(View.GONE);
    }

    private void bindTagReport(ReportViewHolder holder, TagReport report) {
        // نام
        holder.tvName.setText(report.getTagName());

        // مبلغ
        holder.tvAmount.setText(numberFormat.format(report.getTotalAmount()));

        // تعداد تراکنش
        holder.tvTransactionCount.setText(
                context.getString(R.string.transaction_count_format, report.getTransactionCount()));

        // رنگ
        setBackgroundColor(holder.viewColorBg, report.getTagColor());

        // آیکون تگ
        holder.ivIcon.setImageResource(R.drawable.ic_tag);

        // مخفی کردن تگ
        holder.layoutTags.setVisibility(View.GONE);
    }

    private void bindCombinedReport(ReportViewHolder holder, CombinedReport report) {
        // نام دسته‌بندی
        String categoryName = report.getCategoryName();
        if (categoryName == null || categoryName.isEmpty()) {
            categoryName = context.getString(R.string.no_category);
        }
        holder.tvName.setText(categoryName);

        // مبلغ
        holder.tvAmount.setText(numberFormat.format(report.getTotalAmount()));

        // تعداد تراکنش
        holder.tvTransactionCount.setText(
                context.getString(R.string.transaction_count_format, report.getTransactionCount()));

        // رنگ دسته‌بندی
        setBackgroundColor(holder.viewColorBg, report.getCategoryColor());

        // آیکون پیش‌فرض
        holder.ivIcon.setImageResource(R.drawable.ic_folder);

        // نمایش تگ
        if (report.getTagName() != null && !report.getTagName().isEmpty()) {
            holder.layoutTags.setVisibility(View.VISIBLE);
            holder.chipTag.setText(report.getTagName());

            // رنگ تگ
            if (report.getTagColor() != null && !report.getTagColor().isEmpty()) {
                try {
                    int color = Color.parseColor(report.getTagColor());
                    holder.chipTag.setChipBackgroundColorResource(android.R.color.transparent);
                    holder.chipTag.setChipStrokeColorResource(android.R.color.transparent);
                    holder.chipTag.setChipBackgroundColor(
                            android.content.res.ColorStateList.valueOf(adjustAlpha(color, 0.2f)));
                    holder.chipTag.setTextColor(color);
                } catch (Exception ignored) {
                }
            }
        } else {
            holder.layoutTags.setVisibility(View.GONE);
        }
    }

    private void setBackgroundColor(View view, String colorHex) {
        int color;
        try {
            if (colorHex != null && !colorHex.isEmpty()) {
                color = Color.parseColor(colorHex);
            } else {
                color = context.getColor(R.color.primary);
            }
        } catch (Exception e) {
            color = context.getColor(R.color.primary);
        }

        GradientDrawable drawable = (GradientDrawable) view.getBackground();
        if (drawable != null) {
            drawable.setColor(color);
        } else {
            view.setBackgroundColor(color);
        }
    }


    private int adjustAlpha(int color, float factor) {
        int alpha = Math.round(Color.alpha(color) * factor);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public long getTotalAmount() {
        long total = 0;
        for (Object item : items) {
            if (item instanceof CategoryReport) {
                total += ((CategoryReport) item).getTotalAmount();
            } else if (item instanceof TagReport) {
                total += ((TagReport) item).getTotalAmount();
            } else if (item instanceof CombinedReport) {
                total += ((CombinedReport) item).getTotalAmount();
            }
        }
        return total;
    }

    static class ReportViewHolder extends RecyclerView.ViewHolder {
        View viewColorBg;
        ImageView ivIcon;
        TextView tvName;
        TextView tvTransactionCount;
        LinearLayout layoutTags;
        Chip chipTag;
        TextView tvAmount;

        ReportViewHolder(@NonNull View itemView) {
            super(itemView);
            viewColorBg = itemView.findViewById(R.id.view_color_bg);
            ivIcon = itemView.findViewById(R.id.iv_icon);
            tvName = itemView.findViewById(R.id.tv_name);
            tvTransactionCount = itemView.findViewById(R.id.tv_transaction_count);
            layoutTags = itemView.findViewById(R.id.layout_tags);
            chipTag = itemView.findViewById(R.id.chip_tag);
            tvAmount = itemView.findViewById(R.id.tv_amount);
        }
    }
}

