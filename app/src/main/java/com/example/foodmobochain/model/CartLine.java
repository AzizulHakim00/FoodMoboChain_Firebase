package com.example.foodmobochain.model;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class CartLine {
    public String foodId;
    public String vendorId;
    public String vendorName;
    public String storeId;
    public String name;
    public String imageUrl;
    public double unitPrice;
    public int quantity;

    public CartLine() { }

    public CartLine(FoodItem item, int quantity) {
        this.foodId = item.id;
        this.vendorId = item.vendorId;
        this.vendorName = item.vendorName;
        this.storeId = item.storeId;
        this.name = item.name;
        this.imageUrl = item.imageUrl;
        this.unitPrice = item.price;
        this.quantity = quantity;
    }
}
