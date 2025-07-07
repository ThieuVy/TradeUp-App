package com.example.testapptradeup.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.testapptradeup.fragments.PurchaseHistoryFragment;
import com.example.testapptradeup.fragments.SalesHistoryFragment;

public class HistoryPagerAdapter extends FragmentStateAdapter {

    public HistoryPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0) {
            return new PurchaseHistoryFragment();
        } else {
            return new SalesHistoryFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 2; // Chúng ta có 2 tab: Mua và Bán
    }
}