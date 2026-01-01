package com.example.chiokojakharjkardam.ui.cards;

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
import com.example.chiokojakharjkardam.ui.adapters.CardAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class CardsFragment extends Fragment {

    private CardsViewModel viewModel;
    private RecyclerView rvCards;
    private LinearLayout layoutEmpty;
    private CardAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cards, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(CardsViewModel.class);

        initViews(view);
        setupRecyclerView();
        setupListeners(view);
        observeData();
    }

    private void initViews(View view) {
        rvCards = view.findViewById(R.id.rv_cards);
        layoutEmpty = view.findViewById(R.id.layout_empty);
    }

    private void setupRecyclerView() {
        adapter = new CardAdapter(card -> {
            // کلیک روی کارت - ویرایش
            Bundle args = new Bundle();
            args.putLong("cardId", card.getId());
            Navigation.findNavController(requireView())
                    .navigate(R.id.addCardFragment, args);
        });
        rvCards.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvCards.setAdapter(adapter);
    }

    private void setupListeners(View view) {
        FloatingActionButton fabAdd = view.findViewById(R.id.fab_add_card);
        fabAdd.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.addCardFragment);
        });
    }

    private void observeData() {
        viewModel.getAllCardsWithMember().observe(getViewLifecycleOwner(), cards -> {
            if (cards != null && !cards.isEmpty()) {
                adapter.submitList(cards);
                rvCards.setVisibility(View.VISIBLE);
                layoutEmpty.setVisibility(View.GONE);
            } else {
                rvCards.setVisibility(View.GONE);
                layoutEmpty.setVisibility(View.VISIBLE);
            }
        });
    }
}

