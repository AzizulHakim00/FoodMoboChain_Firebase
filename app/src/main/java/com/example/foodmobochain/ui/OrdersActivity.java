package com.example.foodmobochain.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.foodmobochain.R;
import com.example.foodmobochain.data.SparkOperations;
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
import java.util.List;

public class OrdersActivity extends BaseScreenActivity {
    private AppUser currentUser;
    private LinearLayout buyerList;
    private LinearLayout vendorList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupScreen("Orders & tracking", "Live store progress, delivery details and trusted reviews", true);
        LinearLayout intro = Ui.softCard(this);
        intro.addView(Ui.label(this, "REAL-TIME ORDER CENTRE"));
        intro.addView(Ui.heading(this, "Follow every store order from placement to delivery."));
        intro.addView(Ui.body(this,
                "Items from separate stores appear as separate orders. Only the owning vendor or administrator can progress an order."));
        content.addView(intro);
        content.addView(Ui.heading(this, "My food orders"));
        buyerList = new LinearLayout(this);
        buyerList.setOrientation(LinearLayout.VERTICAL);
        content.addView(buyerList);
        vendorList = new LinearLayout(this);
        vendorList.setOrientation(LinearLayout.VERTICAL);
        firebase.loadCurrentUser(user -> {
            currentUser = user;
            listenBuyerOrders();
            if (user != null && ("vendor".equals(user.role) || firebase.isAdminUser())) {
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
        firebase.orders().orderByChild("buyerId").equalTo(uid)
                .addValueEventListener(orderListener(buyerList, false));
    }

    private void listenVendorOrders() {
        String uid = firebase.uid();
        if (uid == null) return;
        firebase.orders().orderByChild("vendorId").equalTo(uid)
                .addValueEventListener(orderListener(vendorList, true));
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

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Ui.toast(OrdersActivity.this, error.getMessage());
            }
        };
    }

    private LinearLayout orderCard(FoodOrder order, boolean vendorView) {
        LinearLayout card = Ui.card(this);
        card.addView(Ui.label(this, order.status == null ? "PLACED" : order.status.replace('_', ' ')));
        card.addView(Ui.title(this, safe(order.storeName, "Marketplace store")));
        card.addView(Ui.body(this, "Order " + shortId(order.id) + "  •  " + Ui.money(order.computedTotal())
                + "  •  " + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                .format(new Date(order.createdAt))));
        if (!"cancelled".equals(order.status)) card.addView(statusTimeline(order.status));
        else {
            TextView cancelled = Ui.title(this, "Order cancelled");
            cancelled.setTextColor(getColor(R.color.brand_danger));
            card.addView(cancelled);
        }
        card.addView(Ui.body(this, statusMessage(order.status)));
        String details = (vendorView ? "Buyer: " + order.buyerName + "\n" : "")
                + "Deliver to: " + order.address
                + (TextUtils.isEmpty(order.deliveryNote) ? "" : "\nDelivery note: " + order.deliveryNote)
                + "\nPayment: Cash on delivery";
        card.addView(Ui.body(this, details));

        LinearLayout itemsCard = Ui.softCard(this);
        itemsCard.addView(Ui.label(this, "ORDER SUMMARY"));
        if (order.items != null) {
            for (CartLine line : order.items.values()) {
                if (line == null) continue;
                itemsCard.addView(Ui.body(this, line.quantity + " × " + line.name + " — "
                        + Ui.money(line.unitPrice * line.quantity)));
            }
        }
        card.addView(itemsCard);

        String uid = firebase.uid();
        if (vendorView && uid != null && uid.equals(order.vendorId)) {
            if ("delivered".equals(order.status)) {
                Button reviewBuyer = Ui.outlineButton(this, "Rate this buyer");
                reviewBuyer.setOnClickListener(v -> showReviewDialog(order, "buyer"));
                card.addView(reviewBuyer);
            } else {
                String label = nextStatusLabel(order.status);
                if (label != null) {
                    Button status = Ui.button(this, label);
                    status.setOnClickListener(v -> advanceStatus(order));
                    card.addView(status);
                }
            }
        } else if (!vendorView && uid != null && uid.equals(order.buyerId)
                && "delivered".equals(order.status)) {
            Button review = Ui.button(this, "Rate this transaction");
            review.setOnClickListener(v -> showReviewDialog(order, "vendor"));
            card.addView(review);
        }
        return card;
    }

    private LinearLayout statusTimeline(String status) {
        String[] labels = {"Placed", "Accepted", "Preparing", "Packed", "On way", "Delivered"};
        int active = statusIndex(status);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.topMargin = Ui.dp(this, 14);
        rowParams.bottomMargin = Ui.dp(this, 10);
        row.setLayoutParams(rowParams);
        for (int i = 0; i < labels.length; i++) {
            LinearLayout step = new LinearLayout(this);
            step.setOrientation(LinearLayout.VERTICAL);
            step.setGravity(Gravity.CENTER);
            step.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            TextView dot = Ui.title(this, i <= active ? "●" : "○");
            dot.setGravity(Gravity.CENTER);
            dot.setTextColor(getColor(i <= active ? R.color.brand_green : R.color.brand_muted));
            TextView label = Ui.text(this, labels[i], 8,
                    i <= active ? R.color.brand_green_dark : R.color.brand_muted);
            label.setGravity(Gravity.CENTER);
            step.addView(dot);
            step.addView(label);
            row.addView(step);
        }
        return row;
    }

    private int statusIndex(String status) {
        if ("accepted".equals(status)) return 1;
        if ("preparing".equals(status)) return 2;
        if ("packed".equals(status)) return 3;
        if ("out_for_delivery".equals(status)) return 4;
        if ("delivered".equals(status)) return 5;
        return 0;
    }

    private String statusMessage(String status) {
        if ("accepted".equals(status)) return "The store confirmed your order and will begin preparation soon.";
        if ("preparing".equals(status)) return "Your food is being prepared. The next update will appear automatically.";
        if ("packed".equals(status)) return "Your order is packed and waiting for dispatch.";
        if ("out_for_delivery".equals(status)) return "Your food has left the store and is travelling to the delivery address.";
        if ("delivered".equals(status)) return "Delivered successfully. Share a review to help the community.";
        if ("cancelled".equals(status)) return "This order will not progress further.";
        return "The order is waiting for the store to accept it.";
    }

    private String nextStatusLabel(String status) {
        if ("placed".equals(status)) return "Accept order";
        if ("accepted".equals(status)) return "Mark as preparing";
        if ("preparing".equals(status)) return "Mark as packed";
        if ("packed".equals(status)) return "Mark out for delivery";
        if ("out_for_delivery".equals(status)) return "Mark as delivered";
        return null;
    }

    private void advanceStatus(FoodOrder order) {
        SparkOperations.advanceOrderStatus(firebase, order, (status, error) -> Ui.toast(this,
                error == null ? "Order status updated."
                        : "Could not update status: " + safeMessage(error)));
    }

    private void showReviewDialog(FoodOrder order, String targetLabel) {
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
        if (TextUtils.isEmpty(comment) || comment.length() > 1000) {
            Ui.toast(this, "Add a written review of no more than 1000 characters.");
            return;
        }
        SparkOperations.submitReview(firebase, order, stars, comment, (reviewId, error) -> Ui.toast(this,
                error == null ? "Thank you. Your review was saved."
                        : "Review could not be saved: " + safeMessage(error)));
    }

    private String safeMessage(Exception exception) {
        return exception == null ? "Unknown database error." : exception.getLocalizedMessage();
    }

    private String shortId(String id) {
        if (id == null) return "";
        return id.length() <= 7 ? id : id.substring(id.length() - 7);
    }

    private String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }
}
