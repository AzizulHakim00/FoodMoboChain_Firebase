package com.example.foodmobochain.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
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
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class FoodCatalogActivity extends BaseScreenActivity {
    private final List<FoodItem> allFoods = new ArrayList<>();
    private LinearLayout results;
    private EditText search;
    private Spinner category;
    private Spinner sort;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupScreen("Discover food", "Local favourites, delivered from trusted carts", true);
        buildFilters();
        listenForFoods();
    }

    private void buildFilters() {
        LinearLayout promotion = Ui.softCard(this);
        promotion.addView(Ui.label(this, "FOODMOBILE MARKETPLACE"));
        promotion.addView(Ui.heading(this, "What are you craving today?"));
        promotion.addView(Ui.body(this,
                "Search popular Bangladeshi street food, meals, drinks and desserts from verified local sellers."));
        content.addView(promotion);

        LinearLayout filters = Ui.card(this);
        search = Ui.input(this, "Search biryani, fuchka, momo, burger…");
        filters.addView(search);

        category = marketplaceSpinner(Arrays.asList("All", "Rice", "Traditional", "Street Food",
                "Fast Food", "Snacks", "Noodles", "Grill", "Drinks", "Dessert"));
        filters.addView(category);

        sort = marketplaceSpinner(Arrays.asList("Recommended", "Top rated", "Price: low to high",
                "Price: high to low", "Fastest preparation"));
        filters.addView(sort);

        Button apply = Ui.button(this, "Show matching dishes");
        apply.setOnClickListener(v -> renderFoods());
        filters.addView(apply);
        content.addView(filters);

        LinearLayout shortcuts = new LinearLayout(this);
        shortcuts.setOrientation(LinearLayout.HORIZONTAL);
        shortcuts.setGravity(Gravity.CENTER);
        Button bag = shortcutButton("Bag");
        bag.setOnClickListener(v -> open(BagActivity.class));
        Button favourites = shortcutButton("Favourites");
        favourites.setOnClickListener(v -> open(FavoritesActivity.class));
        Button orders = shortcutButton("Orders");
        orders.setOnClickListener(v -> open(OrdersActivity.class));
        shortcuts.addView(bag);
        shortcuts.addView(favourites);
        shortcuts.addView(orders);
        content.addView(shortcuts);

        results = new LinearLayout(this);
        results.setOrientation(LinearLayout.VERTICAL);
        content.addView(results);

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { renderFoods(); }
            @Override public void afterTextChanged(Editable s) { }
        });
    }

    private Spinner marketplaceSpinner(List<String> values) {
        Spinner spinner = new Spinner(this);
        spinner.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, values));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 54));
        params.topMargin = Ui.dp(this, 10);
        spinner.setLayoutParams(params);
        spinner.setBackgroundResource(R.drawable.bg_input);
        spinner.setPadding(Ui.dp(this, 12), 0, Ui.dp(this, 12), 0);
        return spinner;
    }

    private Button shortcutButton(String text) {
        Button button = Ui.outlineButton(this, text);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, Ui.dp(this, 48), 1f);
        params.setMargins(Ui.dp(this, 3), Ui.dp(this, 8), Ui.dp(this, 3), 0);
        button.setLayoutParams(params);
        return button;
    }

    private void listenForFoods() {
        firebase.foods().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allFoods.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    FoodItem item = child.getValue(FoodItem.class);
                    if (item != null) {
                        if (item.id == null) item.id = child.getKey();
                        allFoods.add(item);
                    }
                }
                renderFoods();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Ui.toast(FoodCatalogActivity.this, error.getMessage());
            }
        });
    }

    private void renderFoods() {
        if (results == null || search == null || category == null || sort == null) return;
        results.removeAllViews();
        String query = search.getText().toString().trim().toLowerCase(Locale.ROOT);
        String selected = String.valueOf(category.getSelectedItem());
        List<FoodItem> filtered = new ArrayList<>();
        for (FoodItem item : allFoods) {
            if (!item.available) continue;
            String searchable = safe(item.name) + " " + safe(item.description) + " "
                    + safe(item.vendorName) + " " + safe(item.category);
            if (!query.isEmpty() && !searchable.toLowerCase(Locale.ROOT).contains(query)) continue;
            if (!"All".equals(selected) && !selected.equalsIgnoreCase(item.category)) continue;
            filtered.add(item);
        }
        applySort(filtered, String.valueOf(sort.getSelectedItem()));

        LinearLayout summary = Ui.softCard(this);
        summary.addView(Ui.label(this, "AVAILABLE NOW"));
        summary.addView(Ui.title(this, filtered.size() + (filtered.size() == 1 ? " dish found" : " dishes found")));
        summary.addView(Ui.body(this, selected + "  •  " + String.valueOf(sort.getSelectedItem())));
        results.addView(summary);

        for (FoodItem item : filtered) results.addView(foodCard(item));
        if (filtered.isEmpty()) {
            LinearLayout empty = Ui.card(this);
            empty.addView(Ui.title(this, "No dishes found"));
            empty.addView(Ui.body(this, "Try another category or search phrase."));
            results.addView(empty);
        }
    }

    private void applySort(List<FoodItem> foods, String selectedSort) {
        if ("Top rated".equals(selectedSort)) {
            foods.sort(Comparator.comparingDouble((FoodItem value) -> value.rating).reversed());
        } else if ("Price: low to high".equals(selectedSort)) {
            foods.sort(Comparator.comparingDouble(value -> value.price));
        } else if ("Price: high to low".equals(selectedSort)) {
            foods.sort(Comparator.comparingDouble((FoodItem value) -> value.price).reversed());
        } else if ("Fastest preparation".equals(selectedSort)) {
            foods.sort(Comparator.comparingInt(value -> value.preparationMinutes > 0
                    ? value.preparationMinutes : 20));
        } else {
            foods.sort(Comparator
                    .comparing((FoodItem value) -> !value.featured)
                    .thenComparing(Comparator.comparingDouble((FoodItem value) -> value.rating).reversed()));
        }
    }

    private View foodCard(FoodItem item) {
        LinearLayout card = Ui.card(this);
        card.setClickable(true);
        card.setFocusable(true);

        ImageView image = new ImageView(this);
        image.setContentDescription(item.name + " image");
        image.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 190)));
        ImageLoader.load(image, item.imageUrl, R.drawable.ic_food_plate);
        card.addView(image);

        LinearLayout headingRow = new LinearLayout(this);
        headingRow.setOrientation(LinearLayout.HORIZONTAL);
        headingRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        copy.addView(Ui.label(this, safe(item.category).isEmpty() ? "LOCAL FOOD" : item.category));
        copy.addView(Ui.title(this, safe(item.name)));
        copy.addView(Ui.body(this, safe(item.vendorName)));
        headingRow.addView(copy);
        Button heart = Ui.outlineButton(this,
                FavoritesStore.contains(this, item.id) ? "♥" : "♡");
        heart.setTextSize(22);
        heart.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(this, 58), Ui.dp(this, 48)));
        heart.setOnClickListener(v -> {
            boolean saved = FavoritesStore.toggle(this, item.id);
            heart.setText(saved ? "♥" : "♡");
        });
        headingRow.addView(heart);
        card.addView(headingRow);

        card.addView(Ui.body(this, safe(item.description)));
        String detail = "★ " + String.format(Locale.US, "%.1f", item.rating > 0 ? item.rating : 4.7)
                + "  •  " + (item.preparationMinutes > 0 ? item.preparationMinutes : 20)
                + "–" + ((item.preparationMinutes > 0 ? item.preparationMinutes : 20) + 10) + " min"
                + "  •  " + (item.deliveryFee <= 0 ? "Free delivery" : Ui.money(item.deliveryFee) + " delivery");
        card.addView(Ui.body(this, detail));
        TextView price = Ui.heading(this, Ui.money(item.price));
        price.setTextColor(getColor(R.color.brand_orange));
        card.addView(price);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button details = Ui.outlineButton(this, "Details");
        details.setLayoutParams(actionParams());
        details.setOnClickListener(v -> FoodDetailActivity.open(this, item));
        Button add = Ui.button(this, "Add to bag");
        add.setLayoutParams(actionParams());
        add.setOnClickListener(v -> addToBag(item, add));
        actions.addView(details);
        actions.addView(add);
        card.addView(actions);
        card.setOnClickListener(v -> FoodDetailActivity.open(this, item));
        return card;
    }

    private LinearLayout.LayoutParams actionParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, Ui.dp(this, 50), 1f);
        params.setMargins(Ui.dp(this, 3), Ui.dp(this, 8), Ui.dp(this, 3), 0);
        return params;
    }

    private void addToBag(FoodItem item, Button button) {
        String uid = firebase.uid();
        if (uid == null || item.id == null) return;
        button.setEnabled(false);
        firebase.carts().child(uid).child(item.id).runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                CartLine line = currentData.getValue(CartLine.class);
                if (line == null) line = new CartLine(item, 0);
                line.unitPrice = item.price;
                line.quantity = Math.min(20, line.quantity + 1);
                currentData.setValue(line);
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed, DataSnapshot snapshot) {
                button.setEnabled(true);
                Ui.toast(FoodCatalogActivity.this,
                        committed ? item.name + " added to your bag." : "Could not update your bag.");
            }
        });
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
