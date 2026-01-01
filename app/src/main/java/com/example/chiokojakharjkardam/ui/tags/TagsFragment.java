package com.example.chiokojakharjkardam.ui.tags;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.chiokojakharjkardam.R;
import com.example.chiokojakharjkardam.data.database.entity.Tag;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

public class TagsFragment extends Fragment {

    private TagsViewModel viewModel;
    private ChipGroup chipGroupTags;
    private LinearLayout layoutEmpty;

    // رنگ‌های پیش‌فرض برای تگ‌ها
    private final String[] TAG_COLORS = {"#2196F3", "#4CAF50", "#FF9800", "#E91E63", "#9C27B0", "#00BCD4", "#795548"};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tags, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(TagsViewModel.class);

        initViews(view);
        setupListeners(view);
        observeData();
    }

    private void initViews(View view) {
        chipGroupTags = view.findViewById(R.id.chip_group_tags);
        layoutEmpty = view.findViewById(R.id.layout_empty);
    }

    private void setupListeners(View view) {
        FloatingActionButton fabAdd = view.findViewById(R.id.fab_add_tag);
        fabAdd.setOnClickListener(v -> showAddTagDialog());
    }

    private void observeData() {
        viewModel.getAllTags().observe(getViewLifecycleOwner(), tags -> {
            chipGroupTags.removeAllViews();

            if (tags != null && !tags.isEmpty()) {
                layoutEmpty.setVisibility(View.GONE);
                chipGroupTags.setVisibility(View.VISIBLE);

                for (Tag tag : tags) {
                    Chip chip = new Chip(requireContext());
                    chip.setText(tag.getName());
                    chip.setCloseIconVisible(true);
                    chip.setClickable(true);

                    // کلیک برای ویرایش
                    chip.setOnClickListener(v -> showEditTagDialog(tag));

                    // حذف با دکمه ضربدر
                    chip.setOnCloseIconClickListener(v -> showDeleteConfirmDialog(tag));

                    chipGroupTags.addView(chip);
                }
            } else {
                layoutEmpty.setVisibility(View.VISIBLE);
                chipGroupTags.setVisibility(View.GONE);
            }
        });
    }

    private void showAddTagDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_tag, null);

        TextInputEditText etTagName = dialogView.findViewById(R.id.et_tag_name);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.add_tag)
                .setView(dialogView)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String name = etTagName.getText().toString().trim();
                    if (!name.isEmpty()) {
                        // رنگ تصادفی
                        String color = TAG_COLORS[(int) (Math.random() * TAG_COLORS.length)];
                        Tag tag = new Tag(name, color);
                        viewModel.insertTag(tag);
                        Toast.makeText(requireContext(), "تگ اضافه شد", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "نام تگ را وارد کنید", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showEditTagDialog(Tag tag) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_tag, null);

        TextInputEditText etTagName = dialogView.findViewById(R.id.et_tag_name);
        etTagName.setText(tag.getName());

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("ویرایش تگ")
                .setView(dialogView)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String name = etTagName.getText().toString().trim();
                    if (!name.isEmpty()) {
                        tag.setName(name);
                        viewModel.updateTag(tag);
                        Toast.makeText(requireContext(), "تگ ویرایش شد", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showDeleteConfirmDialog(Tag tag) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("حذف تگ")
                .setMessage("آیا از حذف «" + tag.getName() + "» مطمئن هستید؟")
                .setPositiveButton("حذف", (dialog, which) -> {
                    viewModel.deleteTag(tag);
                    Toast.makeText(requireContext(), "تگ حذف شد", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}

