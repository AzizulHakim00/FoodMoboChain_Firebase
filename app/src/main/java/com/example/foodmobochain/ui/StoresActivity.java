package com.example.foodmobochain.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.example.foodmobochain.R;
import com.example.foodmobochain.model.MarketplaceStore;
import com.example.foodmobochain.util.ImageLoader;
import com.example.foodmobochain.util.Ui;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class StoresActivity extends BaseScreenActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupScreen("Restaurants & carts", "Verified local sellers across Dhaka", true);
        listenStores();
    }

    private void listenStores() {
        content.removeAllViews();
        LinearLayout intro = Ui.softCard(this);
        intro.addView(Ui.label(this, "LARGE MARKETPLACE"));
        intro.addView(Ui.heading(this, "Choose a store, then explore its complete menu."));
        intro.addView(Ui.body(this,
                "Stores show live opening status, delivery cost, minimum order, estimated preparation time and customer rating."));
        content.addView(intro);
        content.addView(Ui.body(this, "Loading stores…"));
        firebase.stores().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<MarketplaceStore> stores = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    MarketplaceStore store = child.getValue(MarketplaceStore.class);
                    if (store == null) continue;
                    if (store.id == null) store.id = child.getKey();
                    stores.add(store);
                }
                stores.sort(Comparator.comparing((MarketplaceStore value) -> !value.featured)
                        .thenComparing(Comparator.comparingDouble((MarketplaceStore value) -> value.rating).reversed()));
                render(stores);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Ui.toast(StoresActivity.this, error.getMessage());
            }
        });
    }

    private void render(List<MarketplaceStore> stores) {
        content.removeAllViews();
        LinearLayout intro = Ui.softCard(this);
        intro.addView(Ui.label(this, "VERIFIED SELLERS"));
        intro.addView(Ui.heading(this, stores.size() + " marketplace stores"));
        intro.addView(Ui.body(this, "Open a store to see only its available foods and offers."));
        content.addView(intro);
        for (MarketplaceStore store : stores) content.addView(storeCard(store));
        if (stores.isEmpty()) content.addView(Ui.body(this,
                "No stores are available yet. The administrator can create the large starter marketplace."));
    }

    private LinearLayout storeCard(MarketplaceStore store) {
        LinearLayout card = Ui.card(this);
        ImageView banner = new ImageView(this);
        banner.setContentDescription(store.name + " store image");
        banner.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 180)));
        ImageLoader.load(banner,
                store.bannerUrl == null || store.bannerUrl.trim().isEmpty() ? store.imageUrl : store.bannerUrl,
                R.drawable.ic_food_plate);
        card.addView(banner);
        card.addView(Ui.label(this, (store.open ? "OPEN" : "CLOSED")
                + (store.verified ? "  •  VERIFIED" : "")));
        card.addView(Ui.heading(this, store.name));
        card.addView(Ui.body(this, safe(store.cuisine, "Mixed cuisine") + "  •  "
                + safe(store.location, "Dhaka")));
        card.addView(Ui.body(this, "★ " + String.format(Locale.US, "%.1f", store.rating)
                + "  •  " + Math.max(10, store.preparationMinutes) + "–"
                + (Math.max(10, store.preparationMinutes) + 10) + " min  •  "
                + (store.deliveryFee <= 0 ? "Free delivery" : Ui.money(store.deliveryFee) + " delivery")));
        card.addView(Ui.body(this, "Minimum order " + Ui.money(Math.max(0, store.minimumOrder))));
        Button menu = Ui.button(this, "View complete menu");
        menu.setEnabled(store.open);
        menu.setOnClickListener(v -> VendorStoreActivity.openStore(this, store.id, store.name));
        card.addView(menu);
        return card;
    }

    private String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }
}
