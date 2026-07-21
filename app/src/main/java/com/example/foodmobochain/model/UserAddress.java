package com.example.foodmobochain.model;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class UserAddress {
    public String id;
    public String userId;
    public String label;
    public String recipientName;
    public String contactNumber;
    public String line1;
    public String area;
    public String city;
    public String deliveryNote;
    public boolean defaultAddress;
    public long createdAt;
    public long updatedAt;

    public UserAddress() { }

    public String displayAddress() {
        String result = line1 == null ? "" : line1.trim();
        if (area != null && !area.trim().isEmpty()) result += (result.isEmpty() ? "" : ", ") + area.trim();
        if (city != null && !city.trim().isEmpty()) result += (result.isEmpty() ? "" : ", ") + city.trim();
        return result;
    }
}
