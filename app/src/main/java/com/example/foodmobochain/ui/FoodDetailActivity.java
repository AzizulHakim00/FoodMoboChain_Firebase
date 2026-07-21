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
        intent.putExtra("name", item.name);
        intent.putExtra("description", item.description);
        intent.putExtra("category", item.category);
        intent.putExtra("imageUrl", item.imageUrl);
        intent.putExtra("price", item.price);
        intent.putExtra("rating", item.rating);
        intent.putExtra("deliveryFee", item.deliveryFee);
        intent.putExtra("discountPercent", item.discountPercent);
        intent.putExtra("preparationMinutes", item.preparationMinutes);
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
                item.vendorName == null ? "Local vendor" : item.vendorName, true);
        render();
    }

    private FoodItem readItem() {
        Intent intent = getIntent();
        FoodItem value = new FoodItem();
        value.id = intent.getStringExtra("id");
        value.vendorId = intent.getStringExtra("vendorId");
        value.vendorName = intent.getStringExtra("vendorName");
        value.name = intent.getStringExtra("name");
        value.description = intent.getStringExtra("description");
        value.category = intent.getStringExtra("category");
        value.imageUrl = intent.getStringExtra("imageUrl");
        value.price = intent.getDoubleExtra("price", 0);
        value.rating = intent.getDoubleExtra("rating", 0);
        value.deliveryFee = intent.getDoubleExtra("deliveryFee", 0);
        value.discountPercent = intent.getDoubleExtra("discountPercent", 0);
        value.preparationMinutes = intent.getIntExtra("preparationMinutes", 20);
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
                LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 240)));
        image.setBackgroundResource(R.drawable.bg_soft_card);
        ImageLoader.load(image, item.imageUrl, R.drawable.ic_food_plate);
        content.addView(image);

        LinearLayout card = Ui.card(this);
        card.addView(Ui.label(this, safe(item.category, "POPULAR DISH")));
        card.addView(Ui.heading(this, safe(item.name, "Food item")));
        card.addView(Ui.body(this, safe(item.description, "Freshly prepared by a local FoodMoboChain partner.")));
        card.addView(Ui.spacer(this, 10));

        String meta = "★ " + String.format(Locale.US, "%.1f", item.rating > 0 ? item.rating : 4.7)
                + "   •   " + Math.max(10, item.preparationMinutes) + "–"
                + (Math.max(10, item.preparationMinutes) + 10) + " min"
                + "   •   " + (item.deliveryFee <= 0 ? "Free delivery" : Ui.money(item.deliveryFee) + " delivery");
        card.addView(Ui.body(this, meta));

        if (item.discountPercent > 0) {
            card.addView(Ui.label(this, String.format(Locale.US, "%.0f%% OFF", item.discountPercent)));
            TextView oldPrice = Ui.body(this, "Regular price " + Ui.money(item.price));
            card.addView(oldPrice);
        }
        TextView price = Ui.heading(this, Ui.money(item.salePrice()));
        price.setTextColor(getColor(R.color.brand_orange));
        card.addView(price);

        LinearLayout quantityRow = new LinearLayout(this);
        quantityRow.setOrientation(LinearLayout.HORIZONTAL);
        quantityRow.setGravity(Gravity.CENTER_VERTICAL);
        Button minus = compactButton("−");
        quantityView = Ui.title(this, String.valueOf(quantity));
        quantityView.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams quantityParams = new LinearLayout.LayoutParams(0,
                Ui.dp(this, 48), 1f);
        quantityView.setLayoutParams(quantityParams);
        Button plus = compactButton("+");
        minus.setOnClickListener(v -> {
            if (quantity > 1) quantity--;
            refreshQuantity();
        });
        plus.setOnClickListener(v -> {
            if (quantity < 20) quantity++;
            refreshQuantity();
        });
        quantityRow.addView(minus);
        quantityRow.addView(quantityView);
        quantityRow.addView(plus);
        card.addView(quantityRow);

        addButton = Ui.button(this, "Add to bag — " + Ui.money(item.salePrice() * quantity));
        addButton.setEnabled(item.available);
        addButton.setOnClickListener(v -> addToBag());
        card.addView(addButton);

        favoriteButton = Ui.outlineButton(this, favoriteText());
        favoriteButton.setOnClickListener(v -> {
            FavoritesStore.toggle(this, item.id);
            favoriteButton.setText(favoriteText());
        });
        card.addView(favoriteButton);

        Button store = Ui.outlineButton(this, "View " + safe(item.vendorName, "vendor") + " menu");
        store.setOnClickListener(v -> VendorStoreActivity.open(this, item.vendorId, item.vendorName));
        card.addView(store);
        content.addView(card);

        LinearLayout promise = Ui.softCard(this);
        promise.addView(Ui.label(this, "FOODMOBILE PROMISE"));
        promise.addView(Ui.title(this, "Transparent price. Verified seller. Protected order."));
        promise.addView(Ui.body(this,
                "The database rules verify the official menu price and vendor before accepting your order."));
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
        addButton.setText("Add to bag — " + Ui.money(item.salePrice() * quantity));
    }

    private String favoriteText() {
        return FavoritesStore.contains(this, item.id) ? "♥ Saved to favourites" : "♡ Save to favourites";
    }

    private void addToBag() {
        String uid = firebase.uid();
        if (uid == null || item.id == null || !item.available) return;
        addButton.setEnabled(false);
        firebase.carts().child(uid).child(item.id).runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                CartLine line = currentData.getValue(CartLine.class);
                if (line == null) line = new CartLine(item, 0);
                line.unitPrice = item.price;
                line.quantity = Math.min(20, line.quantity + quantity);
                currentData.setValue(line);
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed, DataSnapshot snapshot) {
                addButton.setEnabled(true);
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
