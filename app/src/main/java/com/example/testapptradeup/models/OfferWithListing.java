package com.example.testapptradeup.models;

// Lớp helper để kết hợp thông tin từ Offer và Listing
public class OfferWithListing {
    private final Offer offer;
    private final Listing listing;

    public OfferWithListing(Offer offer, Listing listing) {
        this.offer = offer;
        this.listing = listing;
    }

    public Offer getOffer() {
        return offer;
    }

    public Listing getListing() {
        return listing;
    }
}