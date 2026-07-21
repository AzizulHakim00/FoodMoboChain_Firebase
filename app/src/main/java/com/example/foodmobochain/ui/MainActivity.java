package com.example.foodmobochain.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.foodmobochain.R;
import com.example.foodmobochain.model.AppUser;
import com.example.foodmobochain.model.FoodItem;
import com.example.foodmobochain.model.FoodOrder;
import com.example.foodmobochain.util.ImageLoader;
import com.example.foodmobochain.util.Ui;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends BaseScreenActivity {
    private AppUser currentUser;
    private LinearLayout featuredList;
    private LinearLayout vendorList;
    private LinearLayout activeOrderContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupScreen("FoodMoboChain", "Food, carts and opportunity — connected", false);
        showLoading();
        firebase.loadCurrentUser(this::render);
    }

    private void showLoading() {
        content.removeAllViews();
        content.addView(Ui.body(this, "Preparing your marketplace…"));
    }

    private void render(AppUser user) {
        content.removeAllViews();
        if (user == null) {
            Ui.toast(this, "Your profile is missing. Please sign in again.");
            signOut();
            return;
        }
        currentUser = user;
        addDeliveryHeader(user);
        addSearchAndPromotion();
        addServiceShortcuts();
        addCategories();

        content.addView(Ui.heading(this, "Featured near you"));
        featuredList = new LinearLayout(this);
        featuredList.setOrientation(LinearLayout.VERTICAL);
        featuredList.addView(Ui.body(this, "Loading popular dishes…"));
        content.addView(featuredList);

        content.addView(Ui.spacer(this, 16));
        content.addView(Ui.heading(this, "Popular local sellers"));
        vendorList = new LinearLayout(this);
        vendorList.setOrientation(LinearLayout.VERTICAL);
        vendorList.addView(Ui.body(this, "Loading verified sellers…"));
        content.addView(vendorList);

        content.addView(Ui.spacer(this, 16));
        content.addView(Ui.heading(this, "Active order"));
        activeOrderContainer = new LinearLayout(this);
        activeOrderContainer.setOrientation(LinearLayout.VERTICAL);
        activeOrderContainer.addView(Ui.body(this, "Checking your latest order…"));
        content.addView(activeOrderContainer);

        addRoleWorkspace(user);
        loadMarketplaceData();
        listenLatestOrder();
    }

    private void addDeliveryHeader(AppUser user) {
        LinearLayout header = Ui.card(this);
        header.addView(Ui.label(this, "DELIVER TO"));
        header.addView(Ui.title(this, safe(user.location, "Set your delivery location")));
        header.addView(Ui.body(this, "Hello " + safe(user.name, "food explorer")
                + "  •  " + (firebase.isAdminUser() ? "Administrator" : capitalise(user.role))));
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button notifications = compactAction("Notifications");
        notifications.setOnClickListener(v -> open(NotificationsActivity.class));
        Button profile = compactAction("Profile");
        profile.setOnClickListener(v -> open(ProfileActivity.class));
        actions.addView(notifications);
        actions.addView(profile);
        header.addView(actions);
        content.addView(header);
    }

    private void addSearchAndPromotion() {
        Button search = Ui.outlineButton(this, "⌕  Search foods, dishes or vendors");
        search.setOnClickListener(v -> open(FoodCatalogActivity.class));
        content.addView(search);

        LinearLayout hero = Ui.softCard(this);
        hero.addView(Ui.label(this, "WELCOME OFFER"));
        ImageView illustration = new ImageView(this);
        illustration.setContentDescription("Food marketplace promotion");
        illustration.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 150)));
        ImageLoader.load(illustration,
                "https://images.unsplash.com/photo-1504674900247-0877df9cc836?auto=format&fit=crop&w=1200&q=80",
                R.drawable.ic_food_plate);
        hero.addView(illustration);
        hero.addView(Ui.heading(this, "Your city’s best street food,\none trusted marketplace."));
        hero.addView(Ui.body(this,
                "Browse verified sellers, compare delivery times, place protected orders and support young entrepreneurs."));
        Button explore = Ui.button(this, "Explore food now");
        explore.setOnClickListener(v -> open(FoodCatalogActivity.class));
        hero.addView(explore);
        content.addView(hero);
    }

    private void addServiceShortcuts() {
        content.addView(Ui.heading(this, "Your services"));
        addShortcutRow(
                new Shortcut("Browse foods", "Menus and offers", FoodCatalogActivity.class),
                new Shortcut("My bag", "Checkout securely", BagActivity.class));
        addShortcutRow(
                new Shortcut("Orders", "Live progress", OrdersActivity.class),
                new Shortcut("Favourites", "Saved dishes", FavoritesActivity.class));
        addShortcutRow(
                new Shortcut("Rent a cart", "Start a business", RentalActivity.class),
                new Shortcut("Free training", "Learn and certify", TrainingActivity.class));
        addShortcutRow(
                new Shortcut("Newsfeed", "Tips and offers", NewsfeedActivity.class),
                new Shortcut("Notifications", "Updates in one feed", NotificationsActivity.class));
    }

    private void addShortcutRow(Shortcut left, Shortcut right) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.addView(shortcutCard(left));
        row.addView(shortcutCard(right));
        content.addView(row);
    }

    private LinearLayout shortcutCard(Shortcut shortcut) {
        LinearLayout card = Ui.card(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        params.setMargins(Ui.dp(this, 3), Ui.dp(this, 3), Ui.dp(this, 3), Ui.dp(this, 3));
        card.setLayoutParams(params);
        card.addView(Ui.title(this, shortcut.title));
        card.addView(Ui.body(this, shortcut.subtitle));
        card.addView(Ui.label(this, "OPEN  ›"));
        card.setOnClickListener(v -> open(shortcut.destination));
        return card;
    }

    private void addCategories() {
        content.addView(Ui.heading(this, "Browse categories"));
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        for (String category : new String[]{"Biryani", "Street food", "Burger", "Momo"}) {
            Button button = compactAction(category);
            button.setOnClickListener(v -> open(FoodCatalogActivity.class));
            row.addView(button);
        }
        content.addView(row);
    }

    private Button compactAction(String text) {
        Button button = Ui.outlineButton(this, text);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, Ui.dp(this, 48), 1f);
        params.setMargins(Ui.dp(this, 3), Ui.dp(this, 6), Ui.dp(this, 3), 0);
        button.setLayoutParams(params);
        button.setTextSize(12);
        return button;
    }

    private void loadMarketplaceData() {
        firebase.foods().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<FoodItem> foods = new ArrayList<>();
                Map<String, List<FoodItem>> vendors = new LinkedHashMap<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    FoodItem item = child.getValue(FoodItem.class);
                    if (item == null || !item.available) continue;
                    if (item.id == null) item.id = child.getKey();
                    foods.add(item);
                    if (item.vendorId != null) vendors
                            .computeIfAbsent(item.vendorId, ignored -> new ArrayList<>()).add(item);
                }
                foods.sort(Comparator
                        .comparing((FoodItem item) -> !item.featured)
                        .thenComparing(Comparator.comparingDouble((FoodItem item) -> item.rating).reversed()));
                renderFeatured(foods);
                renderVendors(vendors);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                featuredList.removeAllViews();
                featuredList.addView(Ui.body(MainActivity.this, error.getMessage()));
            }
        });
    }

    private void renderFeatured(List<FoodItem> foods) {
        featuredList.removeAllViews();
        int shown = 0;
        for (FoodItem item : foods) {
            if (shown++ >= 5) break;
            LinearLayout card = Ui.card(this);
            ImageView image = new ImageView(this);
            image.setContentDescription(item.name + " image");
            image.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 175)));
            ImageLoader.load(image, item.imageUrl, R.drawable.ic_food_plate);
            card.addView(image);
            card.addView(Ui.label(this, item.featured ? "FEATURED" : item.category));
            card.addView(Ui.title(this, item.name));
            card.addView(Ui.body(this, item.vendorName + "  •  ★ "
                    + String.format(Locale.US, "%.1f", item.rating > 0 ? item.rating : 4.7)
                    + "  •  " + (item.preparationMinutes > 0 ? item.preparationMinutes : 20) + "–"
                    + ((item.preparationMinutes > 0 ? item.preparationMinutes : 20) + 10) + " min"));
            TextView price = Ui.heading(this, Ui.money(item.price));
            price.setTextColor(getColor(R.color.brand_orange));
            card.addView(price);
            card.setOnClickListener(v -> FoodDetailActivity.open(this, item));
            featuredList.addView(card);
        }
        if (foods.isEmpty()) featuredList.addView(Ui.body(this,
                "No foods are published yet. The administrator can create starter content."));
        Button all = Ui.outlineButton(this, "See all foods");
        all.setOnClickListener(v -> open(FoodCatalogActivity.class));
        featuredList.addView(all);
    }

    private void renderVendors(Map<String, List<FoodItem>> vendors) {
        vendorList.removeAllViews();
        int shown = 0;
        for (Map.Entry<String, List<FoodItem>> entry : vendors.entrySet()) {
            if (shown++ >= 4 || entry.getValue().isEmpty()) break;
            List<FoodItem> menu = entry.getValue();
            FoodItem first = menu.get(0);
            double totalRating = 0;
            for (FoodItem item : menu) totalRating += item.rating > 0 ? item.rating : 4.7;
            double rating = totalRating / menu.size();
            LinearLayout card = Ui.card(this);
            card.addView(Ui.label(this, "VERIFIED LOCAL SELLER"));
            card.addView(Ui.title(this, safe(first.vendorName, "FoodMoboChain Kitchen")));
            card.addView(Ui.body(this, "★ " + String.format(Locale.US, "%.1f", rating)
                    + "  •  " + menu.size() + " dishes  •  Order protected by Firebase rules"));
            Button openStore = Ui.button(this, "View full menu");
            openStore.setOnClickListener(v -> VendorStoreActivity.open(this,
                    entry.getKey(), first.vendorName));
            card.addView(openStore);
            vendorList.addView(card);
        }
        if (vendors.isEmpty()) vendorList.addView(Ui.body(this, "No active sellers yet."));
    }

    private void listenLatestOrder() {
        String uid = firebase.uid();
        if (uid == null) return;
        firebase.orders().orderByChild("buyerId").equalTo(uid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        FoodOrder latest = null;
                        for (DataSnapshot child : snapshot.getChildren()) {
                            FoodOrder order = child.getValue(FoodOrder.class);
                            if (order == null || "delivered".equals(order.status)
                                    || "cancelled".equals(order.status)) continue;
                            if (latest == null || order.updatedAt > latest.updatedAt) latest = order;
                        }
                        activeOrderContainer.removeAllViews();
                        if (latest == null) {
                            LinearLayout empty = Ui.card(MainActivity.this);
                            empty.addView(Ui.title(MainActivity.this, "No active delivery"));
                            empty.addView(Ui.body(MainActivity.this,
                                    "Your newest live order will appear here."));
                            activeOrderContainer.addView(empty);
                            return;
                        }
                        final FoodOrder selected = latest;
                        LinearLayout card = Ui.softCard(MainActivity.this);
                        card.addView(Ui.label(MainActivity.this,
                                selected.status == null ? "PLACED" : selected.status.replace('_', ' ')));
                        card.addView(Ui.title(MainActivity.this,
                                "Order " + shortId(selected.id) + "  •  " + Ui.money(selected.computedTotal())));
                        card.addView(Ui.body(MainActivity.this, statusMessage(selected.status)));
                        Button track = Ui.button(MainActivity.this, "Track order");
                        track.setOnClickListener(v -> open(OrdersActivity.class));
                        card.addView(track);
                        activeOrderContainer.addView(card);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        activeOrderContainer.removeAllViews();
                        activeOrderContainer.addView(Ui.body(MainActivity.this, error.getMessage()));
                    }
                });
    }

    private void addRoleWorkspace(AppUser user) {
        content.addView(Ui.spacer(this, 18));
        content.addView(Ui.heading(this, "Business workspace"));
        if ("vendor".equals(user.role)) {
            if ("approved".equals(user.status) || "active".equals(user.status)) {
                addBusinessAction("Manage my menu", "Publish images, prices, preparation times and availability.",
                        VendorMenuActivity.class);
                addBusinessAction("Customer orders", "Accept and progress orders through delivery.",
                        OrdersActivity.class);
            } else {
                LinearLayout pending = Ui.softCard(this);
                pending.addView(Ui.title(this, "Vendor approval pending"));
                pending.addView(Ui.body(this,
                        "The administrator must approve your application before you can publish food."));
                content.addView(pending);
            }
        }
        if (firebase.isAdminUser() || "admin".equals(user.role)) {
            addBusinessAction("Admin operations", "Approve vendors, moderate reports and seed marketplace content.",
                    AdminActivity.class);
        }
        addBusinessAction("My profile", "Manage contact, business and location details.", ProfileActivity.class);
        Button signOut = Ui.outlineButton(this, "Sign out");
        signOut.setOnClickListener(v -> signOut());
        content.addView(signOut);
    }

    private void addBusinessAction(String title, String description, Class<?> destination) {
        LinearLayout card = Ui.card(this);
        card.addView(Ui.title(this, title));
        card.addView(Ui.body(this, description));
        card.addView(Ui.label(this, "OPEN  ›"));
        card.setOnClickListener(v -> open(destination));
        content.addView(card);
    }

    private String statusMessage(String status) {
        if ("accepted".equals(status)) return "The seller accepted your order.";
        if ("preparing".equals(status)) return "Your meal is being prepared.";
        if ("out_for_delivery".equals(status)) return "The order is on the way to you.";
        return "Your order was placed and is waiting for seller confirmation.";
    }

    private String shortId(String id) {
        if (id == null) return "";
        return id.length() <= 7 ? id : id.substring(id.length() - 7);
    }

    private String capitalise(String value) {
        String safe = safe(value, "user");
        return safe.substring(0, 1).toUpperCase(Locale.ROOT) + safe.substring(1);
    }

    private String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private void signOut() {
        firebase.auth.signOut();
        Intent intent = new Intent(this, AuthActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private static final class Shortcut {
        final String title;
        final String subtitle;
        final Class<?> destination;

        Shortcut(String title, String subtitle, Class<?> destination) {
            this.title = title;
            this.subtitle = subtitle;
            this.destination = destination;
        }
    }
}
