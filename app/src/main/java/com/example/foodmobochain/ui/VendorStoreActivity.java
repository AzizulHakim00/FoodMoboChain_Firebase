package com.example.foodmobochain.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.foodmobochain.R;
import com.example.foodmobochain.model.FoodItem;
import com.example.foodmobochain.util.ImageLoader;
import com.example.foodmobochain.util.Ui;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class VendorStoreActivity extends BaseScreenActivity {
    private String vendorId;
    private String vendorName;
    private LinearLayout menu;
    private TextView summary;

    public static void open(Context context, String vendorId, String vendorName) {
        if (vendorId == null) return;
        Intent intent = new Intent(context, VendorStoreActivity.class);
        intent.putExtra("vendorId", vendorId);
        intent.putExtra("vendorName", vendorName);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        vendorId = getIntent().getStringExtra("vendorId");
        vendorName = getIntent().getStringExtra("vendorName");
        setupScreen(safe(vendorName, "Vendor menu"), "Verified FoodMoboChain partner", true);
        buildHeader();
        listenMenu();
    }

    private void buildHeader() {
        LinearLayout header = Ui.softCard(this);
        header.addView(Ui.label(this, "LOCAL PARTNER"));
        header.addView(Ui.heading(this, safe(vendorName, "FoodMoboChain Kitchen")));
        summary = Ui.body(this, "Loading menu, rating and delivery information…");
        header.addView(summary);
        Button orders = Ui.outlineButton(this, "View my orders");
        orders.setOnClickListener(v -> open(OrdersActivity.class));
        header.addView(orders);
        content.addView(header);
        content.addView(Ui.heading(this, "Popular menu"));
        menu = new LinearLayout(this);
        menu.setOrientation(LinearLayout.VERTICAL);
        content.addView(menu);
    }

    private void listenMenu() {
        firebase.foods().orderByChild("vendorId").equalTo(vendorId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<FoodItem> foods = new ArrayList<>();
                        double ratingTotal = 0;
                        int ratingCount = 0;
                        int minimumTime = Integer.MAX_VALUE;
                        double minimumDelivery = Double.MAX_VALUE;
                        for (DataSnapshot child : snapshot.getChildren()) {
                            FoodItem item = child.getValue(FoodItem.class);
                            if (item == null || !item.available) continue;
                            if (item.id == null) item.id = child.getKey();
                            foods.add(item);
                            if (item.rating > 0) {
                                ratingTotal += item.rating;
                                ratingCount++;
                            }
                            minimumTime = Math.min(minimumTime,
                                    item.preparationMinutes > 0 ? item.preparationMinutes : 20);
                            minimumDelivery = Math.min(minimumDelivery, Math.max(0, item.deliveryFee));
                        }
                        foods.sort(Comparator.comparingDouble((FoodItem value) -> value.rating).reversed());
                        menu.removeAllViews();
                        for (FoodItem item : foods) menu.addView(foodCard(item));
                        if (foods.isEmpty()) menu.addView(Ui.body(VendorStoreActivity.this,
                                "This vendor has no available items right now."));
                        double rating = ratingCount == 0 ? 4.7 : ratingTotal / ratingCount;
                        String delivery = minimumDelivery == Double.MAX_VALUE || minimumDelivery <= 0
                                ? "Free delivery" : "Delivery from " + Ui.money(minimumDelivery);
                        summary.setText("★ " + String.format(Locale.US, "%.1f", rating)
                                + "  •  " + (minimumTime == Integer.MAX_VALUE ? 20 : minimumTime)
                                + "–" + ((minimumTime == Integer.MAX_VALUE ? 20 : minimumTime) + 10)
                                + " min  •  " + delivery + "\n" + foods.size() + " available dishes");
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Ui.toast(VendorStoreActivity.this, error.getMessage());
                    }
                });
    }

    private LinearLayout foodCard(FoodItem item) {
        LinearLayout card = Ui.card(this);
        ImageView image = new ImageView(this);
        image.setContentDescription(item.name + " image");
        image.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 170)));
        ImageLoader.load(image, item.imageUrl, R.drawable.ic_food_plate);
        card.addView(image);
        card.addView(Ui.label(this, item.category));
        card.addView(Ui.title(this, item.name));
        card.addView(Ui.body(this, item.description));
        TextView meta = Ui.title(this, Ui.money(item.price) + "  •  ★ "
                + String.format(Locale.US, "%.1f", item.rating > 0 ? item.rating : 4.7));
        meta.setTextColor(getColor(R.color.brand_orange));
        card.addView(meta);
        card.setOnClickListener(v -> FoodDetailActivity.open(this, item));
        Button view = Ui.button(this, "View and order");
        view.setOnClickListener(v -> FoodDetailActivity.open(this, item));
        card.addView(view);
        return card;
    }

    private String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }
}
