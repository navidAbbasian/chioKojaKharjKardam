package com.example.chiokojakharjkardam.ui.categories;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chiokojakharjkardam.R;
import com.example.chiokojakharjkardam.data.database.entity.Category;
import com.example.chiokojakharjkardam.ui.adapters.CategoryListAdapter;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

public class CategoriesFragment extends Fragment {

    private CategoriesViewModel viewModel;
    private RecyclerView rvCategories;
    private CategoryListAdapter adapter;

    // آیکون‌های پیش‌فرض برای دسته‌بندی
    private final String[] ICONS = {"🍎", "🚗", "📱", "🏠", "👕", "💊", "🎬", "📚", "🎁", "📦", "💰", "💵"};
    private int selectedIconIndex = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_categories, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(CategoriesViewModel.class);

        initViews(view);
        setupRecyclerView();
        setupListeners(view);
        observeData();
    }

    private void initViews(View view) {
        rvCategories = view.findViewById(R.id.rv_categories);
    }

    private void setupRecyclerView() {
        adapter = new CategoryListAdapter(this::showEditOrDeleteDialog);
        rvCategories.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvCategories.setAdapter(adapter);
    }

    private void setupListeners(View view) {
        FloatingActionButton fabAdd = view.findViewById(R.id.fab_add_category);
        fabAdd.setOnClickListener(v -> showAddCategoryDialog());
    }

    private void observeData() {
        viewModel.getAllCategories().observe(getViewLifecycleOwner(), categories -> {
            if (categories != null) {
                adapter.submitList(categories);
            }
        });
    }

    private void showAddCategoryDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_category, null);

        TextInputEditText etName = dialogView.findViewById(R.id.et_category_name);
        RadioGroup rgType = dialogView.findViewById(R.id.rg_type);
        RecyclerView rvIcons = dialogView.findViewById(R.id.rv_icons);

        // تنظیم آیکون‌ها
        setupIconsRecyclerView(rvIcons);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.add_category)
                .setView(dialogView)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    if (!name.isEmpty()) {
                        int type = Category.TYPE_EXPENSE;
                        if (rgType.getCheckedRadioButtonId() == R.id.rb_income) {
                            type = Category.TYPE_INCOME;
                        } else if (rgType.getCheckedRadioButtonId() == R.id.rb_both) {
                            type = Category.TYPE_BOTH;
                        }

                        Category category = new Category(name, ICONS[selectedIconIndex], "#4CAF50", type);
                        viewModel.insertCategory(category);
                        Toast.makeText(requireContext(), "دسته‌بندی اضافه شد", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "نام دسته‌بندی را وارد کنید", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showEditOrDeleteDialog(Category category) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(category.getName())
                .setItems(new String[]{"ویرایش", "حذف"}, (dialog, which) -> {
                    if (which == 0) {
                        showEditCategoryDialog(category);
                    } else {
                        showDeleteConfirmDialog(category);
                    }
                })
                .show();
    }

    private void showEditCategoryDialog(Category category) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_category, null);

        TextInputEditText etName = dialogView.findViewById(R.id.et_category_name);
        RadioGroup rgType = dialogView.findViewById(R.id.rg_type);
        RecyclerView rvIcons = dialogView.findViewById(R.id.rv_icons);

        // مقادیر فعلی
        etName.setText(category.getName());

        // تنظیم نوع
        switch (category.getType()) {
            case Category.TYPE_INCOME:
                rgType.check(R.id.rb_income);
                break;
            case Category.TYPE_BOTH:
                rgType.check(R.id.rb_both);
                break;
            default:
                rgType.check(R.id.rb_expense);
                break;
        }

        // پیدا کردن آیکون فعلی
        for (int i = 0; i < ICONS.length; i++) {
            if (ICONS[i].equals(category.getIcon())) {
                selectedIconIndex = i;
                break;
            }
        }

        setupIconsRecyclerView(rvIcons);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("ویرایش دسته‌بندی")
                .setView(dialogView)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    if (!name.isEmpty()) {
                        int type = Category.TYPE_EXPENSE;
                        if (rgType.getCheckedRadioButtonId() == R.id.rb_income) {
                            type = Category.TYPE_INCOME;
                        } else if (rgType.getCheckedRadioButtonId() == R.id.rb_both) {
                            type = Category.TYPE_BOTH;
                        }

                        category.setName(name);
                        category.setIcon(ICONS[selectedIconIndex]);
                        category.setType(type);
                        viewModel.updateCategory(category);
                        Toast.makeText(requireContext(), "دسته‌بندی ویرایش شد", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showDeleteConfirmDialog(Category category) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("حذف دسته‌بندی")
                .setMessage("آیا از حذف «" + category.getName() + "» مطمئن هستید؟")
                .setPositiveButton("حذف", (dialog, which) -> {
                    viewModel.deleteCategory(category);
                    Toast.makeText(requireContext(), "دسته‌بندی حذف شد", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void setupIconsRecyclerView(RecyclerView rvIcons) {
        IconAdapter iconAdapter = new IconAdapter(ICONS, selectedIconIndex, position -> {
            selectedIconIndex = position;
        });
        rvIcons.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvIcons.setAdapter(iconAdapter);
    }

    // آداپتر ساده برای آیکون‌ها
    private static class IconAdapter extends RecyclerView.Adapter<IconAdapter.IconViewHolder> {
        private final String[] icons;
        private int selectedPosition;
        private final OnIconClickListener listener;

        interface OnIconClickListener {
            void onIconClick(int position);
        }

        IconAdapter(String[] icons, int selectedPosition, OnIconClickListener listener) {
            this.icons = icons;
            this.selectedPosition = selectedPosition;
            this.listener = listener;
        }

        @NonNull
        @Override
        public IconViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_icon, parent, false);
            return new IconViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull IconViewHolder holder, int position) {
            holder.bind(icons[position], position == selectedPosition);
            holder.itemView.setOnClickListener(v -> {
                int oldPosition = selectedPosition;
                selectedPosition = position;
                notifyItemChanged(oldPosition);
                notifyItemChanged(selectedPosition);
                listener.onIconClick(position);
            });
        }

        @Override
        public int getItemCount() {
            return icons.length;
        }

        static class IconViewHolder extends RecyclerView.ViewHolder {
            private final android.widget.TextView tvIcon;
            private final View container;

            IconViewHolder(@NonNull View itemView) {
                super(itemView);
                tvIcon = itemView.findViewById(R.id.tv_icon);
                container = itemView;
            }

            void bind(String icon, boolean isSelected) {
                tvIcon.setText(icon);
                container.setBackgroundResource(isSelected ? R.drawable.circle_shape : 0);
            }
        }
    }
}

