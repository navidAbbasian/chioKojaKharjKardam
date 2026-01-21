package com.example.chiokojakharjkardam.ui.bills;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chiokojakharjkardam.R;
import com.example.chiokojakharjkardam.ui.adapters.BillAdapter;
import com.example.chiokojakharjkardam.utils.PersianDateUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

public class BillsFragment extends Fragment {

    private BillsViewModel viewModel;
    private RecyclerView rvBills;
    private RecyclerView rvFutureBills;
    private LinearLayout layoutEmpty;
    private LinearLayout layoutFutureBills;
    private LinearLayout headerFutureBills;
    private ImageView ivExpandArrow;
    private TextView tvThisMonthTitle;
    private TextView tvFutureBillsCount;
    private TabLayout tabLayout;
    private BillAdapter adapter;
    private BillAdapter futureBillsAdapter;
    private boolean isFutureBillsExpanded = false;

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
        rvFutureBills = view.findViewById(R.id.rv_future_bills);
        layoutEmpty = view.findViewById(R.id.layout_empty);
        layoutFutureBills = view.findViewById(R.id.layout_future_bills);
        headerFutureBills = view.findViewById(R.id.header_future_bills);
        ivExpandArrow = view.findViewById(R.id.iv_expand_arrow);
        tvThisMonthTitle = view.findViewById(R.id.tv_this_month_title);
        tvFutureBillsCount = view.findViewById(R.id.tv_future_bills_count);
        tabLayout = view.findViewById(R.id.tab_layout);
    }

    private void setupRecyclerView() {
        // آداپتر قبض‌های ماه جاری
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

        // آداپتر قبض‌های آینده
        futureBillsAdapter = new BillAdapter(
                bill -> {
                    Bundle args = new Bundle();
                    args.putLong("billId", bill.getId());
                    Navigation.findNavController(requireView())
                            .navigate(R.id.addBillFragment, args);
                },
                bill -> {
                    viewModel.markAsPaid(bill);
                }
        );
        rvFutureBills.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvFutureBills.setAdapter(futureBillsAdapter);
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

        // کلیک روی هدر قبض‌های آینده برای expand/collapse
        headerFutureBills.setOnClickListener(v -> {
            isFutureBillsExpanded = !isFutureBillsExpanded;
            updateFutureBillsExpandState();
        });
    }

    private void updateFutureBillsExpandState() {
        if (isFutureBillsExpanded) {
            rvFutureBills.setVisibility(View.VISIBLE);
            ivExpandArrow.setImageResource(R.drawable.ic_expand_less);
        } else {
            rvFutureBills.setVisibility(View.GONE);
            ivExpandArrow.setImageResource(R.drawable.ic_expand_more);
        }
    }

    private void observeData() {
        // مشاهده قبض‌های ماه جاری
        viewModel.getCurrentMonthBills().observe(getViewLifecycleOwner(), bills -> {
            if (bills != null && !bills.isEmpty()) {
                adapter.submitList(bills);
                rvBills.setVisibility(View.VISIBLE);
                tvThisMonthTitle.setVisibility(View.VISIBLE);
            } else {
                rvBills.setVisibility(View.GONE);
                tvThisMonthTitle.setVisibility(View.GONE);
            }
            updateEmptyState();
        });

        // مشاهده قبض‌های آینده
        viewModel.getFutureBills().observe(getViewLifecycleOwner(), bills -> {
            if (bills != null && !bills.isEmpty()) {
                futureBillsAdapter.submitList(bills);
                layoutFutureBills.setVisibility(View.VISIBLE);
                tvFutureBillsCount.setText(PersianDateUtils.toPersianDigits(String.valueOf(bills.size())) + " قبض");
            } else {
                layoutFutureBills.setVisibility(View.GONE);
            }
            updateEmptyState();
        });
    }

    private void updateEmptyState() {
        boolean hasCurrentMonth = adapter.getItemCount() > 0;
        boolean hasFuture = futureBillsAdapter.getItemCount() > 0;

        if (!hasCurrentMonth && !hasFuture) {
            layoutEmpty.setVisibility(View.VISIBLE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
        }
    }
}

