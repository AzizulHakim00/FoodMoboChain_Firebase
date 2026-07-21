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
        content.addView(Ui.heading(this, "Popular local stores"));
        vendorList = new LinearLayout(this);
        vendorList.setOrientation(LinearLayout.VERTICAL);
        vendorList.addView(Ui.body(this, "Loading verified stores…"));
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
        header.addView(Ui.title(this, safe(user.location, "Choose a saved delivery address")));
        header.addView(Ui.body(this, "Hello " + safe(user.name, "food explorer")
                + "  •  " + (firebase.isAdminUser() ? "Administrator" : capitalise(user.role))));
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button addresses = compactAction("Addresses");
        addresses.setOnClickListener(v -> open(AddressBookActivity.class));
        Button notifications = compactAction("Notifications");
        notifications.setOnClickListener(v -> open(NotificationsActivity.class));
        Button profile = compactAction("Profile");
        profile.setOnClickListener(v -> open(ProfileActivity.class));
        actions.addView(addresses);
        actions.addView(notifications);
        actions.addView(profile);
        header.addView(actions);
        content.addView(header);
    }

    private void addSearchAndPromotion() {
        Button search = Ui.outlineButton(this, "⌕  Search foods, stores or categories");
        search.setOnClickListener(v -> open(FoodCatalogActivity.class));
        content.addView(search);

        LinearLayout hero = Ui.softCard(this);
        hero.addView(Ui.label(this, "V1.3 LARGE MARKETPLACE"));
        ImageView illustration = new ImageView(this);
        illustration.setContentDescription("Food marketplace promotion");
        illustration.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 165)));
        ImageLoader.load(illustration,
                "https://images.unsplash.com/photo-1504674900247-0877df9cc836?auto=format&fit=crop&w=1400&q=82",
                R.drawable.ic_food_plate);
        hero.addView(illustration);
        hero.addView(Ui.heading(this, "More stores. More foods.\nOne protected marketplace."));
        hero.addView(Ui.body(this,
                "Explore store pages, live offers, saved addresses, support tickets, rentals, training and secure Firebase ordering."));
        Button explore = Ui.button(this, "Explore all stores");
        explore.setOnClickListener(v -> open(StoresActivity.class));
        hero.addView(explore);
        Button offers = Ui.outlineButton(this, "View live offers");
        offers.setOnClickListener(v -> open(OffersActivity.class));
        hero.addView(offers);
        content.addView(hero);
    }

    private void addServiceShortcuts() {
        content.addView(Ui.heading(this, "Marketplace services"));
        addShortcutRow(
                new Shortcut("All stores", "Restaurants and carts", StoresActivity.class),
                new Shortcut("Browse foods", "Search the full catalogue", FoodCatalogActivity.class));
        addShortcutRow(
                new Shortcut("Offers", "Live discounted prices", OffersActivity.class),
                new Shortcut("My bag", "Checkout securely", BagActivity.class));
        addShortcutRow(
                new Shortcut("Orders", "Live delivery progress", OrdersActivity.class),
                new Shortcut("Favourites", "Saved dishes", FavoritesActivity.class));
        addShortcutRow(
                new Shortcut("Saved addresses", "Home, work and campus", AddressBookActivity.class),
                new Shortcut("Help & support", "Track support tickets", SupportActivity.class));
        addShortcutRow(
                new Shortcut("Rent a cart", "Start a business", RentalActivity.class),
                new Shortcut("Free training", "Learn and grow", TrainingActivity.class));
        addShortcutRow(
                new Shortcut("Newsfeed", "Tips and announcements", NewsfeedActivity.class),
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
        for (String category : new String[]{"Biryani", "Street food", "Burgers", "Dessert"}) {
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
        button.setTextSize(11);
        return button;
    }

    private void loadMarketplaceData() {
        firebase.foods().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<FoodItem> foods = new ArrayList<>();
                Map<String, List<FoodItem>> stores = new LinkedHashMap<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    FoodItem item = child.getValue(FoodItem.class);
                    if (item == null || !item.inStock()) continue;
                    if (item.id == null) item.id = child.getKey();
                    foods.add(item);
                    String groupId = item.storeId == null || item.storeId.trim().isEmpty()
                            ? item.vendorId : item.storeId;
                    if (groupId != null) stores.computeIfAbsent(groupId,
                            ignored -> new ArrayList<>()).add(item);
                }
                foods.sort(Comparator
                        .comparing((FoodItem item) -> !item.featured)
                        .thenComparing(Comparator.comparingDouble((FoodItem item) -> item.rating).reversed()));
                renderFeatured(foods);
                renderStores(stores);
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
            if (shown++ >= 6) break;
            LinearLayout card = Ui.card(this);
            ImageView image = new ImageView(this);
            image.setContentDescription(item.name + " image");
            image.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 175)));
            ImageLoader.load(image, item.imageUrl, R.drawable.ic_food_plate);
            card.addView(image);
            String badge = item.featured ? "FEATURED" : item.category;
            if (item.discountPercent > 0) badge += "  •  "
                    + String.format(Locale.US, "%.0f%% OFF", item.discountPercent);
            card.addView(Ui.label(this, badge));
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
                "No foods are published yet. The administrator can create the large starter marketplace."));
        Button all = Ui.outlineButton(this, "See the complete food catalogue");
        all.setOnClickListener(v -> open(FoodCatalogActivity.class));
        featuredList.addView(all);
    }

    private void renderStores(Map<String, List<FoodItem>> stores) {
        vendorList.removeAllViews();
        int shown = 0;
        for (Map.Entry<String, List<FoodItem>> entry : stores.entrySet()) {
            if (shown++ >= 6 || entry.getValue().isEmpty()) break;
            List<FoodItem> menu = entry.getValue();
            FoodItem first = menu.get(0);
            double totalRating = 0;
            for (FoodItem item : menu) totalRating += item.rating > 0 ? item.rating : 4.7;
            double rating = totalRating / menu.size();
            LinearLayout card = Ui.card(this);
            card.addView(Ui.label(this, "VERIFIED MARKETPLACE STORE"));
            card.addView(Ui.title(this, safe(first.vendorName, "FoodMoboChain Store")));
            card.addView(Ui.body(this, "★ " + String.format(Locale.US, "%.1f", rating)
                    + "  •  " + menu.size() + " available dishes  •  Protected checkout"));
            Button openStore = Ui.button(this, "View full store menu");
            String storeId = first.storeId == null || first.storeId.trim().isEmpty()
                    ? first.vendorId : first.storeId;
            openStore.setOnClickListener(v -> VendorStoreActivity.openStore(this,
                    storeId, first.vendorName));
            card.addView(openStore);
            vendorList.addView(card);
        }
        if (stores.isEmpty()) vendorList.addView(Ui.body(this, "No active stores yet."));
        Button allStores = Ui.outlineButton(this, "Browse all stores");
        allStores.setOnClickListener(v -> open(StoresActivity.class));
        vendorList.addView(allStores);
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
                                safe(selected.storeName, "Store") + "  •  Order " + shortId(selected.id)));
                        card.addView(Ui.body(MainActivity.this,
                                Ui.money(selected.computedTotal()) + "\n" + statusMessage(selected.status)));
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
                addBusinessAction("Seller centre", "Manage storefront, foods, images, stock and availability.",
                        VendorMenuActivity.class);
                addBusinessAction("Customer orders", "Accept, prepare, pack and progress deliveries.",
                        OrdersActivity.class);
                addBusinessAction("Seller analytics", "Review order volume and delivered sales value.",
                        AnalyticsActivity.class);
            } else {
                LinearLayout pending = Ui.softCard(this);
                pending.addView(Ui.title(this, "Vendor approval pending"));
                pending.addView(Ui.body(this,
                        "The administrator must approve your application before you can publish food."));
                content.addView(pending);
            }
        }
        if (firebase.isAdminUser() || "admin".equals(user.role)) {
            addBusinessAction("Admin operations", "Approve vendors, seed large data and moderate the marketplace.",
                    AdminActivity.class);
            addBusinessAction("Marketplace analytics", "Monitor users, stores, foods, orders, rentals and support.",
                    AnalyticsActivity.class);
        }
        addBusinessAction("My profile", "Manage contact, business and location details.", ProfileActivity.class);
        addBusinessAction("Help & support", "Create and track support conversations.", SupportActivity.class);
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
        if ("packed".equals(status)) return "Your order is packed and waiting for dispatch.";
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
