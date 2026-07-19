package com.example.foodmobochain.model;
import java.util.HashMap; import java.util.Map;
public class FoodOrder { public String id,buyerId,buyerName,address,status; public double total; public long createdAt; public Map<String,CartLine> items=new HashMap<>(); public FoodOrder(){} }
