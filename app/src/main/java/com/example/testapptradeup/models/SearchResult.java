package com.example.testapptradeup.models;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.firebase.firestore.IgnoreExtraProperties;

@IgnoreExtraProperties
public class SearchResult implements Parcelable {
    private String id;
    private String title; // Đã bỏ final
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

    public SearchResult(Listing listing, boolean isFavorite) {
        // Hàm tạo rỗng cần thiết
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
        this.isFavorite = false;
    }

    // Constructor tiện ích để chuyển đổi từ Listing
    public SearchResult(Listing listing) {
        this.id = listing.getId();
        this.title = listing.getTitle();
        this.price = listing.getFormattedPrice();
        this.condition = listing.getConditionText();
        this.location = listing.getLocation();
        this.imageUrl = listing.getPrimaryImageUrl();
        this.category = listing.getCategoryId();
        // this.latitude = listing.getLatitude(); // Bỏ comment nếu Listing có lat/long
        // this.longitude = listing.getLongitude(); // Bỏ comment nếu Listing có lat/long
        this.isFavorite = false;
        this.distance = "";

        if (listing.getTimePosted() != null) {
            this.postedTime = android.text.format.DateUtils.getRelativeTimeSpanString(
                    listing.getTimePosted().getTime()).toString();
        } else {
            this.postedTime = "";
        }
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
    public boolean isFavorite() { return isFavorite; }
    public String getDistance() { return distance; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setPrice(String price) { this.price = price; }
    public void setCondition(String condition) { this.condition = condition; }
    public void setLocation(String location) { this.location = location; }
    public void setPostedTime(String postedTime) { this.postedTime = postedTime; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public void setCategory(String category) { this.category = category; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }
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
        isFavorite = in.readByte() != 0;
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
        dest.writeByte((byte) (isFavorite ? 1 : 0));
    }
}