package com.example.foodmobochain.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.example.foodmobochain.model.CartLine;
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
    private double total;
    private boolean checkoutInProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupScreen("Your bag", "Check quantities before placing the order", true);
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
                if (!checkoutInProgress) render();
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
        totalCard.addView(Ui.label(this, "ESTIMATED TOTAL"));
        totalCard.addView(Ui.heading(this, Ui.money(total)));
        totalCard.addView(Ui.body(this,
                "The secure server checks current menu prices and creates a separate order for each vendor."));
        Button checkout = Ui.button(this, "Place secure order");
        checkout.setOnClickListener(v -> askForAddress());
        totalCard.addView(checkout);
        content.addView(totalCard);
    }

    private void updateQuantity(String foodId, int quantity) {
        String uid = firebase.uid();
        if (uid == null || checkoutInProgress) return;
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
                    if (TextUtils.isEmpty(value) || value.length() < 5) {
                        Ui.toast(this, "A valid delivery address is required.");
                    } else {
                        createOrder(value);
                    }
                }).show();
    }

    private void createOrder(String address) {
        if (firebase.uid() == null || lines.isEmpty() || checkoutInProgress) return;
        checkoutInProgress = true;
        Map<String, Object> request = new HashMap<>();
        request.put("address", address);
        firebase.functions.getHttpsCallable("placeOrder").call(request)
                .addOnCompleteListener(task -> {
                    checkoutInProgress = false;
                    if (!task.isSuccessful()) {
                        Ui.toast(this, "Could not place the order: " + safeMessage(task.getException()));
                        render();
                        return;
                    }
                    int count = 1;
                    Object data = task.getResult() == null ? null : task.getResult().getData();
                    if (data instanceof Map) {
                        Object value = ((Map<?, ?>) data).get("orderCount");
                        if (value instanceof Number) count = ((Number) value).intValue();
                    }
                    Ui.toast(this, count == 1
                            ? "Order placed securely."
                            : count + " vendor orders placed securely.");
                    open(OrdersActivity.class);
                });
    }

    private String safeMessage(Exception exception) {
        return exception == null ? "Unknown server error." : exception.getLocalizedMessage();
    }
}
