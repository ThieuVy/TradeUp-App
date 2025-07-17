package com.example.testapptradeup.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.example.testapptradeup.fragments.PurchaseHistoryFragment;
import com.example.testapptradeup.fragments.SalesHistoryFragment;

public class HistoryPagerAdapter extends FragmentStateAdapter {

    public HistoryPagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0) {
            return new PurchaseHistoryFragment(); // Tab đầu tiên: Lịch sử mua
        } else {
            return new SalesHistoryFragment(); // Tab thứ hai: Lịch sử bán
        }
    }

    @Override
    public int getItemCount() {
        return 2; // Chúng ta có 2 tab
    }
}