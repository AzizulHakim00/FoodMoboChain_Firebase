package com.example.foodmobochain.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.foodmobochain.R;
import com.example.foodmobochain.model.AppUser;
import com.example.foodmobochain.util.Ui;

import java.util.Locale;

public class MainActivity extends BaseScreenActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupScreen("Food Mobo Chain", "Dhaka's food carts, connected", false);
        showLoading();
        firebase.loadCurrentUser(this::render);
    }

    private void showLoading() {
        content.removeAllViews();
        content.addView(Ui.body(this, "Loading your dashboard…"));
    }

    private void render(AppUser user) {
        content.removeAllViews();
        if (user == null) {
            Ui.toast(this, "Your profile is missing. Please sign in again.");
            signOut();
            return;
        }

        LinearLayout hero = Ui.softCard(this);
        hero.addView(Ui.label(this, "FRESH NEAR YOU"));
        ImageView foodIllustration = new ImageView(this);
        foodIllustration.setImageResource(R.drawable.ic_food_plate);
        foodIllustration.setContentDescription("Fresh local food illustration");
        foodIllustration.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        foodIllustration.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 132)));
        hero.addView(foodIllustration);
        hero.addView(Ui.heading(this, "Good food is\ncloser than you think."));
        hero.addView(Ui.spacer(this, 8));
        hero.addView(Ui.body(this,
                "Welcome, " + safe(user.name, "food explorer") + ". Discover trusted local carts, learn, sell and grow."));
        Button findFood = Ui.button(this, "Find food");
        findFood.setOnClickListener(v -> open(FoodCatalogActivity.class));
        hero.addView(findFood);
        content.addView(hero);

        LinearLayout account = Ui.card(this);
        account.addView(Ui.label(this, user.role + " account"));
        account.addView(Ui.title(this, safe(user.name, user.email)));
        account.addView(Ui.body(this, statusLine(user)));
        content.addView(account);

        content.addView(Ui.spacer(this, 12));
        content.addView(Ui.heading(this, "What do you want to do?"));
        addAction("Browse foods", "Explore local menus by category and add items to your bag.", FoodCatalogActivity.class);
        addAction("My bag", "Review selected food and place an online order.", BagActivity.class);
        addAction("Orders & reviews", "Track your orders and leave ratings after a transaction.", OrdersActivity.class);
        addAction("Rent a food cart", "Choose an available cart, dates, location and delivery.", RentalActivity.class);
        addAction("Free training", "Watch food safety and entrepreneurship tutorials.", TrainingActivity.class);
        addAction("Community newsfeed", "Read promotions, announcements and business tips.", NewsfeedActivity.class);

        if ("vendor".equals(user.role)) {
            if ("approved".equals(user.status) || "active".equals(user.status)) {
                addAction("Manage my menu", "Add food, change prices and update availability.", VendorMenuActivity.class);
            } else {
                LinearLayout pending = Ui.softCard(this);
                pending.addView(Ui.title(this, "Vendor approval pending"));
                pending.addView(Ui.body(this, "The admin must approve your vendor application before you can publish food."));
                content.addView(pending);
            }
        }
        if ("admin".equals(user.role) || firebase.isAdminEmail(user.email)) {
            addAction("Admin control", "Approve vendors, moderate flags and create starter content.", AdminActivity.class);
        }

        addAction("My profile", "Update personal, business and document information.", ProfileActivity.class);
        Button signOut = Ui.outlineButton(this, "Sign out");
        signOut.setOnClickListener(v -> signOut());
        content.addView(signOut);
    }

    private void addAction(String title, String description, Class<?> destination) {
        LinearLayout card = Ui.card(this);
        card.setClickable(true);
        card.setFocusable(true);
        TextView titleView = Ui.title(this, title);
        card.addView(titleView);
        card.addView(Ui.spacer(this, 4));
        card.addView(Ui.body(this, description));
        TextView open = Ui.label(this, "OPEN  ›");
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = Ui.dp(this, 12);
        open.setLayoutParams(params);
        card.addView(open);
        card.setOnClickListener(v -> open(destination));
        content.addView(card);
    }

    private String statusLine(AppUser user) {
        String status = safe(user.status, "active").replace('_', ' ');
        return "Status: " + status.substring(0, 1).toUpperCase(Locale.ROOT) + status.substring(1)
                + (user.rating > 0 ? "  •  Rating " + String.format(Locale.US, "%.1f", user.rating) : "");
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
}
