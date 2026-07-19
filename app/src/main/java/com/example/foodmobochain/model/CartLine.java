package com.example.foodmobochain.model;
public class CartLine { public String foodId,vendorId,vendorName,name; public double unitPrice; public int quantity; public CartLine(){} public CartLine(FoodItem item,int quantity){this.foodId=item.id;this.vendorId=item.vendorId;this.vendorName=item.vendorName;this.name=item.name;this.unitPrice=item.price;this.quantity=quantity;}}
