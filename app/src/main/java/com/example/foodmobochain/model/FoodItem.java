package com.example.foodmobochain.model;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class FoodItem {
    public String id;
    public String vendorId;
    public String vendorName;
    public String name;
    public String description;
    public String category;
    public String imageUrl;
    public double price;
    public double rating;
    public double deliveryFee;
    public double discountPercent;
    public int preparationMinutes;
    public boolean available;
    public boolean featured;
    public long createdAt;

    public FoodItem() { }

    public double salePrice() {
        double discount = Math.max(0, Math.min(90, discountPercent));
        return price * (1d - discount / 100d);
    }
}
