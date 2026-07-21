package com.example.foodmobochain.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;

import androidx.annotation.NonNull;

import com.example.foodmobochain.R;
import com.example.foodmobochain.data.SparkOperations;
import com.example.foodmobochain.model.CartLine;
import com.example.foodmobochain.model.UserAddress;
import com.example.foodmobochain.util.ImageLoader;
import com.example.foodmobochain.util.Ui;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BagActivity extends BaseScreenActivity {
    private final Map<String, CartLine> lines = new LinkedHashMap<>();
    private double total;
    private boolean checkoutInProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupScreen("Your bag", "Review store groups, quantities and delivery details", true);
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
            empty.addView(Ui.body(this, "Browse stores and add something fresh."));
            Button stores = Ui.button(this, "Browse stores");
            stores.setOnClickListener(v -> open(StoresActivity.class));
            empty.addView(stores);
            content.addView(empty);
            return;
        }

        total = 0;
        for (Map.Entry<String, CartLine> entry : lines.entrySet()) {
            CartLine line = entry.getValue();
            total += line.unitPrice * line.quantity;
            LinearLayout card = Ui.card(this);
            if (!TextUtils.isEmpty(line.imageUrl)) {
                ImageView image = new ImageView(this);
                image.setContentDescription(line.name + " image");
                image.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 125)));
                ImageLoader.load(image, line.imageUrl, R.drawable.ic_food_plate);
                card.addView(image);
            }
            card.addView(Ui.label(this, TextUtils.isEmpty(line.vendorName) ? "MARKETPLACE STORE" : line.vendorName));
            card.addView(Ui.title(this, line.name));
            card.addView(Ui.body(this, Ui.money(line.unitPrice) + " each"));
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
        totalCard.addView(Ui.label(this, "CASH ON DELIVERY TOTAL"));
        totalCard.addView(Ui.heading(this, Ui.money(total)));
        totalCard.addView(Ui.body(this,
                "Items from different stores become separate orders. Firebase re-checks each official price, store owner and quantity before saving them."));
        Button addresses = Ui.outlineButton(this, "Manage saved addresses");
        addresses.setOnClickListener(v -> open(AddressBookActivity.class));
        totalCard.addView(addresses);
        Button checkout = Ui.button(this, "Continue to protected checkout");
        checkout.setOnClickListener(v -> loadAddressesForCheckout());
        totalCard.addView(checkout);
        content.addView(totalCard);
    }

    private void updateQuantity(String foodId, int quantity) {
        String uid = firebase.uid();
        if (uid == null || checkoutInProgress) return;
        if (quantity <= 0) firebase.carts().child(uid).child(foodId).removeValue();
        else firebase.carts().child(uid).child(foodId).child("quantity").setValue(Math.min(20, quantity));
    }

    private void loadAddressesForCheckout() {
        String uid = firebase.uid();
        if (uid == null) return;
        firebase.addresses().child(uid).get().addOnCompleteListener(task -> {
            List<UserAddress> saved = new ArrayList<>();
            if (task.isSuccessful()) {
                for (DataSnapshot child : task.getResult().getChildren()) {
                    UserAddress address = child.getValue(UserAddress.class);
                    if (address != null) saved.add(address);
                }
            }
            saved.sort((left, right) -> Boolean.compare(right.defaultAddress, left.defaultAddress));
            showCheckoutDialog(saved);
        });
    }

    private void showCheckoutDialog(List<UserAddress> saved) {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(Ui.dp(this, 20), 0, Ui.dp(this, 20), 0);
        List<String> labels = new ArrayList<>();
        for (UserAddress address : saved) labels.add((address.defaultAddress ? "Default · " : "")
                + address.label + " — " + address.displayAddress());
        labels.add("Enter another address");
        Spinner selector = new Spinner(this);
        selector.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, labels));
        EditText manualAddress = Ui.input(this, "Another delivery address");
        manualAddress.setSingleLine(false);
        manualAddress.setMinLines(2);
        EditText note = Ui.input(this, "Delivery note: floor, landmark or instructions");
        note.setSingleLine(false);
        form.addView(selector);
        form.addView(manualAddress);
        form.addView(note);
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delivery and payment")
                .setMessage("Payment method: Cash on delivery")
                .setView(form)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Place order", (dialog, which) -> {
                    int selected = selector.getSelectedItemPosition();
                    String address = selected < saved.size()
                            ? saved.get(selected).displayAddress()
                            : manualAddress.getText().toString().trim();
                    String deliveryNote = note.getText().toString().trim();
                    if (TextUtils.isEmpty(address) || address.length() < 5) {
                        Ui.toast(this, "Select a saved address or enter a valid delivery address.");
                    } else createOrder(address, deliveryNote);
                }).show();
    }

    private void createOrder(String address, String note) {
        if (firebase.uid() == null || lines.isEmpty() || checkoutInProgress) return;
        checkoutInProgress = true;
        SparkOperations.placeOrders(firebase, address, note, (count, error) -> {
            checkoutInProgress = false;
            if (error != null || count == null) {
                Ui.toast(this, "Could not place the order: " + safeMessage(error));
                render();
                return;
            }
            Ui.toast(this, count == 1
                    ? "Protected order placed successfully."
                    : count + " store orders placed successfully.");
            open(OrdersActivity.class);
        });
    }

    private String safeMessage(Exception exception) {
        return exception == null ? "Unknown database error." : exception.getLocalizedMessage();
    }
}
