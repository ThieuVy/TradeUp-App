package com.example.testapptradeup.repositories;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.testapptradeup.models.Report;
import com.example.testapptradeup.models.Review;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class AdminRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public LiveData<Integer> getPendingReviewCount() {
        MutableLiveData<Integer> count = new MutableLiveData<>();
        db.collection("reviews").whereEqualTo("moderationStatus", "pending")
                .get().addOnSuccessListener(snapshot -> count.setValue(snapshot.size()));
        return count;
    }

    public LiveData<Integer> getPendingReportCount() {
        MutableLiveData<Integer> count = new MutableLiveData<>();
        db.collection("reports").whereEqualTo("status", "pending")
                .get().addOnSuccessListener(snapshot -> count.setValue(snapshot.size()));
        return count;
    }

    public LiveData<List<Review>> getPendingReviews() {
        MutableLiveData<List<Review>> reviewsData = new MutableLiveData<>();
        db.collection("reviews")
                .whereEqualTo("moderationStatus", "pending")
                .orderBy("reviewDate", Query.Direction.ASCENDING) // Ưu tiên review cũ hơn
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots != null) {
                        reviewsData.setValue(queryDocumentSnapshots.toObjects(Review.class));
                    }
                })
                .addOnFailureListener(e -> {
                    // Xử lý lỗi, có thể log hoặc trả về list rỗng
                    reviewsData.setValue(new ArrayList<>());
                });
        return reviewsData;
    }
    public LiveData<List<Report>> getPendingReports() {
        MutableLiveData<List<Report>> reportsData = new MutableLiveData<>();
        db.collection("reports")
                .whereEqualTo("status", "pending")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots != null) {
                        reportsData.setValue(queryDocumentSnapshots.toObjects(Report.class));
                    }
                })
                .addOnFailureListener(e -> reportsData.setValue(new ArrayList<>()));
        return reportsData;
    }
}