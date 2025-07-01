package com.example.testapptradeup.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;

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
    private String bankAccount; // New: To store bank account info (e.g., account number, bank name)

    public User() {
        // Public no-argument constructor needed for Firebase Firestore
        // Initialize with default values to prevent NullPointerExceptions
        this.id = "";
        this.name = "";
        this.email = "";
        this.phone = "";
        this.bio = "";
        this.profileImageUrl = "";
        this.address = "";
        this.rating = 0.0f;
        this.reviewCount = 0;
        this.isVerified = false;
        this.accountStatus = "active";
        this.isFlagged = false;
        this.walletStatus = "not_connected";
        this.notificationCount = 0;
        this.bankAccount = ""; // Default empty string
    }

    // Comprehensive constructor
    public User(String id, String name, String email, String phone, String bio,
                String profileImageUrl, String address, float rating, int reviewCount,
                boolean isVerified, String accountStatus, boolean isFlagged,
                String walletStatus, int notificationCount) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.bio = bio;
        this.profileImageUrl = profileImageUrl;
        this.address = address;
        this.rating = rating;
        this.reviewCount = reviewCount;
        this.isVerified = isVerified;
        this.accountStatus = accountStatus;
        this.isFlagged = isFlagged;
        this.walletStatus = walletStatus;
        this.notificationCount = notificationCount;
        this.bankAccount = ""; // Initialize default
    }

    // Constructor with basic info (for new user creation)
    public User(String id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = "";
        this.bio = "";
        this.profileImageUrl = "";
        this.address = "";
        this.rating = 0.0f;
        this.reviewCount = 0;
        this.isVerified = false;
        this.accountStatus = "active";
        this.isFlagged = false;
        this.walletStatus = "not_connected";
        this.notificationCount = 0;
        this.bankAccount = ""; // Initialize default
    }

    // Getters
    @Exclude
    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getBio() { return bio; }
    public String getProfileImageUrl() { return profileImageUrl; }
    public String getAddress() { return address; }
    public float getRating() { return rating; }
    public int getReviewCount() { return reviewCount; }
    public boolean isVerified() { return isVerified; }
    public String getAccountStatus() { return accountStatus; }
    public boolean isFlagged() { return isFlagged; }
    public String getWalletStatus() { return walletStatus; }
    public int getNotificationCount() { return notificationCount; }
    public String getBankAccount() { return bankAccount; } // New Getter

    // Setters
    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setBio(String bio) { this.bio = bio; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }
    public void setAddress(String address) { this.address = address; }
    public void setRating(float rating) { this.rating = rating; }
    public void setReviewCount(int reviewCount) { this.reviewCount = reviewCount; }
    public void setVerified(boolean verified) { isVerified = verified; }
    public void setAccountStatus(String accountStatus) { this.accountStatus = accountStatus; }
    public void setFlagged(boolean flagged) { isFlagged = flagged; }
    public void setWalletStatus(String walletStatus) { this.walletStatus = walletStatus; }
    public void setNotificationCount(int notificationCount) { this.notificationCount = notificationCount; }
    public void setBankAccount(String bankAccount) { this.bankAccount = bankAccount; } // New Setter


    // Parcelable implementation
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
        bankAccount = in.readString(); // Read new field
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(name);
        dest.writeString(email);
        dest.writeString(phone);
        dest.writeString(bio);
        dest.writeString(profileImageUrl);
        dest.writeString(address);
        dest.writeFloat(rating);
        dest.writeInt(reviewCount);
        dest.writeByte((byte) (isVerified ? 1 : 0));
        dest.writeString(accountStatus);
        dest.writeByte((byte) (isFlagged ? 1 : 0));
        dest.writeString(walletStatus);
        dest.writeInt(notificationCount);
        dest.writeString(bankAccount); // Write new field
    }
}