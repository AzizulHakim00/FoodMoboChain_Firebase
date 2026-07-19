package com.example.foodmobochain.model;

public class AppUser {
    public String uid;
    public String name;
    public String email;
    public String phone;
    public String role;
    public String status;
    public String businessName;
    public String location;
    public String documentUrl;
    public double rating;
    public long createdAt;
    public long updatedAt;

    public AppUser() { }

    public AppUser(String uid, String name, String email, String role, String status) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.role = role;
        this.status = status;
        this.createdAt = System.currentTimeMillis();
    }
}
