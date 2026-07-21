package com.example.foodmobochain.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.foodmobochain.R;
import com.example.foodmobochain.model.FoodItem;
import com.example.foodmobochain.model.PromoBanner;
import com.example.foodmobochain.util.ImageLoader;
import com.example.foodmobochain.util.Ui;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class OffersActivity extends BaseScreenActivity {
    private LinearLayout banners;
    private LinearLayout discountedFoods;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupScreen("Offers & promotions", "Live marketplace deals without coupon confusion", true);
        banners = new LinearLayout(this);
        banners.setOrientation(LinearLayout.VERTICAL);
        discountedFoods = new LinearLayout(this);
        discountedFoods.setOrientation(LinearLayout.VERTICAL);
        content.addView(Ui.heading(this, "Featured campaigns"));
        content.addView(banners);
        content.addView(Ui.spacer(this, 18));
        content.addView(Ui.heading(this, "Discounted foods"));
        content.addView(discountedFoods);
        listenBanners();
        listenFoods();
    }

    private void listenBanners() {
        firebase.banners().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long now = System.currentTimeMillis();
                List<PromoBanner> values = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    PromoBanner banner = child.getValue(PromoBanner.class);
                    if (banner == null || !banner.active) continue;
                    if (banner.startsAt > 0 && banner.startsAt > now) continue;
                    if (banner.endsAt > 0 && banner.endsAt < now) continue;
                    if (banner.id == null) banner.id = child.getKey();
                    values.add(banner);
                }
                values.sort(Comparator.comparingInt((PromoBanner value) -> value.priority).reversed());
                banners.removeAllViews();
                for (PromoBanner value : values) banners.addView(bannerCard(value));
                if (values.isEmpty()) banners.addView(Ui.body(OffersActivity.this,
                        "No promotional campaigns are active right now."));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Ui.toast(OffersActivity.this, error.getMessage());
            }
        });
    }

    private void listenFoods() {
        firebase.foods().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<FoodItem> values = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    FoodItem item = child.getValue(FoodItem.class);
                    if (item == null || !item.inStock() || item.discountPercent <= 0) continue;
                    if (item.id == null) item.id = child.getKey();
                    values.add(item);
                }
                values.sort(Comparator.comparingDouble((FoodItem value) -> value.discountPercent).reversed());
                discountedFoods.removeAllViews();
                for (FoodItem item : values) discountedFoods.addView(foodCard(item));
                if (values.isEmpty()) discountedFoods.addView(Ui.body(OffersActivity.this,
                        "No discounted menu items are active right now."));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Ui.toast(OffersActivity.this, error.getMessage());
            }
        });
    }

    private LinearLayout bannerCard(PromoBanner banner) {
        LinearLayout card = Ui.softCard(this);
        ImageView image = new ImageView(this);
        image.setContentDescription(banner.title + " promotion");
        image.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 180)));
        ImageLoader.load(image, banner.imageUrl, R.drawable.ic_food_plate);
        card.addView(image);
        card.addView(Ui.label(this, banner.badge == null ? "MARKETPLACE OFFER" : banner.badge));
        card.addView(Ui.heading(this, banner.title));
        card.addView(Ui.body(this, banner.subtitle));
        Button action = Ui.button(this, "Explore offer");
        action.setOnClickListener(v -> {
            if ("store".equals(banner.actionType)) {
                VendorStoreActivity.openStore(this, banner.actionValue, banner.title);
            } else {
                open(FoodCatalogActivity.class);
            }
        });
        card.addView(action);
        return card;
    }

    private LinearLayout foodCard(FoodItem item) {
        LinearLayout card = Ui.card(this);
        ImageView image = new ImageView(this);
        image.setContentDescription(item.name + " image");
        image.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 160)));
        ImageLoader.load(image, item.imageUrl, R.drawable.ic_food_plate);
        card.addView(image);
        card.addView(Ui.label(this, String.format(Locale.US, "%.0f%% OFF", item.discountPercent)));
        card.addView(Ui.title(this, item.name));
        card.addView(Ui.body(this, item.vendorName + "  •  " + item.category));
        TextView price = Ui.heading(this, Ui.money(item.salePrice()) + "  ·  was " + Ui.money(item.price));
        price.setTextColor(getColor(R.color.brand_orange));
        card.addView(price);
        Button view = Ui.button(this, "View discounted dish");
        view.setOnClickListener(v -> FoodDetailActivity.open(this, item));
        card.addView(view);
        return card;
    }
}
