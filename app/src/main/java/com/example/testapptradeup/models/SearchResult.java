package com.example.testapptradeup.models;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.firebase.firestore.IgnoreExtraProperties;

// Lớp này đại diện cho MỘT KẾT QUẢ TÌM KIẾM
@IgnoreExtraProperties
public class SearchResult implements Parcelable {

    private String id;
    private String title;
    private String price;
    private String condition;
    private String location;
    private String postedTime;
    private String imageUrl;
    private double latitude;
    private double longitude;
    private String distance;
    private String category;
    private boolean isFavorite;

    // Constructor rỗng cần thiết cho Firebase
    public SearchResult() {}

    // Constructor để chuyển đổi từ Listing (ví dụ)
    // Bạn cần hoàn thiện logic này nếu sử dụng
    public SearchResult(Listing listing, boolean isFavorite) {
        // Gán các giá trị từ listing vào đây
        // this.id = listing.getId();
        // this.title = listing.getTitle();
        // ...
        this.isFavorite = isFavorite;
    }

    // --- Getters and Setters ---
    // (Giữ nguyên tất cả các getter và setter của bạn cho các trường trên)
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }
    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getPostedTime() { return postedTime; }
    public void setPostedTime(String postedTime) { this.postedTime = postedTime; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public String getDistance() { return distance; }
    public void setDistance(String distance) { this.distance = distance; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public boolean isFavorite() { return isFavorite; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }


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
        isFavorite = in.readByte() != 0;
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
        dest.writeByte((byte) (isFavorite ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
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
}