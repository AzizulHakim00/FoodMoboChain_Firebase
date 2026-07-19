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
import com.example.foodmobochain.model.Review;
import com.example.foodmobochain.util.Ui;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
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
                Button status = Ui.button(this, nextStatusLabel(order.status));
                status.setOnClickListener(v -> advanceStatus(order));
                card.addView(status);
            }
        } else if ("delivered".equals(order.status)) {
            Button review = Ui.outlineButton(this, "Rate this transaction");
            review.setOnClickListener(v -> {
                String vendorId = firstVendor(order);
                if (vendorId == null) Ui.toast(this, "This order has no vendor information.");
                else showReviewDialog(order, vendorId, "vendor");
            });
            card.addView(review);
        }
        return card;
    }

    private String nextStatusLabel(String status) {
        if ("placed".equals(status)) return "Accept order";
        if ("accepted".equals(status)) return "Mark as preparing";
        if ("preparing".equals(status)) return "Mark as delivered";
        return "Order delivered";
    }

    private void advanceStatus(FoodOrder order) {
        String next;
        if ("placed".equals(order.status)) next = "accepted";
        else if ("accepted".equals(order.status)) next = "preparing";
        else if ("preparing".equals(order.status)) next = "delivered";
        else return;
        Map<String, Object> update = new HashMap<>();
        update.put("orders/" + order.id + "/status", next);
        update.put("userOrders/" + order.buyerId + "/" + order.id + "/status", next);
        if (order.items != null) {
            for (CartLine line : order.items.values()) {
                if (line.vendorId != null) update.put("vendorOrders/" + line.vendorId + "/" + order.id + "/status", next);
            }
        }
        firebase.root.updateChildren(update).addOnCompleteListener(task ->
                Ui.toast(this, task.isSuccessful() ? "Order status updated." : "Could not update status."));
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
                    submitReview(order, targetUserId, rating, comment.getText().toString().trim());
                }).show();
    }

    private void submitReview(FoodOrder order, String vendorId, int stars, String comment) {
        String uid = firebase.uid();
        if (uid == null || TextUtils.isEmpty(comment)) {
            Ui.toast(this, "Please add a short written review.");
            return;
        }
        String reviewId = order.id + "_" + uid;
        firebase.reviews().child(reviewId).get().addOnCompleteListener(existingTask -> {
            if (existingTask.isSuccessful() && existingTask.getResult().exists()) {
                Ui.toast(this, "You already reviewed this order.");
                return;
            }
            Review review = new Review();
            review.id = reviewId;
            review.orderId = order.id;
            review.authorId = uid;
            review.targetUserId = vendorId;
            review.stars = stars;
            review.comment = comment;
            review.createdAt = System.currentTimeMillis();
            firebase.reviews().child(reviewId).setValue(review).addOnCompleteListener(saveTask -> {
                if (!saveTask.isSuccessful()) {
                    Ui.toast(this, "Review could not be saved.");
                    return;
                }
                updateRating(vendorId, stars);
            });
        });
    }

    private void updateRating(String vendorId, int stars) {
        firebase.root.child("ratingStats").child(vendorId).runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                Object sumValue = currentData.child("sum").getValue();
                Object countValue = currentData.child("count").getValue();
                double sum = sumValue instanceof Number ? ((Number) sumValue).doubleValue() : 0;
                long count = countValue instanceof Number ? ((Number) countValue).longValue() : 0;
                currentData.child("sum").setValue(sum + stars);
                currentData.child("count").setValue(count + 1);
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed, DataSnapshot snapshot) {
                if (!committed) {
                    Ui.toast(OrdersActivity.this, "Review saved, but the rating could not be recalculated.");
                    return;
                }
                Object sumValue = snapshot.child("sum").getValue();
                Object countValue = snapshot.child("count").getValue();
                double sum = sumValue instanceof Number ? ((Number) sumValue).doubleValue() : 0;
                long count = countValue instanceof Number ? ((Number) countValue).longValue() : 1;
                firebase.users().child(vendorId).child("rating").setValue(sum / count);
                Ui.toast(OrdersActivity.this, "Thank you. Your review was submitted.");
            }
        });
    }

    private String firstVendor(FoodOrder order) {
        if (order.items == null) return null;
        for (CartLine line : order.items.values()) if (line.vendorId != null) return line.vendorId;
        return null;
    }

    private String shortId(String id) {
        if (id == null) return "";
        return id.length() <= 7 ? id : id.substring(id.length() - 7);
    }
}
