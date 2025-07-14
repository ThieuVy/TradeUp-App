package com.example.testapptradeup.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.testapptradeup.R;
import com.example.testapptradeup.models.User;
import com.example.testapptradeup.viewmodels.MainViewModel;
import com.google.android.material.appbar.MaterialToolbar;

public class PersonalInfoFragment extends Fragment {

    private MainViewModel mainViewModel;
    private NavController navController;

    // UI Components
    private TextView textDisplayName, textEmail, textPhone, textBio, textAddress;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Lấy ViewModel được chia sẻ từ MainActivity
        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate layout mới
        return inflater.inflate(R.layout.fragment_personal_info, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);

        initViews(view);
        observeViewModel();
    }

    private void initViews(View view) {
        MaterialToolbar toolbar = view.findViewById(R.id.toolbar_personal_info);
        toolbar.setNavigationOnClickListener(v -> navController.popBackStack());

        textDisplayName = view.findViewById(R.id.text_info_display_name);
        textEmail = view.findViewById(R.id.text_info_email);
        textPhone = view.findViewById(R.id.text_info_phone);
        textBio = view.findViewById(R.id.text_info_bio);
        textAddress = view.findViewById(R.id.text_info_address);
    }

    private void observeViewModel() {
        mainViewModel.getCurrentUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                populateUI(user);
            } else {
                Toast.makeText(getContext(), "Không thể tải dữ liệu người dùng.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void populateUI(User user) {
        textDisplayName.setText(user.getName() != null && !user.getName().isEmpty() ? user.getName() : "Chưa cập nhật");
        textEmail.setText(user.getEmail() != null && !user.getEmail().isEmpty() ? user.getEmail() : "Chưa cập nhật");
        textPhone.setText(user.getPhone() != null && !user.getPhone().isEmpty() ? user.getPhone() : "Chưa cập nhật");
        textBio.setText(user.getBio() != null && !user.getBio().isEmpty() ? user.getBio() : "Chưa cập nhật");
        textAddress.setText(user.getAddress() != null && !user.getAddress().isEmpty() ? user.getAddress() : "Chưa cập nhật");
    }
}