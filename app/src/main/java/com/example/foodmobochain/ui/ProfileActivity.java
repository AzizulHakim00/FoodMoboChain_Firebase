package com.example.foodmobochain.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.foodmobochain.model.AppUser;
import com.example.foodmobochain.model.Review;
import com.example.foodmobochain.util.Ui;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ProfileActivity extends BaseScreenActivity {
    private AppUser user;
    private EditText name;
    private EditText phone;
    private EditText business;
    private EditText location;
    private EditText documentUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupScreen("My profile", "Personal, business and verification details", true);
        firebase.loadCurrentUser(this::render);
    }

    private void render(AppUser loaded) {
        user = loaded;
        content.removeAllViews();
        if (user == null) {
            content.addView(Ui.body(this, "Profile information could not be loaded."));
            return;
        }
        LinearLayout summary = Ui.softCard(this);
        summary.addView(Ui.label(this, user.role + " • " + user.status));
        summary.addView(Ui.heading(this, user.name));
        summary.addView(Ui.body(this, user.email));
        TextView ratingView = Ui.body(this, "Loading community rating…");
        summary.addView(ratingView);
        content.addView(summary);
        loadRating(ratingView);

        LinearLayout form = Ui.card(this);
        name = Ui.input(this, "Full name");
        phone = Ui.input(this, "Phone number");
        business = Ui.input(this, "Business or cart name");
        location = Ui.input(this, "Location");
        documentUrl = Ui.input(this, "Public document URL (optional)");
        name.setText(value(user.name));
        phone.setText(value(user.phone));
        business.setText(value(user.businessName));
        location.setText(value(user.location));
        documentUrl.setText(value(user.documentUrl));
        Button save = Ui.button(this, "Save profile");
        save.setOnClickListener(v -> saveProfile());
        form.addView(name);
        form.addView(phone);
        form.addView(business);
        form.addView(location);
        form.addView(documentUrl);
        form.addView(Ui.body(this,
                "Spark Edition stores a public HTTPS link instead of uploading private files to Firebase Storage."));
        form.addView(save);
        content.addView(form);

        if (!TextUtils.isEmpty(user.documentUrl)) {
            Button view = Ui.outlineButton(this, "Open document link");
            view.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(user.documentUrl))));
            content.addView(view);
        }
    }

    private void loadRating(TextView target) {
        firebase.reviews().orderByChild("targetUserId").equalTo(user.uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        int count = 0;
                        int total = 0;
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Review review = child.getValue(Review.class);
                            if (review == null || review.stars < 1 || review.stars > 5) continue;
                            total += review.stars;
                            count++;
                        }
                        target.setText(count == 0 ? "No completed-order ratings yet"
                                : "Community rating: " + String.format(Locale.US, "%.1f", (double) total / count)
                                + " from " + count + (count == 1 ? " review" : " reviews"));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        target.setText("Community rating unavailable");
                    }
                });
    }

    private void saveProfile() {
        String uid = firebase.uid();
        if (uid == null || user == null) return;
        String nameValue = name.getText().toString().trim();
        String link = documentUrl.getText().toString().trim();
        if (TextUtils.isEmpty(nameValue)) {
            name.setError("Name is required");
            return;
        }
        if (!TextUtils.isEmpty(link) && !isWebUrl(link)) {
            documentUrl.setError("Use a public http:// or https:// link");
            return;
        }
        Map<String, Object> update = new HashMap<>();
        update.put("name", nameValue);
        update.put("phone", phone.getText().toString().trim());
        update.put("businessName", business.getText().toString().trim());
        update.put("location", location.getText().toString().trim());
        update.put("documentUrl", link);
        update.put("updatedAt", System.currentTimeMillis());
        saveUpdate(uid, update);
    }

    private void saveUpdate(String uid, Map<String, Object> update) {
        firebase.users().child(uid).updateChildren(update).addOnCompleteListener(task -> {
            Ui.toast(this, task.isSuccessful() ? "Profile updated." : "Profile update failed.");
            if (task.isSuccessful()) firebase.loadCurrentUser(this::render);
        });
    }

    private boolean isWebUrl(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        return lower.startsWith("https://") || lower.startsWith("http://");
    }

    private String value(String value) {
        return value == null ? "" : value;
    }
}
