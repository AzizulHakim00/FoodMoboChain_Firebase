package com.example.foodmobochain.model;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.util.HashMap;
import java.util.Map;

@IgnoreExtraProperties
public class FoodOrder {
    public String id;
    public String buyerId;
    public String vendorId;
    public String buyerName;
    public String address;
    public String status;
    public long createdAt;
    public long updatedAt;
    public Map<String, CartLine> items = new HashMap<>();

    public FoodOrder() { }

    @Exclude
    public double computedTotal() {
        double value = 0;
        if (items == null) return value;
        for (CartLine line : items.values()) {
            if (line != null) value += line.unitPrice * line.quantity;
        }
        return value;
    }
}