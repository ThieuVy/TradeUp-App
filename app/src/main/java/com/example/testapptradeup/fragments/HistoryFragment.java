package com.example.testapptradeup.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.viewpager2.widget.ViewPager2;

import com.example.testapptradeup.R;
import com.example.testapptradeup.adapters.HistoryPagerAdapter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class HistoryFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        NavController navController = Navigation.findNavController(view);
        MaterialToolbar toolbar = view.findViewById(R.id.toolbar_history);
        TabLayout tabLayout = view.findViewById(R.id.tab_layout_history);
        ViewPager2 viewPager = view.findViewById(R.id.view_pager_history);

        toolbar.setNavigationOnClickListener(v -> navController.popBackStack());

        HistoryPagerAdapter adapter = new HistoryPagerAdapter(requireActivity());
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) {
                tab.setText("Lịch sử mua");
            } else {
                tab.setText("Lịch sử bán");
            }
        }).attach();
    }
}