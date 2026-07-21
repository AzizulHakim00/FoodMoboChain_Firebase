package com.example.foodmobochain.model;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class PromoBanner {
    public String id;
    public String title;
    public String subtitle;
    public String imageUrl;
    public String actionType;
    public String actionValue;
    public String badge;
    public boolean active;
    public int priority;
    public long startsAt;
    public long endsAt;

    public PromoBanner() { }
}
