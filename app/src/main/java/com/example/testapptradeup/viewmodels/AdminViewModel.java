package com.example.testapptradeup.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.testapptradeup.models.Report;
import com.example.testapptradeup.models.Review;
import com.example.testapptradeup.repositories.AdminRepository;
import com.google.firebase.functions.FirebaseFunctions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class AdminViewModel extends ViewModel {
    private final AdminRepository adminRepository = new AdminRepository();

    private final FirebaseFunctions functions = FirebaseFunctions.getInstance();

    // LiveData cho trạng thái của hành động (thành công/thất bại)
    private final MutableLiveData<String> _actionStatusMessage = new MutableLiveData<>();
    public LiveData<String> getActionStatusMessage() { return _actionStatusMessage; }

    public LiveData<Integer> getPendingReviewCount() {
        return adminRepository.getPendingReviewCount();
    }

    public LiveData<Integer> getPendingReportCount() {
        return adminRepository.getPendingReportCount();
    }
    public LiveData<List<Review>> getPendingReviews() {
        return adminRepository.getPendingReviews();
    }

    public void moderateReview(String reviewId, String newStatus) {
        Map<String, Object> data = new HashMap<>();
        data.put("reviewId", reviewId);
        data.put("newStatus", newStatus);

        functions.getHttpsCallable("moderateReview")
                .call(data)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        _actionStatusMessage.setValue("Hành động thành công!");
                    } else {
                        _actionStatusMessage.setValue("Lỗi: " + task.getException().getMessage());
                    }
                });
    }

    public void clearActionStatus() {
        _actionStatusMessage.setValue(null);
    }
    public LiveData<List<Report>> getPendingReports() {
        return adminRepository.getPendingReports();
    }

    public void resolveReport(String reportId, String reportedUserId, boolean shouldSuspend) {
        Map<String, Object> data = new HashMap<>();
        data.put("reportId", reportId);
        data.put("reportedUserId", reportedUserId);
        data.put("shouldSuspend", shouldSuspend);

        functions.getHttpsCallable("resolveReport")
                .call(data)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        _actionStatusMessage.setValue("Báo cáo đã được xử lý!");
                    } else {
                        _actionStatusMessage.setValue("Lỗi: " + Objects.requireNonNull(task.getException()).getMessage());
                    }
                });
    }
}