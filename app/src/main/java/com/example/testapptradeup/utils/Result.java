package com.example.testapptradeup.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Một lớp generic để đóng gói kết quả của một tác vụ,
 * có thể là thành công với dữ liệu hoặc thất bại với lỗi.
 * @param <T> Kiểu dữ liệu của kết quả thành công.
 */
public class Result<T> {

    @Nullable
    private final T data;
    @Nullable
    private final Exception error;

    private Result(@Nullable T data, @Nullable Exception error) {
        this.data = data;
        this.error = error;
    }

    public boolean isSuccess() {
        return error == null;
    }

    @Nullable
    public T getData() {
        return data;
    }

    @Nullable
    public Exception getError() {
        return error;
    }

    // Factory methods
    public static <T> Result<T> success(@NonNull T data) {
        return new Result<>(data, null);
    }

    public static <T> Result<T> error(@NonNull Exception error) {
        return new Result<>(null, error);
    }
}