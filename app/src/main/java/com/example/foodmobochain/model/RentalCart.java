package com.example.foodmobochain.model;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class RentalCart {
    public String id;
    public String name;
    public String location;
    public String description;
    public String imageUrl;
    public double dailyRate;
    public double deposit;
    public boolean available;

    public RentalCart() { }
}
