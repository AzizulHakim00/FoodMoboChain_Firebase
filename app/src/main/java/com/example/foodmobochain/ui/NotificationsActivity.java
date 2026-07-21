package com.example.foodmobochain.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.example.foodmobochain.model.FoodOrder;
import com.example.foodmobochain.model.NewsPost;
import com.example.foodmobochain.util.Ui;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class NotificationsActivity extends BaseScreenActivity {
    private LinearLayout orderUpdates;
    private LinearLayout announcements;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupScreen("Notifications", "Orders, offers and community updates", true);
        buildSections();
        listenOrders();
        listenAnnouncements();
    }

    private void buildSections() {
        LinearLayout intro = Ui.softCard(this);
        intro.addView(Ui.label(this, "LIVE ACTIVITY"));
        intro.addView(Ui.heading(this, "Everything important, in one feed."));
        intro.addView(Ui.body(this,
                "Order progress is read directly from Firebase, while community announcements come from the moderated newsfeed."));
        content.addView(intro);
        content.addView(Ui.heading(this, "Order updates"));
        orderUpdates = new LinearLayout(this);
        orderUpdates.setOrientation(LinearLayout.VERTICAL);
        content.addView(orderUpdates);
        content.addView(Ui.spacer(this, 18));
        content.addView(Ui.heading(this, "Announcements"));
        announcements = new LinearLayout(this);
        announcements.setOrientation(LinearLayout.VERTICAL);
        content.addView(announcements);
    }

    private void listenOrders() {
        String uid = firebase.uid();
        if (uid == null) return;
        firebase.orders().orderByChild("buyerId").equalTo(uid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<FoodOrder> orders = new ArrayList<>();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            FoodOrder order = child.getValue(FoodOrder.class);
                            if (order != null) orders.add(order);
                        }
                        orders.sort(Comparator.comparingLong((FoodOrder value) -> value.updatedAt).reversed());
                        orderUpdates.removeAllViews();
                        int shown = 0;
                        for (FoodOrder order : orders) {
                            if (shown++ >= 6) break;
                            LinearLayout card = Ui.card(NotificationsActivity.this);
                            card.addView(Ui.label(NotificationsActivity.this, displayStatus(order.status)));
                            card.addView(Ui.title(NotificationsActivity.this,
                                    "Order " + shortId(order.id) + " • " + Ui.money(order.computedTotal())));
                            card.addView(Ui.body(NotificationsActivity.this,
                                    statusMessage(order.status) + "\nUpdated " + DateFormat.getDateTimeInstance(
                                            DateFormat.MEDIUM, DateFormat.SHORT).format(new Date(order.updatedAt))));
                            Button open = Ui.outlineButton(NotificationsActivity.this, "Open order tracking");
                            open.setOnClickListener(v -> open(OrdersActivity.class));
                            card.addView(open);
                            orderUpdates.addView(card);
                        }
                        if (orders.isEmpty()) orderUpdates.addView(Ui.body(NotificationsActivity.this,
                                "Place an order to receive live progress updates here."));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        orderUpdates.removeAllViews();
                        orderUpdates.addView(Ui.body(NotificationsActivity.this, error.getMessage()));
                    }
                });
    }

    private void listenAnnouncements() {
        firebase.newsfeed().limitToLast(6).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<NewsPost> posts = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    NewsPost post = child.getValue(NewsPost.class);
                    if (post != null) posts.add(post);
                }
                posts.sort(Comparator.comparingLong((NewsPost value) -> value.createdAt).reversed());
                announcements.removeAllViews();
                for (NewsPost post : posts) {
                    LinearLayout card = Ui.card(NotificationsActivity.this);
                    card.addView(Ui.label(NotificationsActivity.this,
                            post.authorRole == null ? "COMMUNITY" : post.authorRole));
                    card.addView(Ui.title(NotificationsActivity.this,
                            post.authorName == null ? "FoodMoboChain" : post.authorName));
                    card.addView(Ui.body(NotificationsActivity.this, post.content));
                    announcements.addView(card);
                }
                if (posts.isEmpty()) announcements.addView(Ui.body(NotificationsActivity.this,
                        "No announcements have been published yet."));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                announcements.removeAllViews();
                announcements.addView(Ui.body(NotificationsActivity.this, error.getMessage()));
            }
        });
    }

    private String displayStatus(String status) {
        return status == null ? "PLACED" : status.replace('_', ' ');
    }

    private String statusMessage(String status) {
        if ("accepted".equals(status)) return "The vendor accepted your order.";
        if ("preparing".equals(status)) return "Your food is being prepared.";
        if ("out_for_delivery".equals(status)) return "Your order is on the way.";
        if ("delivered".equals(status)) return "Delivered. You can now leave a review.";
        if ("cancelled".equals(status)) return "This order was cancelled.";
        return "Your order was placed successfully.";
    }

    private String shortId(String id) {
        if (id == null) return "";
        return id.length() <= 7 ? id : id.substring(id.length() - 7);
    }
}
