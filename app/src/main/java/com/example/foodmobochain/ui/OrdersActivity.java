package com.example.foodmobochain.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;

import androidx.annotation.NonNull;

import com.example.foodmobochain.model.AppUser;
import com.example.foodmobochain.model.CartLine;
import com.example.foodmobochain.model.FoodOrder;
import com.example.foodmobochain.util.Ui;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrdersActivity extends BaseScreenActivity {
    private AppUser currentUser;
    private LinearLayout buyerList;
    private LinearLayout vendorList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupScreen("Orders & reviews", "Track transactions and build community trust", true);
        content.addView(Ui.heading(this, "My food orders"));
        buyerList = new LinearLayout(this);
        buyerList.setOrientation(LinearLayout.VERTICAL);
        content.addView(buyerList);
        vendorList = new LinearLayout(this);
        vendorList.setOrientation(LinearLayout.VERTICAL);
        firebase.loadCurrentUser(user -> {
            currentUser = user;
            listenBuyerOrders();
            if (user != null && ("vendor".equals(user.role) || "admin".equals(user.role))) {
                content.addView(Ui.spacer(this, 18));
                content.addView(Ui.heading(this, "Orders for my business"));
                content.addView(vendorList);
                listenVendorOrders();
            }
        });
    }

    private void listenBuyerOrders() {
        String uid = firebase.uid();
        if (uid == null) return;
        firebase.root.child("userOrders").child(uid).addValueEventListener(orderListener(buyerList, false));
    }

    private void listenVendorOrders() {
        String uid = firebase.uid();
        if (uid == null) return;
        firebase.vendorOrders().child(uid).addValueEventListener(orderListener(vendorList, true));
    }

    private ValueEventListener orderListener(LinearLayout target, boolean vendorView) {
        return new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<FoodOrder> orders = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    FoodOrder order = child.getValue(FoodOrder.class);
                    if (order != null) orders.add(order);
                }
                orders.sort(Comparator.comparingLong((FoodOrder order) -> order.createdAt).reversed());
                target.removeAllViews();
                for (FoodOrder order : orders) target.addView(orderCard(order, vendorView));
                if (orders.isEmpty()) target.addView(Ui.body(OrdersActivity.this,
                        vendorView ? "No customer orders yet." : "You have not placed an order yet."));
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {
                Ui.toast(OrdersActivity.this, error.getMessage());
            }
        };
    }

    private LinearLayout orderCard(FoodOrder order, boolean vendorView) {
        LinearLayout card = Ui.card(this);
        card.addView(Ui.label(this, order.status == null ? "PLACED" : order.status));
        card.addView(Ui.title(this, "Order " + shortId(order.id) + "  •  " + Ui.money(order.total)));
        card.addView(Ui.body(this, DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                .format(new Date(order.createdAt))));
        if (vendorView) card.addView(Ui.body(this, "Buyer: " + order.buyerName + "\nDeliver to: " + order.address));
        if (order.items != null) {
            for (CartLine line : order.items.values()) {
                if (vendorView && currentUser != null && !currentUser.uid.equals(line.vendorId)) continue;
                card.addView(Ui.body(this, line.quantity + " × " + line.name + " — "
                        + Ui.money(line.unitPrice * line.quantity)));
            }
        }
        if (vendorView) {
            if ("delivered".equals(order.status)) {
                Button reviewBuyer = Ui.outlineButton(this, "Rate this buyer");
                reviewBuyer.setOnClickListener(v -> showReviewDialog(order, order.buyerId, "buyer"));
                card.addView(reviewBuyer);
            } else {
                String label = nextStatusLabel(order.status);
                if (label != null) {
                    Button status = Ui.button(this, label);
                    status.setOnClickListener(v -> advanceStatus(order));
                    card.addView(status);
                }
            }
        } else if ("delivered".equals(order.status)) {
            Button review = Ui.outlineButton(this, "Rate this transaction");
            review.setOnClickListener(v -> {
                String vendorId = firstVendor(order);
                if (vendorId == null) {
                    Ui.toast(this, "This order has no vendor information.");
                } else {
                    showReviewDialog(order, vendorId, "vendor");
                }
            });
            card.addView(review);
        }
        return card;
    }

    private String nextStatusLabel(String status) {
        if ("placed".equals(status)) return "Accept order";
        if ("accepted".equals(status)) return "Mark as preparing";
        if ("preparing".equals(status)) return "Mark out for delivery";
        if ("out_for_delivery".equals(status)) return "Mark as delivered";
        return null;
    }

    private void advanceStatus(FoodOrder order) {
        if (order.id == null) return;
        Map<String, Object> request = new HashMap<>();
        request.put("orderId", order.id);
        firebase.functions.getHttpsCallable("advanceOrderStatus").call(request)
                .addOnCompleteListener(task -> Ui.toast(this, task.isSuccessful()
                        ? "Order status updated securely."
                        : "Could not update status: " + safeMessage(task.getException())));
    }

    private void showReviewDialog(FoodOrder order, String targetUserId, String targetLabel) {
        if (targetUserId == null) return;
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(Ui.dp(this, 20), 0, Ui.dp(this, 20), 0);
        Spinner stars = new Spinner(this);
        stars.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                Arrays.asList("5 — Excellent", "4 — Good", "3 — Average", "2 — Poor", "1 — Very poor")));
        EditText comment = Ui.input(this, "Write about your experience");
        comment.setSingleLine(false);
        form.addView(stars);
        form.addView(comment);
        new MaterialAlertDialogBuilder(this)
                .setTitle("Rate this " + targetLabel)
                .setView(form)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Submit review", (dialog, which) -> {
                    int rating = 5 - stars.getSelectedItemPosition();
                    submitReview(order, rating, comment.getText().toString().trim());
                }).show();
    }

    private void submitReview(FoodOrder order, int stars, String comment) {
        String uid = firebase.uid();
        if (uid == null || TextUtils.isEmpty(comment)) {
            Ui.toast(this, "Please add a short written review.");
            return;
        }
        Map<String, Object> request = new HashMap<>();
        request.put("orderId", order.id);
        request.put("stars", stars);
        request.put("comment", comment);
        firebase.functions.getHttpsCallable("submitReview").call(request)
                .addOnCompleteListener(task -> Ui.toast(this, task.isSuccessful()
                        ? "Thank you. Your review was submitted securely."
                        : "Review could not be saved: " + safeMessage(task.getException())));
    }

    private String safeMessage(Exception exception) {
        return exception == null ? "Unknown server error." : exception.getLocalizedMessage();
    }

    private String firstVendor(FoodOrder order) {
        if (order.vendorId != null) return order.vendorId;
        if (order.items == null) return null;
        for (CartLine line : order.items.values()) if (line.vendorId != null) return line.vendorId;
        return null;
    }

    private String shortId(String id) {
        if (id == null) return "";
        return id.length() <= 7 ? id : id.substring(id.length() - 7);
    }
}
