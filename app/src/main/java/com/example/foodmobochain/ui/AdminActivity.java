package com.example.foodmobochain.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.example.foodmobochain.data.EnterpriseSeedService;
import com.example.foodmobochain.data.SparkOperations;
import com.example.foodmobochain.model.AppUser;
import com.example.foodmobochain.util.Ui;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class AdminActivity extends BaseScreenActivity {
    private AppUser admin;
    private LinearLayout applications;
    private LinearLayout flags;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupScreen("Admin operations", "Marketplace safety, content and seller management", true);
        firebase.loadCurrentUser(user -> {
            admin = user;
            if (user == null || !(firebase.isAdminUser() || "admin".equals(user.role))) {
                content.addView(Ui.body(this,
                        "This screen is restricted to the FoodMoboChain administrator."));
                return;
            }
            buildDashboard();
            listenApplications();
            listenFlags();
        });
    }

    private void buildDashboard() {
        content.removeAllViews();
        LinearLayout adminCard = Ui.softCard(this);
        adminCard.addView(Ui.label(this, "MARKETPLACE CONTROL CENTRE"));
        adminCard.addView(Ui.heading(this, "Operate a safe and useful food ecosystem."));
        adminCard.addView(Ui.body(this,
                "Approve genuine vendors, moderate reports and initialise a complete marketplace with professional imagery and realistic data."));
        Button seed = Ui.button(this, "Create or refresh enterprise starter data");
        seed.setOnClickListener(v -> new MaterialAlertDialogBuilder(this)
                .setTitle("Create enterprise starter data?")
                .setMessage("This safely refreshes only the administrator sample catalogue, rental carts and announcements. Real users and real vendor listings remain unchanged.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Create data", (dialog, which) -> seedData())
                .show());
        adminCard.addView(seed);
        Button marketplace = Ui.outlineButton(this, "Preview customer marketplace");
        marketplace.setOnClickListener(v -> open(FoodCatalogActivity.class));
        adminCard.addView(marketplace);
        content.addView(adminCard);

        content.addView(Ui.spacer(this, 16));
        content.addView(Ui.heading(this, "Vendor applications"));
        applications = new LinearLayout(this);
        applications.setOrientation(LinearLayout.VERTICAL);
        content.addView(applications);

        content.addView(Ui.spacer(this, 18));
        content.addView(Ui.heading(this, "Reported content"));
        flags = new LinearLayout(this);
        flags.setOrientation(LinearLayout.VERTICAL);
        content.addView(flags);
    }

    private void listenApplications() {
        firebase.vendorApplications().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                applications.removeAllViews();
                int count = 0;
                for (DataSnapshot child : snapshot.getChildren()) {
                    AppUser user = child.getValue(AppUser.class);
                    if (user == null || !"pending".equals(user.status)) continue;
                    applications.addView(applicationCard(user));
                    count++;
                }
                if (count == 0) applications.addView(Ui.body(AdminActivity.this,
                        "No pending vendor applications."));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Ui.toast(AdminActivity.this, error.getMessage());
            }
        });
    }

    private LinearLayout applicationCard(AppUser user) {
        LinearLayout card = Ui.card(this);
        card.addView(Ui.label(this, "PENDING VENDOR"));
        card.addView(Ui.title(this, user.name));
        card.addView(Ui.body(this, user.email
                + (user.businessName == null ? "" : "\nBusiness: " + user.businessName)
                + (user.location == null ? "" : "\nLocation: " + user.location)));
        Button approve = Ui.button(this, "Approve vendor");
        approve.setOnClickListener(v -> setVendorStatus(user, "approved"));
        card.addView(approve);
        Button reject = Ui.outlineButton(this, "Reject application");
        reject.setOnClickListener(v -> setVendorStatus(user, "rejected"));
        card.addView(reject);
        return card;
    }

    private void setVendorStatus(AppUser user, String status) {
        SparkOperations.setVendorStatus(firebase, user, status, (result, error) -> Ui.toast(this,
                error == null ? "Vendor status and permissions updated."
                        : "Could not update vendor: " + safeMessage(error)));
    }

    private void listenFlags() {
        firebase.flags().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                flags.removeAllViews();
                int count = 0;
                for (DataSnapshot child : snapshot.getChildren()) {
                    if (!"open".equals(String.valueOf(child.child("status").getValue()))) continue;
                    flags.addView(flagCard(child));
                    count++;
                }
                if (count == 0) flags.addView(Ui.body(AdminActivity.this, "No open reports."));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Ui.toast(AdminActivity.this, error.getMessage());
            }
        });
    }

    private LinearLayout flagCard(DataSnapshot snapshot) {
        String id = snapshot.getKey();
        String contentId = String.valueOf(snapshot.child("contentId").getValue());
        String reason = String.valueOf(snapshot.child("reason").getValue());
        String preview = String.valueOf(snapshot.child("contentPreview").getValue());
        LinearLayout card = Ui.card(this);
        card.addView(Ui.label(this, "REPORTED NEWS POST"));
        card.addView(Ui.title(this, reason));
        card.addView(Ui.body(this, preview));
        Button resolve = Ui.button(this, "Keep post and resolve report");
        resolve.setOnClickListener(v -> firebase.flags().child(id).child("status").setValue("resolved"));
        card.addView(resolve);
        Button remove = Ui.outlineButton(this, "Remove post and resolve");
        remove.setOnClickListener(v -> new MaterialAlertDialogBuilder(this)
                .setTitle("Remove reported post?")
                .setMessage("This action removes the newsfeed post for every user.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Remove", (dialog, which) -> {
                    Map<String, Object> update = new HashMap<>();
                    update.put("newsfeed/" + contentId, null);
                    update.put("flags/" + id + "/status", "removed");
                    firebase.root.updateChildren(update);
                }).show());
        card.addView(remove);
        return card;
    }

    private void seedData() {
        if (admin == null || !firebase.isAdminUser()) return;
        EnterpriseSeedService.seed(firebase, admin, error -> Ui.toast(this,
                error == null ? "Enterprise starter marketplace is ready."
                        : "Could not create starter content: " + safeMessage(error)));
    }

    private String safeMessage(Exception exception) {
        return exception == null ? "Unknown database error." : exception.getLocalizedMessage();
    }
}
