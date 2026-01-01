package com.example.chiokojakharjkardam.ui.bills;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chiokojakharjkardam.R;
import com.example.chiokojakharjkardam.ui.adapters.BillAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

public class BillsFragment extends Fragment {

    private BillsViewModel viewModel;
    private RecyclerView rvBills;
    private LinearLayout layoutEmpty;
    private TabLayout tabLayout;
    private BillAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bills, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(BillsViewModel.class);

        initViews(view);
        setupRecyclerView();
        setupListeners(view);
        observeData();
    }

    private void initViews(View view) {
        rvBills = view.findViewById(R.id.rv_bills);
        layoutEmpty = view.findViewById(R.id.layout_empty);
        tabLayout = view.findViewById(R.id.tab_layout);
    }

    private void setupRecyclerView() {
        adapter = new BillAdapter(
                bill -> {
                    // کلیک روی قبض - ویرایش
                    Bundle args = new Bundle();
                    args.putLong("billId", bill.getId());
                    Navigation.findNavController(requireView())
                            .navigate(R.id.addBillFragment, args);
                },
                bill -> {
                    // پرداخت قبض
                    viewModel.markAsPaid(bill);
                }
        );
        rvBills.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvBills.setAdapter(adapter);
    }

    private void setupListeners(View view) {
        FloatingActionButton fabAdd = view.findViewById(R.id.fab_add_bill);
        fabAdd.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.addBillFragment);
        });

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewModel.setShowPaid(tab.getPosition() == 1);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void observeData() {
        viewModel.getFilteredBills().observe(getViewLifecycleOwner(), bills -> {
            if (bills != null && !bills.isEmpty()) {
                adapter.submitList(bills);
                rvBills.setVisibility(View.VISIBLE);
                layoutEmpty.setVisibility(View.GONE);
            } else {
                rvBills.setVisibility(View.GONE);
                layoutEmpty.setVisibility(View.VISIBLE);
            }
        });
    }
}

