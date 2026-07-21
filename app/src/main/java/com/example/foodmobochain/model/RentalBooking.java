package com.example.foodmobochain.model;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class RentalBooking {
    public String id;
    public String cartId;
    public String cartName;
    public String userId;
    public String requestedLocation;
    public String status;
    public long startAt;
    public long endAt;
    public long createdAt;
    public long updatedAt;
    public int days;
    public boolean delivery;
    public double dailyRate;
    public double total;

    public RentalBooking() { }
}