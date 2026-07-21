package com.example.foodmobochain.model;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class FoodItem {
    public String id;
    public String vendorId;
    public String vendorName;
    public String storeId;
    public String name;
    public String description;
    public String category;
    public String imageUrl;
    public String tags;
    public double price;
    public double regularPrice;
    public double rating;
    public double deliveryFee;
    public double discountPercent;
    public int preparationMinutes;
    public int stockCount;
    public int spicyLevel;
    public boolean vegetarian;
    public boolean available;
    public boolean featured;
    public long createdAt;

    public FoodItem() { }

    public double salePrice() {
        return price;
    }

    public double listPrice() {
        return regularPrice > price ? regularPrice : price;
    }

    public boolean inStock() {
        return available && stockCount != 0;
    }
}
