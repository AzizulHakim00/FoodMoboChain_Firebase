package com.example.foodmobochain.model;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class MarketplaceStore {
    public String id;
    public String ownerId;
    public String name;
    public String cuisine;
    public String description;
    public String imageUrl;
    public String bannerUrl;
    public String location;
    public double rating;
    public double deliveryFee;
    public double minimumOrder;
    public int preparationMinutes;
    public boolean verified;
    public boolean open;
    public boolean featured;
    public long createdAt;

    public MarketplaceStore() { }
}
