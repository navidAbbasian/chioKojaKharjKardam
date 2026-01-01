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
import com.example.chiokojakharjkardam.data.database.entity.Member;
import com.example.chiokojakharjkardam.ui.adapters.ColorAdapter;
import com.example.chiokojakharjkardam.utils.Constants;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class AddMemberFragment extends Fragment {

    private AddMemberViewModel viewModel;
    private TextInputEditText etName;
    private RecyclerView rvColors;
    private MaterialButton btnSave;

    private String selectedColor = Constants.MEMBER_COLORS[0];
    private long editMemberId = -1;

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
    }

    private void initViews(View view) {
        etName = view.findViewById(R.id.et_name);
        rvColors = view.findViewById(R.id.rv_colors);
        btnSave = view.findViewById(R.id.btn_save);

        ColorAdapter colorAdapter = new ColorAdapter(Constants.MEMBER_COLORS, color -> {
            selectedColor = color;
        });
        rvColors.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvColors.setAdapter(colorAdapter);
    }

    private void setupListeners() {
        btnSave.setOnClickListener(v -> saveMember());
    }

    private void loadMember() {
        viewModel.getMemberById(editMemberId).observe(getViewLifecycleOwner(), member -> {
            if (member != null) {
                etName.setText(member.getName());
                selectedColor = member.getAvatarColor();
            }
        });
    }

    private void saveMember() {
        String name = etName.getText().toString().trim();

        if (name.isEmpty()) {
            etName.setError("نام عضو را وارد کنید");
            return;
        }

        viewModel.getFamily().observe(getViewLifecycleOwner(), family -> {
            if (family != null) {
                Member member = new Member(family.getId(), name, false, selectedColor);

                if (editMemberId != -1) {
                    member.setId(editMemberId);
                    viewModel.updateMember(member);
                } else {
                    viewModel.insertMember(member);
                }

                Toast.makeText(requireContext(), "عضو ذخیره شد", Toast.LENGTH_SHORT).show();
                Navigation.findNavController(requireView()).popBackStack();
            }
        });
    }
}

