package com.example.chiokojakharjkardam.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.chiokojakharjkardam.R;
import com.google.android.material.card.MaterialCardView;

public class SettingsFragment extends Fragment {

    private SettingsViewModel viewModel;
    private TextView tvFamilyName;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(SettingsViewModel.class);

        initViews(view);
        setupListeners(view);
        observeData();
    }

    private void initViews(View view) {
        tvFamilyName = view.findViewById(R.id.tv_family_name);
    }

    private void setupListeners(View view) {
        MaterialCardView cardFamily = view.findViewById(R.id.card_family);
        cardFamily.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.membersFragment);
        });

        MaterialCardView cardCategories = view.findViewById(R.id.card_categories);
        cardCategories.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.categoriesFragment);
        });

        MaterialCardView cardTags = view.findViewById(R.id.card_tags);
        cardTags.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.tagsFragment);
        });

        MaterialCardView cardBackup = view.findViewById(R.id.card_backup);
        cardBackup.setOnClickListener(v -> {
            // TODO: Implement backup
        });

        MaterialCardView cardRestore = view.findViewById(R.id.card_restore);
        cardRestore.setOnClickListener(v -> {
            // TODO: Implement restore
        });

        MaterialCardView cardAbout = view.findViewById(R.id.card_about);
        cardAbout.setOnClickListener(v -> {
            // TODO: Show about dialog
        });
    }

    private void observeData() {
        viewModel.getFamily().observe(getViewLifecycleOwner(), family -> {
            if (family != null) {
                tvFamilyName.setText(family.getName());
            }
        });
    }
}

