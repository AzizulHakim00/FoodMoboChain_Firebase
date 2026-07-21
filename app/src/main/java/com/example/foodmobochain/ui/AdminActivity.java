package com.example.foodmobochain.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.example.foodmobochain.data.SparkOperations;
import com.example.foodmobochain.model.AppUser;
import com.example.foodmobochain.model.FoodItem;
import com.example.foodmobochain.model.NewsPost;
import com.example.foodmobochain.model.RentalCart;
import com.example.foodmobochain.util.Ui;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class AdminActivity extends BaseScreenActivity {
    private AppUser admin;
    private LinearLayout applications;
    private LinearLayout flags;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupScreen("Admin operations", "Marketplace safety, content and seller management", true);
        firebase.loadCurrentUser(user -> {
            admin = user;
            if (user == null || !(firebase.isAdminUser() || "admin".equals(user.role))) {
                content.addView(Ui.body(this, "This screen is restricted to the FoodMoboChain administrator."));
                return;
            }
            buildDashboard();
            listenApplications();
            listenFlags();
        });
    }

    private void buildDashboard() {
        content.removeAllViews();
        LinearLayout adminCard = Ui.softCard(this);
        adminCard.addView(Ui.label(this, "MARKETPLACE CONTROL CENTRE"));
        adminCard.addView(Ui.heading(this, "Operate a safe and useful food ecosystem."));
        adminCard.addView(Ui.body(this,
                "Approve genuine vendors, moderate reports and initialise a complete demonstration marketplace with food photography and realistic data."));
        Button seed = Ui.button(this, "Create or refresh enterprise starter data");
        seed.setOnClickListener(v -> new MaterialAlertDialogBuilder(this)
                .setTitle("Create enterprise starter data?")
                .setMessage("This adds or refreshes sample foods, rental carts and announcements without deleting real user records.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Create data", (dialog, which) -> seedData())
                .show());
        adminCard.addView(seed);
        Button marketplace = Ui.outlineButton(this, "Preview customer marketplace");
        marketplace.setOnClickListener(v -> open(FoodCatalogActivity.class));
        adminCard.addView(marketplace);
        content.addView(adminCard);

        content.addView(Ui.spacer(this, 16));
        content.addView(Ui.heading(this, "Vendor applications"));
        applications = new LinearLayout(this);
        applications.setOrientation(LinearLayout.VERTICAL);
        content.addView(applications);

        content.addView(Ui.spacer(this, 18));
        content.addView(Ui.heading(this, "Reported content"));
        flags = new LinearLayout(this);
        flags.setOrientation(LinearLayout.VERTICAL);
        content.addView(flags);
    }

    private void listenApplications() {
        firebase.vendorApplications().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                applications.removeAllViews();
                int count = 0;
                for (DataSnapshot child : snapshot.getChildren()) {
                    AppUser user = child.getValue(AppUser.class);
                    if (user == null || !"pending".equals(user.status)) continue;
                    applications.addView(applicationCard(user));
                    count++;
                }
                if (count == 0) applications.addView(Ui.body(AdminActivity.this,
                        "No pending vendor applications."));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Ui.toast(AdminActivity.this, error.getMessage());
            }
        });
    }

    private LinearLayout applicationCard(AppUser user) {
        LinearLayout card = Ui.card(this);
        card.addView(Ui.label(this, "PENDING VENDOR"));
        card.addView(Ui.title(this, user.name));
        card.addView(Ui.body(this, user.email
                + (user.businessName == null ? "" : "\nBusiness: " + user.businessName)
                + (user.location == null ? "" : "\nLocation: " + user.location)));
        Button approve = Ui.button(this, "Approve vendor");
        approve.setOnClickListener(v -> setVendorStatus(user, "approved"));
        card.addView(approve);
        Button reject = Ui.outlineButton(this, "Reject application");
        reject.setOnClickListener(v -> setVendorStatus(user, "rejected"));
        card.addView(reject);
        return card;
    }

    private void setVendorStatus(AppUser user, String status) {
        SparkOperations.setVendorStatus(firebase, user, status, (result, error) -> Ui.toast(this,
                error == null ? "Vendor status and permissions updated."
                        : "Could not update vendor: " + safeMessage(error)));
    }

    private String safeMessage(Exception exception) {
        return exception == null ? "Unknown database error." : exception.getLocalizedMessage();
    }

    private void listenFlags() {
        firebase.flags().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                flags.removeAllViews();
                int count = 0;
                for (DataSnapshot child : snapshot.getChildren()) {
                    if (!"open".equals(String.valueOf(child.child("status").getValue()))) continue;
                    flags.addView(flagCard(child));
                    count++;
                }
                if (count == 0) flags.addView(Ui.body(AdminActivity.this, "No open reports."));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Ui.toast(AdminActivity.this, error.getMessage());
            }
        });
    }

    private LinearLayout flagCard(DataSnapshot snapshot) {
        String id = snapshot.getKey();
        String contentId = String.valueOf(snapshot.child("contentId").getValue());
        String reason = String.valueOf(snapshot.child("reason").getValue());
        String preview = String.valueOf(snapshot.child("contentPreview").getValue());
        LinearLayout card = Ui.card(this);
        card.addView(Ui.label(this, "REPORTED NEWS POST"));
        card.addView(Ui.title(this, reason));
        card.addView(Ui.body(this, preview));
        Button resolve = Ui.button(this, "Keep post and resolve report");
        resolve.setOnClickListener(v -> firebase.flags().child(id).child("status").setValue("resolved"));
        card.addView(resolve);
        Button remove = Ui.outlineButton(this, "Remove post and resolve");
        remove.setOnClickListener(v -> new MaterialAlertDialogBuilder(this)
                .setTitle("Remove reported post?")
                .setMessage("This action removes the newsfeed post for every user.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Remove", (dialog, which) -> {
                    Map<String, Object> update = new HashMap<>();
                    update.put("newsfeed/" + contentId, null);
                    update.put("flags/" + id + "/status", "removed");
                    firebase.root.updateChildren(update);
                }).show());
        card.addView(remove);
        return card;
    }

    private void seedData() {
        if (admin == null || !firebase.isAdminUser()) return;
        Map<String, Object> update = new HashMap<>();
        long now = System.currentTimeMillis();

        addFood(update, "sample-kacchi", "Kacchi Biryani", "Rice",
                "Aromatic basmati rice layered with tender mutton, potato, saffron and house spices.",
                360, 4.9, 32, 40, true,
                "https://images.unsplash.com/photo-1589302168068-964664d93dc0?auto=format&fit=crop&w=1200&q=80", now);
        addFood(update, "sample-chicken-biryani", "Chicken Biryani", "Rice",
                "Fragrant rice, marinated chicken, caramelised onion and cooling raita.",
                250, 4.8, 28, 30, true,
                "https://images.unsplash.com/photo-1563379926898-05f4575a45d8?auto=format&fit=crop&w=1200&q=80", now);
        addFood(update, "sample-beef-tehari", "Old Dhaka Beef Tehari", "Traditional",
                "Mustard-oil rice with tender beef, green chilli and classic Old Dhaka flavour.",
                280, 4.7, 30, 35, false,
                "https://images.unsplash.com/photo-1512058564366-18510be2db19?auto=format&fit=crop&w=1200&q=80", now);
        addFood(update, "sample-fuchka", "Spicy Fuchka", "Street Food",
                "Crispy shells filled with spiced potato, chickpea and tangy tamarind water.",
                120, 4.9, 12, 20, true,
                "https://images.unsplash.com/photo-1601050690597-df0568f70950?auto=format&fit=crop&w=1200&q=80", now);
        addFood(update, "sample-chotpoti", "Dhaka Chotpoti", "Street Food",
                "Warm chickpea curry with potato, egg, tamarind, coriander and crunchy toppings.",
                140, 4.7, 15, 20, false,
                "https://images.unsplash.com/photo-1601050690117-94f5f6fa8bd7?auto=format&fit=crop&w=1200&q=80", now);
        addFood(update, "sample-momo", "Chicken Momo", "Snacks",
                "Steamed dumplings packed with seasoned chicken and served with fiery tomato chutney.",
                180, 4.8, 18, 25, true,
                "https://images.unsplash.com/photo-1563245372-f21724e3856d?auto=format&fit=crop&w=1200&q=80", now);
        addFood(update, "sample-chowmein", "Chicken Chow Mein", "Noodles",
                "Wok-tossed noodles with chicken, vegetables and a balanced savoury sauce.",
                220, 4.6, 22, 30, false,
                "https://images.unsplash.com/photo-1559314809-0d155014e29e?auto=format&fit=crop&w=1200&q=80", now);
        addFood(update, "sample-burger", "Double Smash Burger", "Fast Food",
                "Two seared beef patties, melted cheese, pickles and signature sauce in a toasted bun.",
                320, 4.8, 24, 35, true,
                "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?auto=format&fit=crop&w=1200&q=80", now);
        addFood(update, "sample-fries", "Loaded Masala Fries", "Fast Food",
                "Crispy fries loaded with cheese sauce, masala chicken, herbs and chilli.",
                190, 4.5, 16, 25, false,
                "https://images.unsplash.com/photo-1573080496219-bb080dd4f877?auto=format&fit=crop&w=1200&q=80", now);
        addFood(update, "sample-grilled-chicken", "Herb Grilled Chicken", "Grill",
                "Juicy herb-marinated chicken with roasted vegetables and house sauce.",
                340, 4.7, 30, 40, true,
                "https://images.unsplash.com/photo-1532550907401-a500c9a57435?auto=format&fit=crop&w=1200&q=80", now);
        addFood(update, "sample-wings", "Naga Chicken Wings", "Grill",
                "Crispy chicken wings coated with smoky naga chilli glaze.",
                260, 4.6, 25, 35, false,
                "https://images.unsplash.com/photo-1527477396000-e27163b481c2?auto=format&fit=crop&w=1200&q=80", now);
        addFood(update, "sample-borhani", "Classic Borhani", "Drinks",
                "Chilled spiced yoghurt drink with mint, mustard and roasted cumin.",
                90, 4.5, 5, 15, false,
                "https://images.unsplash.com/photo-1544145945-f90425340c7e?auto=format&fit=crop&w=1200&q=80", now);
        addFood(update, "sample-mango-lassi", "Mango Lassi", "Drinks",
                "Creamy yoghurt blended with ripe mango and a touch of cardamom.",
                150, 4.7, 7, 15, true,
                "https://images.unsplash.com/photo-1572490122747-3968b75cc699?auto=format&fit=crop&w=1200&q=80", now);
        addFood(update, "sample-falooda", "Royal Falooda", "Dessert",
                "Rose milk, vermicelli, basil seeds, jelly and ice cream in a chilled dessert glass.",
                210, 4.8, 12, 20, true,
                "https://images.unsplash.com/photo-1563805042-7684c019e1cb?auto=format&fit=crop&w=1200&q=80", now);
        addFood(update, "sample-brownie", "Chocolate Brownie", "Dessert",
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

        addPost(update, "welcome-post", "Welcome to the enterprise marketplace. Explore verified sellers, transparent pricing and protected ordering.", now);
        addPost(update, "food-safety-post", "Business tip: keep raw and cooked food separate, label preparation times and display prices clearly.", now - 60000);
        addPost(update, "vendor-growth-post", "Young entrepreneurs can apply as vendors, complete their profile and use the free training library before launch.", now - 120000);

        firebase.root.updateChildren(update).addOnCompleteListener(task ->
                Ui.toast(this, task.isSuccessful()
                        ? "Enterprise starter marketplace is ready."
                        : "Could not create starter content: " + safeMessage(task.getException())));
    }

    private void addFood(Map<String, Object> update, String id, String name, String category,
                         String description, double price, double rating, int prepMinutes,
                         double deliveryFee, boolean featured, String imageUrl, long createdAt) {
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

    private void addCart(Map<String, Object> update, String id, String name, String location,
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

    private void addPost(Map<String, Object> update, String id, String content, long createdAt) {
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
