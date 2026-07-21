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
import com.example.foodmobochain.model.MarketplaceStore;
import com.example.foodmobochain.util.ImageLoader;
import com.example.foodmobochain.util.Ui;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class VendorStoreActivity extends BaseScreenActivity {
    private String vendorId;
    private String storeId;
    private String storeName;
    private LinearLayout menu;
    private TextView summary;
    private ImageView banner;

    public static void open(Context context, String vendorId, String vendorName) {
        if (vendorId == null) return;
        Intent intent = new Intent(context, VendorStoreActivity.class);
        intent.putExtra("vendorId", vendorId);
        intent.putExtra("storeName", vendorName);
        context.startActivity(intent);
    }

    public static void openStore(Context context, String storeId, String storeName) {
        if (storeId == null) return;
        Intent intent = new Intent(context, VendorStoreActivity.class);
        intent.putExtra("storeId", storeId);
        intent.putExtra("storeName", storeName);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        vendorId = getIntent().getStringExtra("vendorId");
        storeId = getIntent().getStringExtra("storeId");
        storeName = getIntent().getStringExtra("storeName");
        setupScreen(safe(storeName, "Store menu"), "Verified FoodMoboChain marketplace partner", true);
        buildHeader();
        if (storeId != null) loadStore();
        listenMenu();
    }

    private void buildHeader() {
        LinearLayout header = Ui.softCard(this);
        banner = new ImageView(this);
        banner.setContentDescription("Store banner");
        banner.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 190)));
        ImageLoader.load(banner, null, R.drawable.ic_food_plate);
        header.addView(banner);
        header.addView(Ui.label(this, "VERIFIED LOCAL PARTNER"));
        header.addView(Ui.heading(this, safe(storeName, "FoodMoboChain Kitchen")));
        summary = Ui.body(this, "Loading menu, rating and delivery information…");
        header.addView(summary);
        Button orders = Ui.outlineButton(this, "View my orders");
        orders.setOnClickListener(v -> open(OrdersActivity.class));
        header.addView(orders);
        content.addView(header);
        content.addView(Ui.heading(this, "Complete menu"));
        menu = new LinearLayout(this);
        menu.setOrientation(LinearLayout.VERTICAL);
        content.addView(menu);
    }

    private void loadStore() {
        firebase.stores().child(storeId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                MarketplaceStore store = snapshot.getValue(MarketplaceStore.class);
                if (store == null) return;
                storeName = store.name;
                ImageLoader.load(banner,
                        store.bannerUrl == null || store.bannerUrl.trim().isEmpty() ? store.imageUrl : store.bannerUrl,
                        R.drawable.ic_food_plate);
                summary.setText((store.open ? "Open now" : "Currently closed") + "  •  ★ "
                        + String.format(Locale.US, "%.1f", store.rating) + "  •  "
                        + Math.max(10, store.preparationMinutes) + "–"
                        + (Math.max(10, store.preparationMinutes) + 10) + " min  •  "
                        + (store.deliveryFee <= 0 ? "Free delivery" : Ui.money(store.deliveryFee) + " delivery")
                        + "\n" + safe(store.description, safe(store.cuisine, "Local marketplace store")));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Ui.toast(VendorStoreActivity.this, error.getMessage());
            }
        });
    }

    private void listenMenu() {
        Query query = storeId != null
                ? firebase.foods().orderByChild("storeId").equalTo(storeId)
                : firebase.foods().orderByChild("vendorId").equalTo(vendorId);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<FoodItem> foods = new ArrayList<>();
                double ratingTotal = 0;
                int ratingCount = 0;
                int minimumTime = Integer.MAX_VALUE;
                double minimumDelivery = Double.MAX_VALUE;
                for (DataSnapshot child : snapshot.getChildren()) {
                    FoodItem item = child.getValue(FoodItem.class);
                    if (item == null || !item.inStock()) continue;
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
                foods.sort(Comparator.comparing((FoodItem value) -> !value.featured)
                        .thenComparing(Comparator.comparingDouble((FoodItem value) -> value.rating).reversed()));
                menu.removeAllViews();
                for (FoodItem item : foods) menu.addView(foodCard(item));
                if (foods.isEmpty()) menu.addView(Ui.body(VendorStoreActivity.this,
                        "This store has no available items right now."));
                if (storeId == null) {
                    double rating = ratingCount == 0 ? 4.7 : ratingTotal / ratingCount;
                    String delivery = minimumDelivery == Double.MAX_VALUE || minimumDelivery <= 0
                            ? "Free delivery" : "Delivery from " + Ui.money(minimumDelivery);
                    summary.setText("★ " + String.format(Locale.US, "%.1f", rating)
                            + "  •  " + (minimumTime == Integer.MAX_VALUE ? 20 : minimumTime)
                            + "–" + ((minimumTime == Integer.MAX_VALUE ? 20 : minimumTime) + 10)
                            + " min  •  " + delivery + "\n" + foods.size() + " available dishes");
                }
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
        card.addView(Ui.label(this, item.category + (item.discountPercent > 0
                ? "  •  " + String.format(Locale.US, "%.0f%% OFF", item.discountPercent) : "")));
        card.addView(Ui.title(this, item.name));
        card.addView(Ui.body(this, item.description));
        TextView meta = Ui.title(this, Ui.money(item.salePrice()) + "  •  ★ "
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
