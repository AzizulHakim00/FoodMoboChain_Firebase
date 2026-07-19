package com.example.foodmobochain.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

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
        setupScreen("Admin control", "Vendor approval and community moderation", true);
        firebase.loadCurrentUser(user -> {
            admin = user;
            if (user == null || !"admin".equals(user.role)) {
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
        adminCard.addView(Ui.label(this, "SYSTEM ADMINISTRATOR"));
        adminCard.addView(Ui.heading(this, "Keep the marketplace trustworthy."));
        adminCard.addView(Ui.body(this, "Approve genuine vendors, resolve reported content and create sample data for the first demonstration."));
        Button seed = Ui.button(this, "Create starter food and rental data");
        seed.setOnClickListener(v -> seedData());
        adminCard.addView(seed);
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
                if (count == 0) applications.addView(Ui.body(AdminActivity.this, "No pending vendor applications."));
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {
                Ui.toast(AdminActivity.this, error.getMessage());
            }
        });
    }

    private LinearLayout applicationCard(AppUser user) {
        LinearLayout card = Ui.card(this);
        card.addView(Ui.label(this, "PENDING VENDOR"));
        card.addView(Ui.title(this, user.name));
        card.addView(Ui.body(this, user.email + (user.businessName == null ? "" : "\n" + user.businessName)));
        Button approve = Ui.button(this, "Approve vendor");
        approve.setOnClickListener(v -> setVendorStatus(user, "approved"));
        card.addView(approve);
        Button reject = Ui.outlineButton(this, "Reject application");
        reject.setOnClickListener(v -> setVendorStatus(user, "rejected"));
        card.addView(reject);
        return card;
    }

    private void setVendorStatus(AppUser user, String status) {
        Map<String, Object> request = new HashMap<>();
        request.put("uid", user.uid);
        request.put("status", status);
        firebase.functions.getHttpsCallable("approveVendor").call(request)
                .addOnCompleteListener(task -> Ui.toast(this, task.isSuccessful()
                        ? "Vendor status and permissions updated."
                        : "Could not update vendor: " + safeMessage(task.getException())));
    }

    private String safeMessage(Exception exception) {
        return exception == null ? "Unknown server error." : exception.getLocalizedMessage();
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

            @Override public void onCancelled(@NonNull DatabaseError error) {
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
        if (admin == null) return;
        Map<String, Object> update = new HashMap<>();
        addFood(update, "sample-kacchi", "Kacchi Biryani", "Rice",
                "Fragrant basmati rice, tender mutton, potato and house spices.", 320, 4.9);
        addFood(update, "sample-borhani", "Borhani", "Drinks",
                "Chilled spiced yoghurt drink with mint and roasted cumin.", 80, 4.5);
        addFood(update, "sample-grilled-chicken", "Grilled Chicken", "Grill",
                "Herb-grilled chicken served with fresh seasonal vegetables.", 280, 4.7);
        addCart(update, "cart-dhanmondi", "Green Starter Cart", "Dhanmondi", 650,
                "Compact hygienic cart with prep shelf, canopy and storage.");
        addCart(update, "cart-mirpur", "Street Pro Cart", "Mirpur", 850,
                "Larger service counter with lighting and lockable storage.");
        addCart(update, "cart-bashundhara", "Campus Quick Cart", "Bashundhara", 550,
                "Lightweight cart designed for student and campus locations.");
        NewsPost post = new NewsPost();
        post.id = "welcome-post";
        post.authorId = admin.uid;
        post.authorName = "Food Mobo Chain";
        post.authorRole = "admin";
        post.content = "Welcome to FoodMoboChain. Keep food safe, prices transparent and every customer respected.";
        post.createdAt = System.currentTimeMillis();
        update.put("newsfeed/welcome-post", post);
        firebase.root.updateChildren(update).addOnCompleteListener(task ->
                Ui.toast(this, task.isSuccessful() ? "Starter content is ready." : "Could not create starter content."));
    }

    private void addFood(Map<String, Object> update, String id, String name, String category,
                         String description, double price, double rating) {
        FoodItem item = new FoodItem();
        item.id = id;
        item.vendorId = admin.uid;
        item.vendorName = "FoodMoboChain Kitchen";
        item.name = name;
        item.category = category;
        item.description = description;
        item.price = price;
        item.rating = rating;
        item.available = true;
        item.createdAt = System.currentTimeMillis();
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
}
