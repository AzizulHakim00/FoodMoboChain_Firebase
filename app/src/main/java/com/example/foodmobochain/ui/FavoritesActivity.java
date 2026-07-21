package com.example.foodmobochain.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.foodmobochain.R;
import com.example.foodmobochain.model.FoodItem;
import com.example.foodmobochain.util.FavoritesStore;
import com.example.foodmobochain.util.ImageLoader;
import com.example.foodmobochain.util.Ui;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class FavoritesActivity extends BaseScreenActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupScreen("Favourites", "Your saved dishes in one place", true);
        loadFoods();
    }

    private void loadFoods() {
        content.removeAllViews();
        content.addView(Ui.body(this, "Loading saved dishes…"));
        firebase.foods().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Set<String> ids = FavoritesStore.ids(FavoritesActivity.this);
                List<FoodItem> foods = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    FoodItem item = child.getValue(FoodItem.class);
                    if (item == null || !item.available) continue;
                    if (item.id == null) item.id = child.getKey();
                    if (ids.contains(item.id)) foods.add(item);
                }
                foods.sort(Comparator.comparingDouble((FoodItem item) -> item.rating).reversed());
                render(foods);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                content.removeAllViews();
                content.addView(Ui.body(FavoritesActivity.this, error.getMessage()));
            }
        });
    }

    private void render(List<FoodItem> foods) {
        content.removeAllViews();
        LinearLayout intro = Ui.softCard(this);
        intro.addView(Ui.label(this, "SAVED FOR LATER"));
        intro.addView(Ui.heading(this, foods.size() + (foods.size() == 1 ? " favourite dish" : " favourite dishes")));
        intro.addView(Ui.body(this, "Tap a dish to view its full menu details and add it to your bag."));
        content.addView(intro);
        if (foods.isEmpty()) {
            LinearLayout empty = Ui.card(this);
            empty.addView(Ui.title(this, "Nothing saved yet"));
            empty.addView(Ui.body(this, "Browse the marketplace and tap the heart on foods you love."));
            Button browse = Ui.button(this, "Explore foods");
            browse.setOnClickListener(v -> open(FoodCatalogActivity.class));
            empty.addView(browse);
            content.addView(empty);
            return;
        }
        for (FoodItem item : foods) content.addView(foodCard(item));
    }

    private LinearLayout foodCard(FoodItem item) {
        LinearLayout card = Ui.card(this);
        ImageView image = new ImageView(this);
        image.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 160)));
        image.setContentDescription(item.name + " image");
        ImageLoader.load(image, item.imageUrl, R.drawable.ic_food_plate);
        card.addView(image);
        card.addView(Ui.label(this, item.category));
        card.addView(Ui.title(this, item.name));
        card.addView(Ui.body(this, item.vendorName + "  •  ★ "
                + String.format(Locale.US, "%.1f", item.rating > 0 ? item.rating : 4.7)));
        TextView price = Ui.title(this, Ui.money(item.price));
        price.setTextColor(getColor(R.color.brand_orange));
        card.addView(price);
        Button view = Ui.button(this, "View dish");
        view.setOnClickListener(v -> FoodDetailActivity.open(this, item));
        card.addView(view);
        Button remove = Ui.outlineButton(this, "Remove from favourites");
        remove.setOnClickListener(v -> {
            FavoritesStore.toggle(this, item.id);
            loadFoods();
        });
        card.addView(remove);
        return card;
    }
}
