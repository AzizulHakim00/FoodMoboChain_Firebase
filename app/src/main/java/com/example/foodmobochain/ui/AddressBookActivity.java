package com.example.foodmobochain.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.example.foodmobochain.model.UserAddress;
import com.example.foodmobochain.util.Ui;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddressBookActivity extends BaseScreenActivity {
    private LinearLayout list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupScreen("Saved addresses", "Faster checkout for home, work and campus", true);
        Button add = Ui.button(this, "Add a delivery address");
        add.setOnClickListener(v -> showAddressDialog(null));
        content.addView(add);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        content.addView(list);
        listenAddresses();
    }

    private void listenAddresses() {
        String uid = firebase.uid();
        if (uid == null) return;
        firebase.addresses().child(uid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<UserAddress> values = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    UserAddress address = child.getValue(UserAddress.class);
                    if (address == null) continue;
                    if (address.id == null) address.id = child.getKey();
                    values.add(address);
                }
                values.sort(Comparator.comparing((UserAddress value) -> !value.defaultAddress)
                        .thenComparingLong(value -> -value.updatedAt));
                render(values);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Ui.toast(AddressBookActivity.this, error.getMessage());
            }
        });
    }

    private void render(List<UserAddress> values) {
        list.removeAllViews();
        if (values.isEmpty()) {
            LinearLayout empty = Ui.softCard(this);
            empty.addView(Ui.title(this, "No saved addresses yet"));
            empty.addView(Ui.body(this, "Save home, work or campus details once, then select them during checkout."));
            list.addView(empty);
            return;
        }
        for (UserAddress address : values) {
            LinearLayout card = Ui.card(this);
            card.addView(Ui.label(this, address.defaultAddress ? "DEFAULT ADDRESS" : safe(address.label, "SAVED ADDRESS")));
            card.addView(Ui.title(this, safe(address.recipientName, "Recipient")));
            card.addView(Ui.body(this, address.displayAddress()
                    + (TextUtils.isEmpty(address.contactNumber) ? "" : "\nContact: " + address.contactNumber)
                    + (TextUtils.isEmpty(address.deliveryNote) ? "" : "\nNote: " + address.deliveryNote)));
            Button edit = Ui.outlineButton(this, "Edit address");
            edit.setOnClickListener(v -> showAddressDialog(address));
            card.addView(edit);
            if (!address.defaultAddress) {
                Button makeDefault = Ui.outlineButton(this, "Make default");
                makeDefault.setOnClickListener(v -> setDefault(address.id));
                card.addView(makeDefault);
            }
            Button delete = Ui.outlineButton(this, "Delete address");
            delete.setOnClickListener(v -> new MaterialAlertDialogBuilder(this)
                    .setTitle("Delete this address?")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Delete", (dialog, which) -> removeAddress(address))
                    .show());
            card.addView(delete);
            list.addView(card);
        }
    }

    private void showAddressDialog(UserAddress existing) {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(Ui.dp(this, 20), 0, Ui.dp(this, 20), 0);
        EditText label = Ui.input(this, "Label: Home, Work or Campus");
        EditText recipient = Ui.input(this, "Recipient name");
        EditText contact = Ui.input(this, "Contact number");
        EditText line1 = Ui.input(this, "Road, house, building and floor");
        EditText area = Ui.input(this, "Area");
        EditText city = Ui.input(this, "City");
        EditText note = Ui.input(this, "Delivery instructions");
        note.setSingleLine(false);
        CheckBox defaultAddress = new CheckBox(this);
        defaultAddress.setText("Use as default address");
        if (existing != null) {
            label.setText(existing.label);
            recipient.setText(existing.recipientName);
            contact.setText(existing.contactNumber);
            line1.setText(existing.line1);
            area.setText(existing.area);
            city.setText(existing.city);
            note.setText(existing.deliveryNote);
            defaultAddress.setChecked(existing.defaultAddress);
        }
        form.addView(label);
        form.addView(recipient);
        form.addView(contact);
        form.addView(line1);
        form.addView(area);
        form.addView(city);
        form.addView(note);
        form.addView(defaultAddress);
        new MaterialAlertDialogBuilder(this)
                .setTitle(existing == null ? "Add delivery address" : "Edit delivery address")
                .setView(form)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (dialog, which) -> saveAddress(existing,
                        label.getText().toString().trim(), recipient.getText().toString().trim(),
                        contact.getText().toString().trim(), line1.getText().toString().trim(),
                        area.getText().toString().trim(), city.getText().toString().trim(),
                        note.getText().toString().trim(), defaultAddress.isChecked()))
                .show();
    }

    private void saveAddress(UserAddress existing, String label, String recipient, String contact,
                             String line1, String area, String city, String note, boolean makeDefault) {
        String uid = firebase.uid();
        if (uid == null) return;
        if (recipient.length() < 2 || line1.length() < 5 || area.length() < 2 || city.length() < 2) {
            Ui.toast(this, "Recipient, full address, area and city are required.");
            return;
        }
        String id = existing == null ? firebase.addresses().child(uid).push().getKey() : existing.id;
        if (id == null) return;
        long now = System.currentTimeMillis();
        UserAddress address = new UserAddress();
        address.id = id;
        address.userId = uid;
        address.label = TextUtils.isEmpty(label) ? "Saved address" : label;
        address.recipientName = recipient;
        address.contactNumber = contact;
        address.line1 = line1;
        address.area = area;
        address.city = city;
        address.deliveryNote = note;
        address.defaultAddress = makeDefault;
        address.createdAt = existing == null ? now : existing.createdAt;
        address.updatedAt = now;
        if (makeDefault) {
            firebase.addresses().child(uid).get().addOnCompleteListener(task -> {
                Map<String, Object> updates = new HashMap<>();
                if (task.isSuccessful()) {
                    for (DataSnapshot child : task.getResult().getChildren()) {
                        updates.put("addresses/" + uid + "/" + child.getKey() + "/defaultAddress", false);
                    }
                }
                updates.put("addresses/" + uid + "/" + id, address);
                firebase.root.updateChildren(updates).addOnCompleteListener(saveTask ->
                        Ui.toast(this, saveTask.isSuccessful() ? "Address saved." : "Could not save address."));
            });
        } else {
            firebase.addresses().child(uid).child(id).setValue(address).addOnCompleteListener(task ->
                    Ui.toast(this, task.isSuccessful() ? "Address saved." : "Could not save address."));
        }
    }

    private void setDefault(String id) {
        String uid = firebase.uid();
        if (uid == null || id == null) return;
        firebase.addresses().child(uid).get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) return;
            Map<String, Object> updates = new HashMap<>();
            for (DataSnapshot child : task.getResult().getChildren()) {
                updates.put("addresses/" + uid + "/" + child.getKey() + "/defaultAddress",
                        id.equals(child.getKey()));
                updates.put("addresses/" + uid + "/" + child.getKey() + "/updatedAt",
                        System.currentTimeMillis());
            }
            firebase.root.updateChildren(updates);
        });
    }

    private void removeAddress(UserAddress address) {
        String uid = firebase.uid();
        if (uid == null || address.id == null) return;
        firebase.addresses().child(uid).child(address.id).removeValue();
    }

    private String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }
}
