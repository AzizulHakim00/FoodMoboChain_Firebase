package com.example.foodmobochain.ui;

import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.example.foodmobochain.model.FoodOrder;
import com.example.foodmobochain.util.Ui;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.Locale;

public class AnalyticsActivity extends BaseScreenActivity {
    private LinearLayout summary;
    private long users;
    private long stores;
    private long foods;
    private long orders;
    private long tickets;
    private long rentals;
    private long delivered;
    private double revenue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupScreen("Marketplace analytics", "Live operational summary from Firebase", true);
        summary = new LinearLayout(this);
        summary.setOrientation(LinearLayout.VERTICAL);
        content.addView(summary);
        load();
    }

    private void load() {
        if (firebase.isAdminUser()) {
            count(firebase.users(), value -> users = value);
            count(firebase.stores(), value -> stores = value);
            count(firebase.foods(), value -> foods = value);
            count(firebase.supportTickets(), value -> tickets = value);
            count(firebase.rentalBookings(), value -> rentals = value);
            firebase.orders().addValueEventListener(orderListener(false));
        } else {
            String uid = firebase.uid();
            if (uid == null) return;
            firebase.foods().orderByChild("vendorId").equalTo(uid)
                    .addValueEventListener(countListener(value -> foods = value));
            firebase.orders().orderByChild("vendorId").equalTo(uid)
                    .addValueEventListener(orderListener(true));
            stores = 1;
            users = 0;
            tickets = 0;
            rentals = 0;
        }
        render();
    }

    private interface Counter { void set(long value); }

    private void count(com.google.firebase.database.DatabaseReference reference, Counter counter) {
        reference.addValueEventListener(countListener(counter));
    }

    private ValueEventListener countListener(Counter counter) {
        return new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                counter.set(snapshot.getChildrenCount());
                render();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Ui.toast(AnalyticsActivity.this, error.getMessage());
            }
        };
    }

    private ValueEventListener orderListener(boolean vendorOnly) {
        return new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                orders = 0;
                delivered = 0;
                revenue = 0;
                for (DataSnapshot child : snapshot.getChildren()) {
                    FoodOrder order = child.getValue(FoodOrder.class);
                    if (order == null) continue;
                    orders++;
                    if ("delivered".equals(order.status)) {
                        delivered++;
                        revenue += order.computedTotal();
                    }
                }
                render();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Ui.toast(AnalyticsActivity.this, error.getMessage());
            }
        };
    }

    private void render() {
        if (summary == null) return;
        summary.removeAllViews();
        LinearLayout hero = Ui.softCard(this);
        hero.addView(Ui.label(this, firebase.isAdminUser() ? "ADMIN OVERVIEW" : "SELLER OVERVIEW"));
        hero.addView(Ui.heading(this, Ui.money(revenue) + " delivered order value"));
        double completion = orders == 0 ? 0 : (delivered * 100d / orders);
        hero.addView(Ui.body(this, delivered + " delivered from " + orders + " total orders  •  "
                + String.format(Locale.US, "%.0f%%", completion) + " completion"));
        summary.addView(hero);
        addMetric("Orders", String.valueOf(orders), "All visible marketplace orders");
        addMetric("Delivered", String.valueOf(delivered), "Completed transactions");
        addMetric("Foods", String.valueOf(foods), "Published menu records");
        addMetric("Stores", String.valueOf(stores), "Marketplace storefronts");
        if (firebase.isAdminUser()) {
            addMetric("Users", String.valueOf(users), "Registered profiles");
            addMetric("Rental requests", String.valueOf(rentals), "Food-cart bookings");
            addMetric("Support tickets", String.valueOf(tickets), "Customer help conversations");
        }
    }

    private void addMetric(String title, String value, String detail) {
        LinearLayout card = Ui.card(this);
        card.addView(Ui.label(this, title.toUpperCase(Locale.ROOT)));
        card.addView(Ui.heading(this, value));
        card.addView(Ui.body(this, detail));
        summary.addView(card);
    }
}
