package com.example.foodmobochain.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.example.foodmobochain.model.AppUser;
import com.example.foodmobochain.model.CartLine;
import com.example.foodmobochain.model.FoodOrder;
import com.example.foodmobochain.util.Ui;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class BagActivity extends BaseScreenActivity {
    private final Map<String, CartLine> lines = new LinkedHashMap<>();
    private AppUser currentUser;
    private double total;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupScreen("Your bag", "Check quantities before placing the order", true);
        firebase.loadCurrentUser(user -> currentUser = user);
        listenToBag();
    }

    private void listenToBag() {
        String uid = firebase.uid();
        if (uid == null) return;
        firebase.carts().child(uid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                lines.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    CartLine line = child.getValue(CartLine.class);
                    if (line != null && line.quantity > 0) lines.put(child.getKey(), line);
                }
                render();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Ui.toast(BagActivity.this, error.getMessage());
            }
        });
    }

    private void render() {
        content.removeAllViews();
        if (lines.isEmpty()) {
            LinearLayout empty = Ui.softCard(this);
            empty.addView(Ui.heading(this, "Your bag is empty."));
            empty.addView(Ui.body(this, "Browse the food catalogue and add something fresh."));
            Button browse = Ui.button(this, "Browse foods");
            browse.setOnClickListener(v -> open(FoodCatalogActivity.class));
            empty.addView(browse);
            content.addView(empty);
            return;
        }

        total = 0;
        for (Map.Entry<String, CartLine> entry : lines.entrySet()) {
            CartLine line = entry.getValue();
            total += line.unitPrice * line.quantity;
            LinearLayout card = Ui.card(this);
            card.addView(Ui.title(this, line.name));
            card.addView(Ui.body(this, line.vendorName + "  •  " + Ui.money(line.unitPrice) + " each"));
            card.addView(Ui.title(this, "Quantity: " + line.quantity + "  •  "
                    + Ui.money(line.unitPrice * line.quantity)));
            Button plus = Ui.outlineButton(this, "+ Add one");
            plus.setOnClickListener(v -> updateQuantity(entry.getKey(), line.quantity + 1));
            card.addView(plus);
            Button minus = Ui.outlineButton(this, line.quantity == 1 ? "Remove" : "− Remove one");
            minus.setOnClickListener(v -> updateQuantity(entry.getKey(), line.quantity - 1));
            card.addView(minus);
            content.addView(card);
        }

        LinearLayout totalCard = Ui.softCard(this);
        totalCard.addView(Ui.label(this, "ORDER TOTAL"));
        totalCard.addView(Ui.heading(this, Ui.money(total)));
        totalCard.addView(Ui.body(this, "Payment is recorded as cash on delivery in this academic version."));
        Button checkout = Ui.button(this, "Place order");
        checkout.setOnClickListener(v -> askForAddress());
        totalCard.addView(checkout);
        content.addView(totalCard);
    }

    private void updateQuantity(String foodId, int quantity) {
        String uid = firebase.uid();
        if (uid == null) return;
        if (quantity <= 0) {
            firebase.carts().child(uid).child(foodId).removeValue();
        } else {
            firebase.carts().child(uid).child(foodId).child("quantity").setValue(quantity);
        }
    }

    private void askForAddress() {
        EditText address = Ui.input(this, "Delivery address");
        address.setSingleLine(false);
        address.setMinLines(2);
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delivery details")
                .setMessage("Enter where the vendor should deliver this order.")
                .setView(address)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Place order", (dialog, which) -> {
                    String value = address.getText().toString().trim();
                    if (TextUtils.isEmpty(value)) {
                        Ui.toast(this, "A delivery address is required.");
                    } else {
                        createOrder(value);
                    }
                }).show();
    }

    private void createOrder(String address) {
        String uid = firebase.uid();
        if (uid == null || lines.isEmpty()) return;
        String orderId = firebase.orders().push().getKey();
        if (orderId == null) return;
        FoodOrder order = new FoodOrder();
        order.id = orderId;
        order.buyerId = uid;
        order.buyerName = currentUser == null ? "Buyer" : currentUser.name;
        order.address = address;
        order.status = "placed";
        order.total = total;
        order.createdAt = System.currentTimeMillis();
        order.items.putAll(lines);

        Map<String, Object> updates = new HashMap<>();
        updates.put("orders/" + orderId, order);
        updates.put("userOrders/" + uid + "/" + orderId, order);
        for (CartLine line : lines.values()) {
            if (line.vendorId != null) {
                updates.put("vendorOrders/" + line.vendorId + "/" + orderId, order);
            }
        }
        updates.put("carts/" + uid, null);
        firebase.root.updateChildren(updates).addOnCompleteListener(task -> {
            Ui.toast(this, task.isSuccessful()
                    ? "Order placed successfully."
                    : "Could not place the order: " + task.getException());
            if (task.isSuccessful()) open(OrdersActivity.class);
        });
    }
}
