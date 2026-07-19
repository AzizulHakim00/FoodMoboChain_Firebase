package com.example.foodmobochain.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.foodmobochain.model.CartLine;
import com.example.foodmobochain.model.FoodItem;
import com.example.foodmobochain.util.Ui;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class FoodCatalogActivity extends BaseScreenActivity {
    private final List<FoodItem> allFoods = new ArrayList<>();
    private LinearLayout results;
    private EditText search;
    private Spinner category;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupScreen("Foods", "Fresh dishes from trusted local carts", true);
        buildFilters();
        listenForFoods();
    }

    private void buildFilters() {
        LinearLayout filters = Ui.softCard(this);
        filters.addView(Ui.label(this, "FIND YOUR CRAVING"));
        search = Ui.input(this, "Search kacchi, fuchka, momo…");
        filters.addView(search);
        category = new Spinner(this);
        List<String> categories = Arrays.asList("All", "Rice", "Drinks", "Grill", "Traditional",
                "Street Food", "Dessert", "Snacks", "Noodles", "Fast Food");
        category.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, categories));
        LinearLayout.LayoutParams spinnerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 54));
        spinnerParams.topMargin = Ui.dp(this, 10);
        category.setLayoutParams(spinnerParams);
        category.setBackgroundResource(com.example.foodmobochain.R.drawable.bg_input);
        category.setPadding(Ui.dp(this, 12), 0, Ui.dp(this, 12), 0);
        filters.addView(category);
        Button apply = Ui.button(this, "Apply filters");
        apply.setOnClickListener(v -> renderFoods());
        filters.addView(apply);
        content.addView(filters);

        Button bag = Ui.outlineButton(this, "View my bag");
        bag.setOnClickListener(v -> open(BagActivity.class));
        content.addView(bag);
        results = new LinearLayout(this);
        results.setOrientation(LinearLayout.VERTICAL);
        content.addView(results);

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { renderFoods(); }
            @Override public void afterTextChanged(Editable s) { }
        });
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
        if (results == null) return;
        results.removeAllViews();
        String query = search.getText().toString().trim().toLowerCase(Locale.ROOT);
        String selected = String.valueOf(category.getSelectedItem());
        int count = 0;
        for (FoodItem item : allFoods) {
            if (!item.available) continue;
            String searchable = safe(item.name) + " " + safe(item.description) + " " + safe(item.vendorName);
            if (!query.isEmpty() && !searchable.toLowerCase(Locale.ROOT).contains(query)) continue;
            if (!"All".equals(selected) && !selected.equalsIgnoreCase(item.category)) continue;
            results.addView(foodCard(item));
            count++;
        }
        TextView summary = Ui.body(this, count + " dishes found");
        results.addView(summary, 0);
        if (count == 0) {
            LinearLayout empty = Ui.card(this);
            empty.addView(Ui.title(this, "No dishes found"));
            empty.addView(Ui.body(this, "Try another category, or ask an approved vendor to publish menu items."));
            results.addView(empty);
        }
    }

    private View foodCard(FoodItem item) {
        LinearLayout card = Ui.card(this);
        card.addView(Ui.label(this, safe(item.category).isEmpty() ? "LOCAL FOOD" : item.category));
        card.addView(Ui.title(this, safe(item.name)));
        card.addView(Ui.body(this, safe(item.description)));
        card.addView(Ui.spacer(this, 8));
        String details = Ui.money(item.price) + "  •  " + safe(item.vendorName)
                + (item.rating > 0 ? "  •  ★ " + String.format(Locale.US, "%.1f", item.rating) : "");
        TextView price = Ui.title(this, details);
        price.setTextColor(getColor(com.example.foodmobochain.R.color.brand_green));
        card.addView(price);
        Button add = Ui.button(this, "Add to bag");
        add.setOnClickListener(v -> addToBag(item));
        card.addView(add);
        return card;
    }

    private void addToBag(FoodItem item) {
        String uid = firebase.uid();
        if (uid == null || item.id == null) return;
        firebase.carts().child(uid).child(item.id).runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                CartLine line = currentData.getValue(CartLine.class);
                if (line == null) line = new CartLine(item, 0);
                line.quantity += 1;
                currentData.setValue(line);
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed, DataSnapshot snapshot) {
                Ui.toast(FoodCatalogActivity.this,
                        committed ? item.name + " added to your bag." : "Could not update your bag.");
            }
        });
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
