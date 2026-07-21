package com.example.foodmobochain.model;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class SupportTicket {
    public String id;
    public String userId;
    public String userName;
    public String category;
    public String subject;
    public String message;
    public String status;
    public String adminReply;
    public long createdAt;
    public long updatedAt;

    public SupportTicket() { }
}
