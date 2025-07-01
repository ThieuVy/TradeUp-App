package com.example.testapptradeup.models;

import android.os.Parcel;
import android.os.Parcelable;

// Đảm bảo các import khác nếu có (ví dụ: cho Firebase Firestore)
import com.google.firebase.firestore.IgnoreExtraProperties; // Quan trọng nếu bạn đang dùng Firestore

@IgnoreExtraProperties // Firestore sẽ bỏ qua các thuộc tính không có trong document khi deserialize
public class SearchResult implements Parcelable {
    // Các thuộc tính hiện có của bạn (ví dụ từ Firebase)
    private String id;
    private final String title;
    private String price;
    private String condition;
    private String location;
    private String postedTime;
    private String imageUrl; // Thêm trường này nếu chưa có
    private double latitude; // Thêm trường này nếu chưa có
    private double longitude; // Thêm trường này nếu chưa có
    private String distance; // Có thể là String để hiển thị, hoặc tính toán từ lat/long
    private String category; // Category của sản phẩm

    // THUỘC TÍNH MỚI CHO TRẠNG THÁI YÊU THÍCH
    private boolean isFavorite;

    public SearchResult() {
        // Hàm tạo rỗng cần thiết cho Firebase Firestore
        // Khởi tạo giá trị mặc định để tránh null
        this.title = "";
        this.price = "";
        this.condition = "";
        this.location = "";
        this.postedTime = "";
        this.imageUrl = "";
        this.distance = "";
        this.category = "";
        this.latitude = 0.0;
        this.longitude = 0.0;
        this.isFavorite = false; // Mặc định không phải là yêu thích
    }

    // Constructor với các trường cần thiết để khởi tạo
    // (Bạn có thể điều chỉnh constructor này theo nhu cầu thực tế của bạn)
    public SearchResult(String id, String title, String price, String condition, String location,
                        String postedTime, String imageUrl, double latitude, double longitude, String category, boolean isFavorite) {
        this.id = id;
        this.title = title;
        this.price = price;
        this.condition = condition;
        this.location = location;
        this.postedTime = postedTime;
        this.imageUrl = imageUrl;
        this.latitude = latitude;
        this.longitude = longitude;
        this.category = category;
        this.isFavorite = isFavorite;
        // distance sẽ được tính toán hoặc set sau
        this.distance = "";
    }


    // Getters
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getPrice() { return price; }
    public String getCondition() { return condition; }
    public String getLocation() { return location; }
    public String getPostedTime() { return postedTime; }
    public String getImageUrl() { return imageUrl; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getCategory() { return category; }

    // Getter và Setter cho isFavorite
    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }

    // Setter (nếu bạn cần thay đổi ID sau khi tạo)
    public void setId(String id) { this.id = id; }
    public void setPrice(String price) { this.price = price; }
    public void setCondition(String condition) { this.condition = condition; }
    public void setLocation(String location) { this.location = location; }
    public void setPostedTime(String postedTime) { this.postedTime = postedTime; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public void setCategory(String category) { this.category = category; }

    // Setter cho distance (có thể được tính toán hoặc set từ bên ngoài)
    public String getDistance() { return distance; }
    public void setDistance(String distance) { this.distance = distance; }


    // --- Parcelable Implementation ---
    protected SearchResult(Parcel in) {
        id = in.readString();
        title = in.readString();
        price = in.readString();
        condition = in.readString();
        location = in.readString();
        postedTime = in.readString();
        imageUrl = in.readString();
        latitude = in.readDouble();
        longitude = in.readDouble();
        distance = in.readString();
        category = in.readString();
        isFavorite = in.readByte() != 0; // Đọc boolean
    }

    public static final Creator<SearchResult> CREATOR = new Creator<SearchResult>() {
        @Override
        public SearchResult createFromParcel(Parcel in) {
            return new SearchResult(in);
        }

        @Override
        public SearchResult[] newArray(int size) {
            return new SearchResult[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(title);
        dest.writeString(price);
        dest.writeString(condition);
        dest.writeString(location);
        dest.writeString(postedTime);
        dest.writeString(imageUrl);
        dest.writeDouble(latitude);
        dest.writeDouble(longitude);
        dest.writeString(distance);
        dest.writeString(category);
        dest.writeByte((byte) (isFavorite ? 1 : 0)); // Ghi boolean
    }
}
