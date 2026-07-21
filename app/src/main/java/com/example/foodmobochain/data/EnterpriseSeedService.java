package com.example.foodmobochain.data;

import com.example.foodmobochain.model.AppUser;
import com.example.foodmobochain.model.FoodItem;
import com.example.foodmobochain.model.NewsPost;
import com.example.foodmobochain.model.RentalCart;

import java.util.HashMap;
import java.util.Map;

public final class EnterpriseSeedService {
    public interface Callback {
        void onComplete(Exception error);
    }

    private static final String[] FOOD_IDS = {
            "sample-kacchi", "sample-chicken-biryani", "sample-beef-tehari", "sample-fuchka",
            "sample-chotpoti", "sample-momo", "sample-chowmein", "sample-burger", "sample-fries",
            "sample-grilled-chicken", "sample-wings", "sample-borhani", "sample-mango-lassi",
            "sample-falooda", "sample-brownie"
    };

    private EnterpriseSeedService() { }

    public static void seed(FirebaseService firebase, AppUser admin, Callback callback) {
        Map<String, Object> cleanup = new HashMap<>();
        for (String id : FOOD_IDS) cleanup.put("foods/" + id, null);
        firebase.root.updateChildren(cleanup).addOnCompleteListener(cleanTask -> {
            if (!cleanTask.isSuccessful()) {
                callback.onComplete(cleanTask.getException());
                return;
            }
            writeCatalogue(firebase, admin, callback);
        });
    }

    private static void writeCatalogue(FirebaseService firebase, AppUser admin, Callback callback) {
        Map<String, Object> update = new HashMap<>();
        long now = System.currentTimeMillis();

        addFood(update, admin, "sample-kacchi", "Kacchi Biryani", "Rice",
                "Aromatic basmati rice layered with tender mutton, potato, saffron and house spices.",
                360, 4.9, 32, 40, true,
                "https://images.unsplash.com/photo-1589302168068-964664d93dc0?auto=format&fit=crop&w=1200&q=80", now);
        addFood(update, admin, "sample-chicken-biryani", "Chicken Biryani", "Rice",
                "Fragrant rice, marinated chicken, caramelised onion and cooling raita.",
                250, 4.8, 28, 30, true,
                "https://images.unsplash.com/photo-1563379926898-05f4575a45d8?auto=format&fit=crop&w=1200&q=80", now);
        addFood(update, admin, "sample-beef-tehari", "Old Dhaka Beef Tehari", "Traditional",
                "Mustard-oil rice with tender beef, green chilli and classic Old Dhaka flavour.",
                280, 4.7, 30, 35, false,
                "https://images.unsplash.com/photo-1512058564366-18510be2db19?auto=format&fit=crop&w=1200&q=80", now);
        addFood(update, admin, "sample-fuchka", "Spicy Fuchka", "Street Food",
                "Crispy shells filled with spiced potato, chickpea and tangy tamarind water.",
                120, 4.9, 12, 20, true,
                "https://images.unsplash.com/photo-1601050690597-df0568f70950?auto=format&fit=crop&w=1200&q=80", now);
        addFood(update, admin, "sample-chotpoti", "Dhaka Chotpoti", "Street Food",
                "Warm chickpea curry with potato, egg, tamarind, coriander and crunchy toppings.",
                140, 4.7, 15, 20, false,
                "https://images.unsplash.com/photo-1601050690117-94f5f6fa8bd7?auto=format&fit=crop&w=1200&q=80", now);
        addFood(update, admin, "sample-momo", "Chicken Momo", "Snacks",
                "Steamed dumplings packed with seasoned chicken and served with fiery tomato chutney.",
                180, 4.8, 18, 25, true,
                "https://images.unsplash.com/photo-1563245372-f21724e3856d?auto=format&fit=crop&w=1200&q=80", now);
        addFood(update, admin, "sample-chowmein", "Chicken Chow Mein", "Noodles",
                "Wok-tossed noodles with chicken, vegetables and a balanced savoury sauce.",
                220, 4.6, 22, 30, false,
                "https://images.unsplash.com/photo-1559314809-0d155014e29e?auto=format&fit=crop&w=1200&q=80", now);
        addFood(update, admin, "sample-burger", "Double Smash Burger", "Fast Food",
                "Two seared beef patties, melted cheese, pickles and signature sauce in a toasted bun.",
                320, 4.8, 24, 35, true,
                "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?auto=format&fit=crop&w=1200&q=80", now);
        addFood(update, admin, "sample-fries", "Loaded Masala Fries", "Fast Food",
                "Crispy fries loaded with cheese sauce, masala chicken, herbs and chilli.",
                190, 4.5, 16, 25, false,
                "https://images.unsplash.com/photo-1573080496219-bb080dd4f877?auto=format&fit=crop&w=1200&q=80", now);
        addFood(update, admin, "sample-grilled-chicken", "Herb Grilled Chicken", "Grill",
                "Juicy herb-marinated chicken with roasted vegetables and house sauce.",
                340, 4.7, 30, 40, true,
                "https://images.unsplash.com/photo-1532550907401-a500c9a57435?auto=format&fit=crop&w=1200&q=80", now);
        addFood(update, admin, "sample-wings", "Naga Chicken Wings", "Grill",
                "Crispy chicken wings coated with smoky naga chilli glaze.",
                260, 4.6, 25, 35, false,
                "https://images.unsplash.com/photo-1527477396000-e27163b481c2?auto=format&fit=crop&w=1200&q=80", now);
        addFood(update, admin, "sample-borhani", "Classic Borhani", "Drinks",
                "Chilled spiced yoghurt drink with mint, mustard and roasted cumin.",
                90, 4.5, 5, 15, false,
                "https://images.unsplash.com/photo-1544145945-f90425340c7e?auto=format&fit=crop&w=1200&q=80", now);
        addFood(update, admin, "sample-mango-lassi", "Mango Lassi", "Drinks",
                "Creamy yoghurt blended with ripe mango and a touch of cardamom.",
                150, 4.7, 7, 15, true,
                "https://images.unsplash.com/photo-1572490122747-3968b75cc699?auto=format&fit=crop&w=1200&q=80", now);
        addFood(update, admin, "sample-falooda", "Royal Falooda", "Dessert",
                "Rose milk, vermicelli, basil seeds, jelly and ice cream in a chilled dessert glass.",
                210, 4.8, 12, 20, true,
                "https://images.unsplash.com/photo-1563805042-7684c019e1cb?auto=format&fit=crop&w=1200&q=80", now);
        addFood(update, admin, "sample-brownie", "Chocolate Brownie", "Dessert",
                "Warm fudgy brownie with dark chocolate and a soft centre.",
                170, 4.6, 10, 20, false,
                "https://images.unsplash.com/photo-1606313564200-e75d5e30476c?auto=format&fit=crop&w=1200&q=80", now);

        addCart(update, "cart-dhanmondi", "Green Starter Cart", "Dhanmondi", 650,
                "Compact hygienic cart with prep shelf, canopy and lockable storage.");
        addCart(update, "cart-mirpur", "Street Pro Cart", "Mirpur", 850,
                "Larger service counter with lighting, water container and secure storage.");
        addCart(update, "cart-bashundhara", "Campus Quick Cart", "Bashundhara", 550,
                "Lightweight cart designed for student and campus locations.");
        addCart(update, "cart-uttara", "Urban Grill Cart", "Uttara", 950,
                "Ventilated mobile grill cart with stainless counter and night lighting.");
        addCart(update, "cart-motijheel", "Office Lunch Cart", "Motijheel", 750,
                "Fast-service lunch cart with insulated storage and serving counter.");

        addPost(update, admin, "welcome-post",
                "Welcome to the enterprise marketplace. Explore verified sellers, transparent pricing and protected ordering.", now);
        addPost(update, admin, "food-safety-post",
                "Business tip: keep raw and cooked food separate, label preparation times and display prices clearly.", now - 60000);
        addPost(update, admin, "vendor-growth-post",
                "Young entrepreneurs can apply as vendors, complete their profile and use the free training library before launch.", now - 120000);

        firebase.root.updateChildren(update).addOnCompleteListener(task ->
                callback.onComplete(task.isSuccessful() ? null : task.getException()));
    }

    private static void addFood(Map<String, Object> update, AppUser admin, String id, String name,
                                String category, String description, double price, double rating,
                                int prepMinutes, double deliveryFee, boolean featured,
                                String imageUrl, long createdAt) {
        FoodItem item = new FoodItem();
        item.id = id;
        item.vendorId = admin.uid;
        item.vendorName = "FoodMoboChain Kitchen";
        item.name = name;
        item.category = category;
        item.description = description;
        item.price = price;
        item.rating = rating;
        item.preparationMinutes = prepMinutes;
        item.deliveryFee = deliveryFee;
        item.featured = featured;
        item.discountPercent = 0;
        item.imageUrl = imageUrl;
        item.available = true;
        item.createdAt = createdAt;
        update.put("foods/" + id, item);
    }

    private static void addCart(Map<String, Object> update, String id, String name, String location,
                                double dailyRate, String description) {
        RentalCart cart = new RentalCart();
        cart.id = id;
        cart.name = name;
        cart.location = location;
        cart.dailyRate = dailyRate;
        cart.description = description;
        cart.available = true;
        update.put("rentalCarts/" + id, cart);
    }

    private static void addPost(Map<String, Object> update, AppUser admin, String id,
                                String content, long createdAt) {
        NewsPost post = new NewsPost();
        post.id = id;
        post.authorId = admin.uid;
        post.authorName = admin.name;
        post.authorRole = "admin";
        post.content = content;
        post.createdAt = createdAt;
        update.put("newsfeed/" + id, post);
    }
}
