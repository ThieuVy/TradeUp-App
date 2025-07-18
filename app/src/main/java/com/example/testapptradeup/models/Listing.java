package com.example.testapptradeup.models;

import android.annotation.SuppressLint;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;
import java.text.NumberFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class Listing implements Parcelable {

    @DocumentId
    private String id; // Firestore sẽ tự động điền ID của document vào đây

    private String title;
    private String description;
    private double price;
    private List<String> imageUrls;
    private String location;

    @ServerTimestamp
    private Date timePosted; // Firestore sẽ tự động điền timestamp của server

    private String category;
    private float rating;
    private int reviewCount;
    private String sellerId;
    private String sellerName;
    private String condition; // "new", "like_new", "good", "fair", "used"
    private boolean isNegotiable;
    private String status; // "available", "sold", "paused"
    private int views;
    private int offersCount;
    private boolean isSold = false; // Thêm trường này để dễ dàng query
    private List<String> tags;
    private double latitude;
    private double longitude;
    private String geohash;
    private boolean featured = false;

    public Listing() {
        // Constructor rỗng cho Firebase Firestore
    }

    // Constructor đầy đủ (có thể dùng khi tạo mới)
    public Listing(String title, String description, double price, List<String> imageUrls, String location, String categoryId, String sellerId, String sellerName, String condition, boolean isNegotiable) {
        this.title = title;
        this.description = description;
        this.price = price;
        this.imageUrls = imageUrls;
        this.location = location;
        this.category = categoryId;
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.condition = condition;
        this.isNegotiable = isNegotiable;
        this.status = "available";
        this.views = 0;
        this.offersCount = 0;
        this.rating = 0.0f;
        this.reviewCount = 0;
        this.isSold = false;
        this.tags = null;
        this.latitude = 0.0;
        this.longitude = 0.0;
        this.geohash = null;
        this.featured = false;
    }


    // Getters
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public double getPrice() { return price; }
    public List<String> getImageUrls() { return imageUrls; }
    public String getLocation() { return location; }
    public Date getTimePosted() { return timePosted; }
    public String getCategoryId() { return category; }
    public float getRating() { return rating; }
    public int getReviewCount() { return reviewCount; }
    public String getSellerId() { return sellerId; }
    public String getSellerName() { return sellerName; }
    public String getCondition() { return condition; }
    public boolean isNegotiable() { return isNegotiable; }
    public String getStatus() { return status; }
    public int getViews() { return views; }
    public int getOffersCount() { return offersCount; }
    public boolean isSold() { return isSold; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getGeohash() { return geohash; }
    public boolean isFeatured() { return featured; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setPrice(double price) { this.price = price; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }
    public void setLocation(String location) { this.location = location; }
    public void setTimePosted(Date timePosted) { this.timePosted = timePosted; }
    public void setCategoryId(String categoryId) { this.category = categoryId; }
    public void setRating(float rating) { this.rating = rating; }
    public void setReviewCount(int reviewCount) { this.reviewCount = reviewCount; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }
    public void setCondition(String condition) { this.condition = condition; }
    public void setNegotiable(boolean negotiable) { isNegotiable = negotiable; }
    public void setStatus(String status) { this.status = status; }
    public void setViews(int views) { this.views = views; }
    public void setOffersCount(int offersCount) { this.offersCount = offersCount; }
    public void setSold(boolean sold) { isSold = sold; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public void setGeohash(String geohash) { this.geohash = geohash; }
    public void setFeatured(boolean featured) { this.featured = featured; }

    public List<String> getTags() {
        return tags;
    }
    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    /**
     * Lấy URL ảnh đại diện (ảnh đầu tiên trong danh sách)
     */
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
        if (price >= 1_000_000_000) {
            return String.format("%.1f tỷ", price / 1_000_000_000);
        } else if (price >= 1_000_000) {
            return String.format("%.0f triệu", price / 1_000_000);
        } else if (price >= 1_000) {
            return String.format("%.0f nghìn", price / 1_000);
        } else {
            return String.format("%.0f đ", price);
        }
    }

    @SuppressLint("DefaultLocale")
    public String getRatingText() {
        if (reviewCount == 0) {
            return "Chưa có đánh giá";
        }
        return String.format("%.1f ⭐ (%d)", rating, reviewCount);
    }

    public String getConditionText() {
        switch (condition) {
            case "new": return "Mới";
            case "like_new": return "Như mới";
            case "good": return "Tốt";
            case "fair": case "used": return "Đã dùng";
            default: return "Không xác định";
        }
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Listing that = (Listing) obj;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    protected Listing(Parcel in) {
        id = in.readString();
        title = in.readString();
        description = in.readString();
        price = in.readDouble();
        imageUrls = in.createStringArrayList();
        location = in.readString();
        long tmpTimePosted = in.readLong();
        timePosted = tmpTimePosted == -1 ? null : new java.util.Date(tmpTimePosted);
        category = in.readString();
        rating = in.readFloat();
        reviewCount = in.readInt();
        sellerId = in.readString();
        sellerName = in.readString();
        condition = in.readString();
        isNegotiable = in.readByte() != 0;
        status = in.readString();
        views = in.readInt();
        offersCount = in.readInt();
        isSold = in.readByte() != 0;
        tags = in.createStringArrayList();
        latitude = in.readDouble();
        longitude = in.readDouble();
        geohash = in.readString();
        featured = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id != null ? id : "");
        dest.writeString(title != null ? title : "");
        dest.writeString(description != null ? description : "");
        dest.writeDouble(price);
        dest.writeStringList(imageUrls);
        dest.writeString(location != null ? location : "");
        dest.writeLong(timePosted != null ? timePosted.getTime() : -1);
        dest.writeString(category != null ? category : "");
        dest.writeFloat(rating);
        dest.writeInt(reviewCount);
        dest.writeString(sellerId != null ? sellerId : "");
        dest.writeString(sellerName != null ? sellerName : "");
        dest.writeString(condition != null ? condition : "");
        dest.writeByte((byte) (isNegotiable ? 1 : 0));
        dest.writeString(status != null ? status : "");
        dest.writeInt(views);
        dest.writeInt(offersCount);
        dest.writeByte((byte) (isSold ? 1 : 0));
        dest.writeStringList(tags);
        dest.writeDouble(latitude);
        dest.writeDouble(longitude);
        dest.writeString(geohash != null ? geohash : "");
        dest.writeByte((byte) (featured ? 1 : 0));
    }

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