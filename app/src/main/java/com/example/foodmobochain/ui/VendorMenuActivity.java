package com.example.foodmobochain.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;

import androidx.annotation.NonNull;

import com.example.foodmobochain.model.AppUser;
import com.example.foodmobochain.model.FoodItem;
import com.example.foodmobochain.util.Ui;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.Arrays;
import java.util.Locale;

public class VendorMenuActivity extends BaseScreenActivity {
    private AppUser vendor;
    private EditText name;
    private EditText description;
    private EditText price;
    private Spinner category;
    private CheckBox available;
    private LinearLayout list;
    private Button save;
    private String editingId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupScreen("Manage menu", "Publish food only after vendor approval", true);
        firebase.loadCurrentUser(user -> {
            vendor = user;
            if (user == null || !"vendor".equals(user.role)
                    || !("approved".equals(user.status) || "active".equals(user.status))) {
                showBlocked();
            } else {
                buildForm();
                listenToMenu();
            }
        });
    }

    private void showBlocked() {
        content.removeAllViews();
        LinearLayout card = Ui.softCard(this);
        card.addView(Ui.heading(this, "Menu access is locked"));
        card.addView(Ui.body(this, "Your vendor application must be approved by the admin first."));
        content.addView(card);
    }

    private void buildForm() {
        content.removeAllViews();
        LinearLayout form = Ui.softCard(this);
        form.addView(Ui.label(this, "MENU ITEM"));
        name = Ui.input(this, "Food name");
        description = Ui.input(this, "Short description");
        price = Ui.numberInput(this, "Price in BDT");
        category = new Spinner(this);
        category.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                Arrays.asList("Rice", "Drinks", "Grill", "Traditional", "Street Food",
                        "Dessert", "Snacks", "Noodles", "Fast Food")));
        category.setBackgroundResource(com.example.foodmobochain.R.drawable.bg_input);
        LinearLayout.LayoutParams spinnerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 54));
        spinnerParams.topMargin = Ui.dp(this, 10);
        category.setLayoutParams(spinnerParams);
        category.setPadding(Ui.dp(this, 12), 0, Ui.dp(this, 12), 0);
        available = new CheckBox(this);
        available.setText("Available for ordering");
        available.setChecked(true);
        available.setTextColor(getColor(com.example.foodmobochain.R.color.brand_ink));
        available.setPadding(0, Ui.dp(this, 10), 0, 0);
        save = Ui.button(this, "Publish item");
        save.setOnClickListener(v -> saveItem());
        form.addView(name);
        form.addView(description);
        form.addView(price);
        form.addView(category);
        form.addView(available);
        form.addView(save);
        content.addView(form);
        content.addView(Ui.spacer(this, 12));
        content.addView(Ui.heading(this, "My published menu"));
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        content.addView(list);
    }

    private void saveItem() {
        String foodName = name.getText().toString().trim();
        String details = description.getText().toString().trim();
        String amount = price.getText().toString().trim();
        if (TextUtils.isEmpty(foodName) || TextUtils.isEmpty(details) || TextUtils.isEmpty(amount)) {
            Ui.toast(this, "Name, description and price are required.");
            return;
        }
        double value;
        try {
            value = Double.parseDouble(amount);
        } catch (NumberFormatException exception) {
            Ui.toast(this, "Enter a valid price.");
            return;
        }
        String id = editingId == null ? firebase.foods().push().getKey() : editingId;
        if (id == null || vendor == null) return;
        FoodItem item = new FoodItem();
        item.id = id;
        item.vendorId = vendor.uid;
        item.vendorName = TextUtils.isEmpty(vendor.businessName) ? vendor.name : vendor.businessName;
        item.name = foodName;
        item.description = details;
        item.price = value;
        item.category = String.valueOf(category.getSelectedItem());
        item.available = available.isChecked();
        item.createdAt = System.currentTimeMillis();
        firebase.foods().child(id).setValue(item).addOnCompleteListener(task -> {
            Ui.toast(this, task.isSuccessful() ? "Menu item saved." : "Could not save the item.");
            if (task.isSuccessful()) clearForm();
        });
    }

    private void listenToMenu() {
        firebase.foods().orderByChild("vendorId").equalTo(vendor.uid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        list.removeAllViews();
                        int count = 0;
                        for (DataSnapshot child : snapshot.getChildren()) {
                            FoodItem item = child.getValue(FoodItem.class);
                            if (item == null) continue;
                            if (item.id == null) item.id = child.getKey();
                            list.addView(menuCard(item));
                            count++;
                        }
                        if (count == 0) list.addView(Ui.body(VendorMenuActivity.this, "No items published yet."));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Ui.toast(VendorMenuActivity.this, error.getMessage());
                    }
                });
    }

    private LinearLayout menuCard(FoodItem item) {
        LinearLayout card = Ui.card(this);
        card.addView(Ui.label(this, item.available ? "AVAILABLE" : "PAUSED"));
        card.addView(Ui.title(this, item.name + "  •  " + Ui.money(item.price)));
        card.addView(Ui.body(this, item.category + " — " + item.description));
        Button edit = Ui.outlineButton(this, "Edit");
        edit.setOnClickListener(v -> edit(item));
        card.addView(edit);
        Button remove = Ui.outlineButton(this, "Delete");
        remove.setOnClickListener(v -> new MaterialAlertDialogBuilder(this)
                .setTitle("Delete " + item.name + "?")
                .setMessage("This removes the item from the public catalogue.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> firebase.foods().child(item.id).removeValue())
                .show());
        card.addView(remove);
        return card;
    }

    private void edit(FoodItem item) {
        editingId = item.id;
        name.setText(item.name);
        description.setText(item.description);
        price.setText(String.format(Locale.US, "%.0f", item.price));
        available.setChecked(item.available);
        for (int i = 0; i < category.getCount(); i++) {
            if (String.valueOf(category.getItemAtPosition(i)).equalsIgnoreCase(item.category)) {
                category.setSelection(i);
                break;
            }
        }
        save.setText("Update item");
    }

    private void clearForm() {
        editingId = null;
        name.setText("");
        description.setText("");
        price.setText("");
        available.setChecked(true);
        save.setText("Publish item");
    }
}
