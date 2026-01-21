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

                        Category category = new Category(name, "", "#4CAF50", type);
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
                        category.setType(type);
                        viewModel.updateCategory(category);
                        Toast.makeText(requireContext(), "دسته‌بندی ویرایش شد", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showDeleteConfirmDialog(Category category) {
        // ابتدا تعداد تراکنش‌های مرتبط را دریافت می‌کنیم
        viewModel.getTransactionCount(category.getId(), count -> {
            requireActivity().runOnUiThread(() -> {
                String message;
                if (count > 0) {
                    message = "با حذف دسته‌بندی «" + category.getName() + "»، تعداد " + count +
                            " تراکنش مرتبط با آن نیز حذف خواهد شد و موجودی کارت‌ها بازگردانده می‌شود.\n\nآیا مطمئن هستید؟";
                } else {
                    message = "آیا از حذف «" + category.getName() + "» مطمئن هستید؟";
                }

                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("حذف دسته‌بندی")
                        .setMessage(message)
                        .setPositiveButton("حذف", (dialog, which) -> {
                            viewModel.deleteCategory(category);
                            Toast.makeText(requireContext(), "دسته‌بندی حذف شد", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show();
            });
        });
    }
}

