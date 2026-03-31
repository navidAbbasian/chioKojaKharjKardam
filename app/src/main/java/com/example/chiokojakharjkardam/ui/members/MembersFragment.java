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
import com.example.chiokojakharjkardam.data.database.entity.Family;
import com.example.chiokojakharjkardam.ui.adapters.MemberAdapter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import android.widget.TextView;

public class MembersFragment extends Fragment {

    private MembersViewModel viewModel;
    private RecyclerView rvMembers;
    private MemberAdapter adapter;

    private MaterialCardView cardFamilyHeader;
    private TextView tvFamilyName;
    private MaterialButton btnEditFamily;
    private View layoutNoFamily;
    private View layoutMembers;
    private MaterialButton btnCreateFamily;
    private FloatingActionButton fabAdd;

    private Family currentFamily = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_members, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(MembersViewModel.class);

        initViews(view);
        setupRecyclerView();
        setupListeners();
        observeData();
    }

    private void initViews(View view) {
        cardFamilyHeader = view.findViewById(R.id.card_family_header);
        tvFamilyName = view.findViewById(R.id.tv_family_name);
        btnEditFamily = view.findViewById(R.id.btn_edit_family);
        layoutNoFamily = view.findViewById(R.id.layout_no_family);
        layoutMembers = view.findViewById(R.id.layout_members);
        rvMembers = view.findViewById(R.id.rv_members);
        btnCreateFamily = view.findViewById(R.id.btn_create_family);
        fabAdd = view.findViewById(R.id.fab_add_member);
    }

    private void setupRecyclerView() {
        adapter = new MemberAdapter(member -> {
            Bundle args = new Bundle();
            args.putLong("memberId", member.getId());
            Navigation.findNavController(requireView()).navigate(R.id.addMemberFragment, args);
        });
        adapter.setOnMemberLongClickListener(member ->
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.confirm_delete)
                        .setMessage(member.getName())
                        .setPositiveButton(R.string.delete, (d, w) -> viewModel.deleteMember(member))
                        .setNegativeButton(R.string.cancel, null)
                        .show()
        );
        rvMembers.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvMembers.setAdapter(adapter);
    }

    private void setupListeners() {
        fabAdd.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.addMemberFragment)
        );

        btnEditFamily.setOnClickListener(v -> showEditFamilyDialog());

        btnCreateFamily.setOnClickListener(v -> showCreateFamilyDialog());
    }

    private void observeData() {
        viewModel.getFamily().observe(getViewLifecycleOwner(), family -> {
            currentFamily = family;
            if (family != null) {
                // خانواده وجود دارد
                cardFamilyHeader.setVisibility(View.VISIBLE);
                tvFamilyName.setText(family.getName());
                layoutNoFamily.setVisibility(View.GONE);
                layoutMembers.setVisibility(View.VISIBLE);
            } else {
                // خانواده وجود ندارد
                cardFamilyHeader.setVisibility(View.GONE);
                layoutNoFamily.setVisibility(View.VISIBLE);
                layoutMembers.setVisibility(View.GONE);
            }
        });

        viewModel.getAllMembers().observe(getViewLifecycleOwner(), members -> {
            if (members != null) {
                adapter.submitList(members);
            }
        });
    }

    private void showEditFamilyDialog() {
        if (currentFamily == null) return;

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_family, null);
        TextInputEditText etFamilyName = dialogView.findViewById(R.id.et_family_name);
        TextInputLayout tilFamilyName = dialogView.findViewById(R.id.til_family_name);
        etFamilyName.setText(currentFamily.getName());

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.edit_family_title)
                .setView(dialogView)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String name = etFamilyName.getText() != null
                            ? etFamilyName.getText().toString().trim() : "";
                    if (name.isEmpty()) {
                        tilFamilyName.setError(getString(R.string.family_name_required));
                        return;
                    }
                    currentFamily.setName(name);
                    viewModel.updateFamily(currentFamily);
                    Toast.makeText(requireContext(), R.string.family_updated, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showCreateFamilyDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_create_family, null);
        TextInputEditText etFamilyName = dialogView.findViewById(R.id.et_family_name);
        TextInputEditText etOwnerName = dialogView.findViewById(R.id.et_owner_name);
        TextInputLayout tilFamilyName = dialogView.findViewById(R.id.til_family_name);
        TextInputLayout tilOwnerName = dialogView.findViewById(R.id.til_owner_name);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.create_family)
                .setView(dialogView)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String familyName = etFamilyName.getText() != null
                            ? etFamilyName.getText().toString().trim() : "";
                    String ownerName = etOwnerName.getText() != null
                            ? etOwnerName.getText().toString().trim() : "";

                    if (familyName.isEmpty()) {
                        tilFamilyName.setError(getString(R.string.family_name_required));
                        return;
                    }
                    if (ownerName.isEmpty()) {
                        tilOwnerName.setError(getString(R.string.owner_name_required));
                        return;
                    }

                    viewModel.createFamilyWithOwner(familyName, ownerName,
                            com.example.chiokojakharjkardam.utils.Constants.MEMBER_COLORS[0],
                            new MembersViewModel.OnSetupCompleteListener() {
                                @Override
                                public void onComplete() {
                                    if (isAdded()) {
                                        requireActivity().runOnUiThread(() ->
                                                Toast.makeText(requireContext(),
                                                        R.string.family_created, Toast.LENGTH_SHORT).show()
                                        );
                                    }
                                }

                                @Override
                                public void onError(String error) {
                                    if (isAdded()) {
                                        requireActivity().runOnUiThread(() ->
                                                Toast.makeText(requireContext(),
                                                        R.string.error, Toast.LENGTH_SHORT).show()
                                        );
                                    }
                                }
                            });
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
