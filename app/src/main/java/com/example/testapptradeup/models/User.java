package com.example.testapptradeup.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;

import java.util.Date;
import java.util.List;

@IgnoreExtraProperties
public class User implements Parcelable {
    @Exclude
    private String id;
    private String name;
    private String email;
    private String phone;
    private String bio;
    private String profileImageUrl;
    private String address;
    private float rating;
    private int reviewCount;
    private boolean isVerified;
    private String accountStatus; // e.g., "active", "suspended"
    private boolean isFlagged;
    private String walletStatus; // e.g., "connected", "not_connected"
    private int notificationCount;
    private String bankAccount;
    private List<String> favoriteListingIds;
    private String location;
    private Date memberSince;
    private int activeListingsCount;
    private int completedSalesCount;
    private List<Review> reviews;
    private String fcmToken;
    @Exclude
    private boolean isAdmin = false;

    public User() {
        // Constructor rỗng cần thiết cho Firebase
    }

    // Constructor và các Getters/Setters khác giữ nguyên...
    // [Các getters và setters của bạn ở đây]
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }
    public int getReviewCount() { return reviewCount; }
    public void setReviewCount(int reviewCount) { this.reviewCount = reviewCount; }
    public boolean isVerified() { return isVerified; }
    public void setVerified(boolean verified) { isVerified = verified; }
    public String getAccountStatus() { return accountStatus; }
    public void setAccountStatus(String accountStatus) { this.accountStatus = accountStatus; }
    public boolean isFlagged() { return isFlagged; }
    public void setFlagged(boolean flagged) { isFlagged = flagged; }
    public String getWalletStatus() { return walletStatus; }
    public void setWalletStatus(String walletStatus) { this.walletStatus = walletStatus; }
    public int getNotificationCount() { return notificationCount; }
    public void setNotificationCount(int notificationCount) { this.notificationCount = notificationCount; }
    public String getBankAccount() { return bankAccount; }
    public void setBankAccount(String bankAccount) { this.bankAccount = bankAccount; }
    public List<String> getFavoriteListingIds() { return favoriteListingIds; }
    public void setFavoriteListingIds(List<String> favoriteListingIds) { this.favoriteListingIds = favoriteListingIds; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public Date getMemberSince() { return memberSince; }
    public void setMemberSince(Date memberSince) { this.memberSince = memberSince; }
    public int getActiveListingsCount() { return activeListingsCount; }
    public void setActiveListingsCount(int activeListingsCount) { this.activeListingsCount = activeListingsCount; }
    public int getCompletedSalesCount() { return completedSalesCount; }
    public void setCompletedSalesCount(int completedSalesCount) { this.completedSalesCount = completedSalesCount; }
    public List<Review> getReviews() { return reviews; }
    public void setReviews(List<Review> reviews) { this.reviews = reviews; }
    public String getFcmToken() { return fcmToken; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }
    @Exclude
    public boolean isAdmin() { return isAdmin; }
    @Exclude
    public void setAdmin(boolean admin) { isAdmin = admin; }

    protected User(Parcel in) {
        id = in.readString();
        name = in.readString();
        email = in.readString();
        phone = in.readString();
        bio = in.readString();
        profileImageUrl = in.readString();
        address = in.readString();
        rating = in.readFloat();
        reviewCount = in.readInt();
        isVerified = in.readByte() != 0;
        accountStatus = in.readString();
        isFlagged = in.readByte() != 0;
        walletStatus = in.readString();
        notificationCount = in.readInt();
        bankAccount = in.readString();
        favoriteListingIds = in.createStringArrayList();
        location = in.readString();
        long tmpMemberSince = in.readLong();
        memberSince = tmpMemberSince == -1 ? null : new Date(tmpMemberSince);
        activeListingsCount = in.readInt();
        completedSalesCount = in.readInt();
        reviews = in.createTypedArrayList(Review.CREATOR);
        fcmToken = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id != null ? id : "");
        dest.writeString(name != null ? name : "");
        dest.writeString(email != null ? email : "");
        dest.writeString(phone != null ? phone : "");
        dest.writeString(bio != null ? bio : "");
        dest.writeString(profileImageUrl != null ? profileImageUrl : "");
        dest.writeString(address != null ? address : "");
        dest.writeFloat(rating);
        dest.writeInt(reviewCount);
        dest.writeByte((byte) (isVerified ? 1 : 0));
        dest.writeString(accountStatus != null ? accountStatus : "");
        dest.writeByte((byte) (isFlagged ? 1 : 0));
        dest.writeString(walletStatus != null ? walletStatus : "");
        dest.writeInt(notificationCount);
        dest.writeString(bankAccount != null ? bankAccount : "");
        dest.writeStringList(favoriteListingIds);
        dest.writeString(location != null ? location : "");
        dest.writeLong(memberSince != null ? memberSince.getTime() : -1);
        dest.writeInt(activeListingsCount);
        dest.writeInt(completedSalesCount);
        dest.writeTypedList(reviews);
        dest.writeString(fcmToken != null ? fcmToken : "");
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<User> CREATOR = new Creator<User>() {
        @Override
        public User createFromParcel(Parcel in) {
            return new User(in);
        }

        @Override
        public User[] newArray(int size) {
            return new User[size];
        }
    };
}