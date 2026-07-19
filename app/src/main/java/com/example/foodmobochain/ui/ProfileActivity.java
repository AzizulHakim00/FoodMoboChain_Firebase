package com.example.foodmobochain.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.example.foodmobochain.model.AppUser;
import com.example.foodmobochain.util.Ui;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends BaseScreenActivity {
    private AppUser user;
    private EditText name;
    private EditText phone;
    private EditText business;
    private EditText location;
    private Button documentButton;
    private Uri selectedDocument;

    private final ActivityResultLauncher<String> documentPicker = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                selectedDocument = uri;
                documentButton.setText(uri == null ? "Choose ID or certificate" : "Document selected ✓");
            });

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
        summary.addView(Ui.body(this, user.rating > 0 ? "Community rating: " + user.rating : "No ratings yet"));
        content.addView(summary);

        LinearLayout form = Ui.card(this);
        name = Ui.input(this, "Full name");
        phone = Ui.input(this, "Phone number");
        business = Ui.input(this, "Business or cart name");
        location = Ui.input(this, "Location");
        name.setText(value(user.name));
        phone.setText(value(user.phone));
        business.setText(value(user.businessName));
        location.setText(value(user.location));
        documentButton = Ui.outlineButton(this,
                TextUtils.isEmpty(user.documentUrl) ? "Choose ID or certificate" : "Replace uploaded document");
        documentButton.setOnClickListener(v -> documentPicker.launch("*/*"));
        Button save = Ui.button(this, "Save profile");
        save.setOnClickListener(v -> saveProfile());
        form.addView(name);
        form.addView(phone);
        form.addView(business);
        form.addView(location);
        form.addView(documentButton);
        form.addView(save);
        content.addView(form);

        if (!TextUtils.isEmpty(user.documentUrl)) {
            Button view = Ui.outlineButton(this, "View uploaded document");
            view.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(user.documentUrl))));
            content.addView(view);
        }
    }

    private void saveProfile() {
        String uid = firebase.uid();
        if (uid == null || user == null) return;
        String nameValue = name.getText().toString().trim();
        if (TextUtils.isEmpty(nameValue)) {
            name.setError("Name is required");
            return;
        }
        Map<String, Object> update = new HashMap<>();
        update.put("name", nameValue);
        update.put("phone", phone.getText().toString().trim());
        update.put("businessName", business.getText().toString().trim());
        update.put("location", location.getText().toString().trim());
        if (selectedDocument == null) {
            saveUpdate(uid, update);
            return;
        }
        documentButton.setEnabled(false);
        StorageReference file = firebase.storage.child("documents").child(uid).child("verification-document");
        file.putFile(selectedDocument).continueWithTask(task -> {
            if (!task.isSuccessful()) throw task.getException();
            return file.getDownloadUrl();
        }).addOnCompleteListener(task -> {
            documentButton.setEnabled(true);
            if (!task.isSuccessful() || task.getResult() == null) {
                Ui.toast(this, "Document upload failed.");
                return;
            }
            update.put("documentUrl", task.getResult().toString());
            saveUpdate(uid, update);
        });
    }

    private void saveUpdate(String uid, Map<String, Object> update) {
        firebase.users().child(uid).updateChildren(update).addOnCompleteListener(task -> {
            Ui.toast(this, task.isSuccessful() ? "Profile updated." : "Profile update failed.");
            if (task.isSuccessful()) firebase.loadCurrentUser(this::render);
        });
    }

    private String value(String value) {
        return value == null ? "" : value;
    }
}
