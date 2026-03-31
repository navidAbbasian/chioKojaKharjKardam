package com.example.chiokojakharjkardam.ui.members;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chiokojakharjkardam.R;
import com.example.chiokojakharjkardam.data.database.AppDatabase;
import com.example.chiokojakharjkardam.data.database.entity.Member;
import com.example.chiokojakharjkardam.ui.adapters.ColorAdapter;
import com.example.chiokojakharjkardam.utils.Constants;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;

public class AddMemberFragment extends Fragment {

    private AddMemberViewModel viewModel;
    private TextInputEditText etName;
    private RecyclerView rvColors;
    private MaterialButton btnSave;
    private MaterialSwitch switchOwner;
    private MaterialToolbar toolbar;

    private String selectedColor = Constants.MEMBER_COLORS[0];
    private long editMemberId = -1;
    private long currentFamilyId = -1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_member, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AddMemberViewModel.class);

        if (getArguments() != null) {
            editMemberId = getArguments().getLong("memberId", -1);
        }

        initViews(view);
        setupListeners();

        if (editMemberId != -1) {
            loadMember();
        }

        viewModel.getFamily().observe(getViewLifecycleOwner(), family -> {
            if (family != null) currentFamilyId = family.getId();
        });
    }

    private void initViews(View view) {
        toolbar = view.findViewById(R.id.toolbar);
        etName = view.findViewById(R.id.et_name);
        rvColors = view.findViewById(R.id.rv_colors);
        btnSave = view.findViewById(R.id.btn_save);
        switchOwner = view.findViewById(R.id.switch_owner);

        toolbar.setTitle(editMemberId != -1 ? getString(R.string.edit_member) : getString(R.string.add_member));

        ColorAdapter colorAdapter = new ColorAdapter(Constants.MEMBER_COLORS, color -> selectedColor = color);
        rvColors.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvColors.setAdapter(colorAdapter);
    }

    private void setupListeners() {
        toolbar.setNavigationOnClickListener(v ->
                requireActivity().getOnBackPressedDispatcher().onBackPressed()
        );
        btnSave.setOnClickListener(v -> saveMember());
    }

    private void loadMember() {
        viewModel.getMemberById(editMemberId).observe(getViewLifecycleOwner(), member -> {
            if (member != null) {
                etName.setText(member.getName());
                selectedColor = member.getAvatarColor();
                switchOwner.setChecked(member.isOwner());
            }
        });
    }

    private void saveMember() {
        String name = etName.getText() != null ? etName.getText().toString().trim() : "";
        boolean isOwner = switchOwner.isChecked();

        if (name.isEmpty()) {
            etName.setError(getString(R.string.member_name_required));
            return;
        }

        if (currentFamilyId == -1) {
            Toast.makeText(requireContext(), R.string.no_family_error, Toast.LENGTH_SHORT).show();
            return;
        }

        if (editMemberId != -1) {
            // ویرایش
            Member member = new Member(currentFamilyId, name, isOwner, selectedColor);
            member.setId(editMemberId);
            if (isOwner) {
                // ابتدا همه owner ها را پاک کن، سپس update کن
                AppDatabase.databaseWriteExecutor.execute(() -> {
                    AppDatabase db = AppDatabase.getDatabase(requireContext().getApplicationContext());
                    db.memberDao().clearAllOwners();
                    db.memberDao().update(member);
                });
            } else {
                viewModel.updateMember(member);
            }
            Toast.makeText(requireContext(), R.string.member_saved, Toast.LENGTH_SHORT).show();
            Navigation.findNavController(requireView()).popBackStack();
        } else {
            // افزودن عضو جدید
            Member member = new Member(currentFamilyId, name, isOwner, selectedColor);
            if (isOwner) {
                AppDatabase.databaseWriteExecutor.execute(() -> {
                    AppDatabase db = AppDatabase.getDatabase(requireContext().getApplicationContext());
                    db.memberDao().clearAllOwners();
                    db.memberDao().insert(member);
                });
            } else {
                viewModel.insertMember(member);
            }
            Toast.makeText(requireContext(), R.string.member_saved, Toast.LENGTH_SHORT).show();
            Navigation.findNavController(requireView()).popBackStack();
        }
    }
}
