package com.example.testapptradeup.models;

import android.annotation.SuppressLint;
import android.os.Parcel;
import android.os.Parcelable;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.PropertyName;
import com.google.firebase.firestore.ServerTimestamp;
import java.text.NumberFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class Listing implements Parcelable {

    @DocumentId
    private String id;
    private String title;
    private String description;
    private double price;
    private List<String> imageUrls;
    private String location;
    @ServerTimestamp
    private Date timePosted;
    private String category;
    private float rating;
    private int reviewCount;
    private String sellerId;
    private String sellerName;
    private String condition;
    private String status;
    private int views;
    private int offersCount;
    private List<String> tags;
    private double latitude;
    private double longitude;
    private String geohash;
    private boolean featured;

    // Đổi tên biến private để tuân thủ quy ước Java Beans
    private boolean negotiable;
    private boolean sold;

    public Listing() {
        // Constructor rỗng cho Firebase
    }

    // Getters
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public double getPrice() { return price; }
    public List<String> getImageUrls() { return imageUrls; }
    public String getLocation() { return location; }
    public Date getTimePosted() { return timePosted; }
    public String getCategory() { return category; }
    public float getRating() { return rating; }
    public int getReviewCount() { return reviewCount; }
    public String getSellerId() { return sellerId; }
    public String getSellerName() { return sellerName; }
    public String getCondition() { return condition; }
    public String getStatus() { return status; }
    public int getViews() { return views; }
    public int getOffersCount() { return offersCount; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getGeohash() { return geohash; }
    public boolean isFeatured() { return featured; }
    public List<String> getTags() { return tags; }

    @PropertyName("isNegotiable")
    public boolean isNegotiable() { return negotiable; }

    @PropertyName("isSold")
    public boolean isSold() { return sold; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setPrice(double price) { this.price = price; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }
    public void setLocation(String location) { this.location = location; }
    public void setTimePosted(Date timePosted) { this.timePosted = timePosted; }
    public void setCategory(String category) { this.category = category; }
    public void setRating(float rating) { this.rating = rating; }
    public void setReviewCount(int reviewCount) { this.reviewCount = reviewCount; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }
    public void setCondition(String condition) { this.condition = condition; }
    public void setStatus(String status) { this.status = status; }
    public void setViews(int views) { this.views = views; }
    public void setOffersCount(int offersCount) { this.offersCount = offersCount; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public void setGeohash(String geohash) { this.geohash = geohash; }
    public void setFeatured(boolean featured) { this.featured = featured; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public void setNegotiable(boolean negotiable) { this.negotiable = negotiable; }
    public void setSold(boolean sold) { this.sold = sold; }

    // Các hàm helper
    public String getPrimaryImageUrl() {
        if (imageUrls != null && !imageUrls.isEmpty()) {
            return imageUrls.get(0);
        }
        return null;
    }

    public String getFormattedPrice() {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        return formatter.format(price);
    }

    @SuppressLint("DefaultLocale")
    public String getShortPrice() {
        if (price >= 1_000_000_000) return String.format("%.1f tỷ", price / 1_000_000_000);
        if (price >= 1_000_000) return String.format("%.0f triệu", price / 1_000_000);
        if (price >= 1_000) return String.format("%.0f nghìn", price / 1_000);
        return String.format("%.0f đ", price);
    }

    @SuppressLint("DefaultLocale")
    public String getRatingText() {
        if (reviewCount == 0) return "Chưa có đánh giá";
        return String.format("%.1f ⭐ (%d)", rating, reviewCount);
    }

    public String getConditionText() {
        if (condition == null) return "Không xác định";
        switch (condition) {
            case "new": return "Mới";
            case "like_new": return "Như mới";
            case "good": return "Tốt";
            case "fair": case "used": return "Đã dùng";
            default: return "Không xác định";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Listing listing = (Listing) o;
        return Objects.equals(id, listing.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    // Parcelable Implementation
    protected Listing(Parcel in) {
        id = in.readString();
        title = in.readString();
        description = in.readString();
        price = in.readDouble();
        imageUrls = in.createStringArrayList();
        location = in.readString();
        long tmpTimePosted = in.readLong();
        timePosted = tmpTimePosted == -1 ? null : new Date(tmpTimePosted);
        category = in.readString();
        rating = in.readFloat();
        reviewCount = in.readInt();
        sellerId = in.readString();
        sellerName = in.readString();
        condition = in.readString();
        status = in.readString();
        views = in.readInt();
        offersCount = in.readInt();
        tags = in.createStringArrayList();
        latitude = in.readDouble();
        longitude = in.readDouble();
        geohash = in.readString();
        featured = in.readByte() != 0;
        negotiable = in.readByte() != 0;
        sold = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(title);
        dest.writeString(description);
        dest.writeDouble(price);
        dest.writeStringList(imageUrls);
        dest.writeString(location);
        dest.writeLong(timePosted != null ? timePosted.getTime() : -1);
        dest.writeString(category);
        dest.writeFloat(rating);
        dest.writeInt(reviewCount);
        dest.writeString(sellerId);
        dest.writeString(sellerName);
        dest.writeString(condition);
        dest.writeString(status);
        dest.writeInt(views);
        dest.writeInt(offersCount);
        dest.writeStringList(tags);
        dest.writeDouble(latitude);
        dest.writeDouble(longitude);
        dest.writeString(geohash);
        dest.writeByte((byte) (featured ? 1 : 0));
        dest.writeByte((byte) (negotiable ? 1 : 0));
        dest.writeByte((byte) (sold ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Listing> CREATOR = new Creator<>() {
        @Override
        public Listing createFromParcel(Parcel in) {
            return new Listing(in);
        }

        @Override
        public Listing[] newArray(int size) {
            return new Listing[size];
        }
    };
}