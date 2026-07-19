package com.example.foodmobochain.model;

import java.util.HashMap;
import java.util.Map;

public class FoodOrder {
    public String id;
    public String buyerId;
    public String vendorId;
    public String buyerName;
    public String address;
    public String status;
    public double total;
    public long createdAt;
    public long updatedAt;
    public Map<String, CartLine> items = new HashMap<>();

    public FoodOrder() { }
}
