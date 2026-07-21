package com.example.foodmobochain.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.foodmobochain.R;
import com.example.foodmobochain.model.CartLine;
import com.example.foodmobochain.model.FoodItem;
import com.example.foodmobochain.util.FavoritesStore;
import com.example.foodmobochain.util.ImageLoader;
import com.example.foodmobochain.util.Ui;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;

import java.util.Locale;

public class FoodDetailActivity extends BaseScreenActivity {
    private FoodItem item;
    private int quantity = 1;
    private TextView quantityView;
    private Button addButton;
    private Button favoriteButton;

    public static void open(Context context, FoodItem item) {
        Intent intent = new Intent(context, FoodDetailActivity.class);
        intent.putExtra("id", item.id);
        intent.putExtra("vendorId", item.vendorId);
        intent.putExtra("vendorName", item.vendorName);
        intent.putExtra("storeId", item.storeId);
        intent.putExtra("name", item.name);
        intent.putExtra("description", item.description);
        intent.putExtra("category", item.category);
        intent.putExtra("imageUrl", item.imageUrl);
        intent.putExtra("tags", item.tags);
        intent.putExtra("price", item.price);
        intent.putExtra("regularPrice", item.regularPrice);
        intent.putExtra("rating", item.rating);
        intent.putExtra("deliveryFee", item.deliveryFee);
        intent.putExtra("discountPercent", item.discountPercent);
        intent.putExtra("preparationMinutes", item.preparationMinutes);
        intent.putExtra("stockCount", item.stockCount);
        intent.putExtra("spicyLevel", item.spicyLevel);
        intent.putExtra("vegetarian", item.vegetarian);
        intent.putExtra("featured", item.featured);
        intent.putExtra("createdAt", item.createdAt);
        intent.putExtra("available", item.available);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        item = readItem();
        setupScreen(item.name == null ? "Food details" : item.name,
                item.vendorName == null ? "Local store" : item.vendorName, true);
        render();
    }

    private FoodItem readItem() {
        Intent intent = getIntent();
        FoodItem value = new FoodItem();
        value.id = intent.getStringExtra("id");
        value.vendorId = intent.getStringExtra("vendorId");
        value.vendorName = intent.getStringExtra("vendorName");
        value.storeId = intent.getStringExtra("storeId");
        value.name = intent.getStringExtra("name");
        value.description = intent.getStringExtra("description");
        value.category = intent.getStringExtra("category");
        value.imageUrl = intent.getStringExtra("imageUrl");
        value.tags = intent.getStringExtra("tags");
        value.price = intent.getDoubleExtra("price", 0);
        value.regularPrice = intent.getDoubleExtra("regularPrice", value.price);
        value.rating = intent.getDoubleExtra("rating", 0);
        value.deliveryFee = intent.getDoubleExtra("deliveryFee", 0);
        value.discountPercent = intent.getDoubleExtra("discountPercent", 0);
        value.preparationMinutes = intent.getIntExtra("preparationMinutes", 20);
        value.stockCount = intent.getIntExtra("stockCount", 1);
        value.spicyLevel = intent.getIntExtra("spicyLevel", 0);
        value.vegetarian = intent.getBooleanExtra("vegetarian", false);
        value.featured = intent.getBooleanExtra("featured", false);
        value.createdAt = intent.getLongExtra("createdAt", 0);
        value.available = intent.getBooleanExtra("available", true);
        return value;
    }

    private void render() {
        content.removeAllViews();
        ImageView image = new ImageView(this);
        image.setContentDescription(item.name + " image");
        image.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 250)));
        image.setBackgroundResource(R.drawable.bg_soft_card);
        ImageLoader.load(image, item.imageUrl, R.drawable.ic_food_plate);
        content.addView(image);

        LinearLayout card = Ui.card(this);
        String badge = safe(item.category, "POPULAR DISH");
        if (item.discountPercent > 0) badge += "  •  "
                + String.format(Locale.US, "%.0f%% OFF", item.discountPercent);
        card.addView(Ui.label(this, badge));
        card.addView(Ui.heading(this, safe(item.name, "Food item")));
        card.addView(Ui.body(this, safe(item.description,
                "Freshly prepared by a local FoodMoboChain partner.")));
        String attributes = (item.vegetarian ? "Vegetarian  •  " : "")
                + (item.spicyLevel > 0 ? "Spice " + item.spicyLevel + "/3  •  " : "")
                + safe(item.tags, "Fresh marketplace item");
        card.addView(Ui.body(this, attributes));
        card.addView(Ui.spacer(this, 10));

        String meta = "★ " + String.format(Locale.US, "%.1f", item.rating > 0 ? item.rating : 4.7)
                + "   •   " + Math.max(10, item.preparationMinutes) + "–"
                + (Math.max(10, item.preparationMinutes) + 10) + " min"
                + "   •   " + (item.deliveryFee <= 0 ? "Free delivery" : Ui.money(item.deliveryFee) + " delivery");
        card.addView(Ui.body(this, meta));
        if (item.listPrice() > item.price) {
            card.addView(Ui.body(this, "Regular price " + Ui.money(item.listPrice())));
        }
        TextView price = Ui.heading(this, Ui.money(item.price));
        price.setTextColor(getColor(R.color.brand_orange));
        card.addView(price);
        card.addView(Ui.body(this, item.inStock()
                ? (item.stockCount > 0 ? item.stockCount + " portions currently available" : "Available now")
                : "Currently unavailable"));

        LinearLayout quantityRow = new LinearLayout(this);
        quantityRow.setOrientation(LinearLayout.HORIZONTAL);
        quantityRow.setGravity(Gravity.CENTER_VERTICAL);
        Button minus = compactButton("−");
        quantityView = Ui.title(this, String.valueOf(quantity));
        quantityView.setGravity(Gravity.CENTER);
        quantityView.setLayoutParams(new LinearLayout.LayoutParams(0, Ui.dp(this, 48), 1f));
        Button plus = compactButton("+");
        minus.setOnClickListener(v -> { if (quantity > 1) quantity--; refreshQuantity(); });
        plus.setOnClickListener(v -> {
            int max = item.stockCount > 0 ? Math.min(20, item.stockCount) : 20;
            if (quantity < max) quantity++;
            refreshQuantity();
        });
        quantityRow.addView(minus);
        quantityRow.addView(quantityView);
        quantityRow.addView(plus);
        card.addView(quantityRow);

        addButton = Ui.button(this, "Add to bag — " + Ui.money(item.price * quantity));
        addButton.setEnabled(item.inStock());
        addButton.setOnClickListener(v -> addToBag());
        card.addView(addButton);
        favoriteButton = Ui.outlineButton(this, favoriteText());
        favoriteButton.setOnClickListener(v -> {
            FavoritesStore.toggle(this, item.id);
            favoriteButton.setText(favoriteText());
        });
        card.addView(favoriteButton);
        Button store = Ui.outlineButton(this, "View " + safe(item.vendorName, "store") + " menu");
        store.setOnClickListener(v -> {
            if (item.storeId != null) VendorStoreActivity.openStore(this, item.storeId, item.vendorName);
            else VendorStoreActivity.open(this, item.vendorId, item.vendorName);
        });
        card.addView(store);
        content.addView(card);

        LinearLayout promise = Ui.softCard(this);
        promise.addView(Ui.label(this, "FOODMOBILE PROMISE"));
        promise.addView(Ui.title(this, "The displayed checkout price is the official database price."));
        promise.addView(Ui.body(this,
                "Firebase Security Rules verify food availability, store ownership, quantity and unit price before accepting an order."));
        content.addView(promise);
    }

    private Button compactButton(String text) {
        Button button = Ui.outlineButton(this, text);
        button.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(this, 64), Ui.dp(this, 48)));
        button.setTextSize(22);
        return button;
    }

    private void refreshQuantity() {
        quantityView.setText(String.valueOf(quantity));
        addButton.setText("Add to bag — " + Ui.money(item.price * quantity));
    }

    private String favoriteText() {
        return FavoritesStore.contains(this, item.id) ? "♥ Saved to favourites" : "♡ Save to favourites";
    }

    private void addToBag() {
        String uid = firebase.uid();
        if (uid == null || item.id == null || !item.inStock()) return;
        addButton.setEnabled(false);
        firebase.carts().child(uid).child(item.id).runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                CartLine line = currentData.getValue(CartLine.class);
                if (line == null) line = new CartLine(item, 0);
                line.unitPrice = item.price;
                int max = item.stockCount > 0 ? Math.min(20, item.stockCount) : 20;
                line.quantity = Math.min(max, line.quantity + quantity);
                currentData.setValue(line);
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed, DataSnapshot snapshot) {
                addButton.setEnabled(item.inStock());
                Ui.toast(FoodDetailActivity.this, committed
                        ? quantity + " × " + item.name + " added to your bag."
                        : "Could not update your bag.");
            }
        });
    }

    private String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }
}
