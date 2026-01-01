package com.example.chiokojakharjkardam.ui.members;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chiokojakharjkardam.R;
import com.example.chiokojakharjkardam.ui.adapters.MemberAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MembersFragment extends Fragment {

    private MembersViewModel viewModel;
    private RecyclerView rvMembers;
    private MemberAdapter adapter;

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
        setupListeners(view);
        observeData();
    }

    private void initViews(View view) {
        rvMembers = view.findViewById(R.id.rv_members);
    }

    private void setupRecyclerView() {
        adapter = new MemberAdapter(member -> {
            Bundle args = new Bundle();
            args.putLong("memberId", member.getId());
            Navigation.findNavController(requireView())
                    .navigate(R.id.addMemberFragment, args);
        });
        rvMembers.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvMembers.setAdapter(adapter);
    }

    private void setupListeners(View view) {
        FloatingActionButton fabAdd = view.findViewById(R.id.fab_add_member);
        fabAdd.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.addMemberFragment);
        });
    }

    private void observeData() {
        viewModel.getAllMembers().observe(getViewLifecycleOwner(), members -> {
            if (members != null) {
                adapter.submitList(members);
            }
        });
    }
}

