package com.example.testapptradeup.models;

import androidx.annotation.Nullable;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.List;

public class PagedResult<T> {

    private final List<T> data;
    private final DocumentSnapshot lastVisible;
    private final Exception error;

    public PagedResult(List<T> data, @Nullable DocumentSnapshot lastVisible, @Nullable Exception error) {
        this.data = data;
        this.lastVisible = lastVisible;
        this.error = error;
    }

    public List<T> getData() {
        return data;
    }

    @Nullable
    public DocumentSnapshot getLastVisible() {
        return lastVisible;
    }

    @Nullable
    public Exception getError() {
        return error;
    }

    public boolean isSuccess() {
        return error == null;
    }
}