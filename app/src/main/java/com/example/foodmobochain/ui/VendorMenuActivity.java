package com.example.foodmobochain.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.example.foodmobochain.R;
import com.example.foodmobochain.model.AppUser;
import com.example.foodmobochain.model.FoodItem;
import com.example.foodmobochain.util.ImageLoader;
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
    private EditText imageUrl;
    private EditText preparationMinutes;
    private EditText deliveryFee;
    private android.widget.Spinner category;
    private CheckBox available;
    private CheckBox featured;
    private LinearLayout list;
    private Button save;
    private String editingId;
    private long editingCreatedAt;
    private double editingRating;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupScreen("Vendor menu studio", "Publish professional listings for your customers", true);
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
        card.addView(Ui.body(this, "Your vendor application must be approved by the administrator first."));
        content.addView(card);
    }

    private void buildForm() {
        content.removeAllViews();
        LinearLayout intro = Ui.softCard(this);
        intro.addView(Ui.label(this, "SELLER CENTRE"));
        intro.addView(Ui.heading(this, "Create a menu customers can trust."));
        intro.addView(Ui.body(this,
                "Add a clear image, accurate price, preparation time and availability. Public HTTPS image links work on the free Spark plan."));
        content.addView(intro);

        LinearLayout form = Ui.card(this);
        form.addView(Ui.label(this, "MENU ITEM DETAILS"));
        name = Ui.input(this, "Food name");
        description = Ui.input(this, "Description and ingredients");
        description.setSingleLine(false);
        price = Ui.numberInput(this, "Price in BDT");
        imageUrl = Ui.input(this, "Public HTTPS food image URL");
        preparationMinutes = Ui.numberInput(this, "Preparation time in minutes");
        deliveryFee = Ui.numberInput(this, "Delivery fee in BDT (0 for free)");
        category = new android.widget.Spinner(this);
        category.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                Arrays.asList("Rice", "Traditional", "Street Food", "Fast Food", "Snacks",
                        "Noodles", "Grill", "Drinks", "Dessert")));
        category.setBackgroundResource(R.drawable.bg_input);
        LinearLayout.LayoutParams spinnerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 54));
        spinnerParams.topMargin = Ui.dp(this, 10);
        category.setLayoutParams(spinnerParams);
        category.setPadding(Ui.dp(this, 12), 0, Ui.dp(this, 12), 0);

        available = new CheckBox(this);
        available.setText("Available for ordering");
        available.setChecked(true);
        available.setTextColor(getColor(R.color.brand_ink));
        available.setPadding(0, Ui.dp(this, 10), 0, 0);
        featured = new CheckBox(this);
        featured.setText("Request featured placement");
        featured.setTextColor(getColor(R.color.brand_ink));

        save = Ui.button(this, "Publish item");
        save.setOnClickListener(v -> saveItem());
        form.addView(name);
        form.addView(description);
        form.addView(price);
        form.addView(imageUrl);
        form.addView(preparationMinutes);
        form.addView(deliveryFee);
        form.addView(category);
        form.addView(available);
        form.addView(featured);
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
        String image = imageUrl.getText().toString().trim();
        if (TextUtils.isEmpty(foodName) || TextUtils.isEmpty(details) || TextUtils.isEmpty(amount)) {
            Ui.toast(this, "Name, description and price are required.");
            return;
        }
        if (!TextUtils.isEmpty(image) && !image.startsWith("https://")) {
            imageUrl.setError("Use a public https:// image link");
            return;
        }

        double value;
        double fee;
        int prep;
        try {
            value = Double.parseDouble(amount);
            fee = TextUtils.isEmpty(deliveryFee.getText()) ? 0
                    : Double.parseDouble(deliveryFee.getText().toString().trim());
            prep = TextUtils.isEmpty(preparationMinutes.getText()) ? 20
                    : Integer.parseInt(preparationMinutes.getText().toString().trim());
        } catch (NumberFormatException exception) {
            Ui.toast(this, "Enter valid numbers for price, time and delivery fee.");
            return;
        }
        if (value <= 0 || value > 100000 || fee < 0 || fee > 1000 || prep < 5 || prep > 180) {
            Ui.toast(this, "Use a positive price, 5–180 minute preparation time and delivery fee up to ৳1,000.");
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
        item.imageUrl = image;
        item.preparationMinutes = prep;
        item.deliveryFee = fee;
        item.available = available.isChecked();
        item.featured = featured.isChecked();
        item.rating = editingRating;
        item.discountPercent = 0;
        item.createdAt = editingId == null ? System.currentTimeMillis() : editingCreatedAt;
        firebase.foods().child(id).setValue(item).addOnCompleteListener(task -> {
            Ui.toast(this, task.isSuccessful() ? "Menu item saved." : "Could not save the item: "
                    + (task.getException() == null ? "permission denied" : task.getException().getLocalizedMessage()));
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
                        if (count == 0) list.addView(Ui.body(VendorMenuActivity.this,
                                "No items published yet. Add your first professional menu listing above."));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Ui.toast(VendorMenuActivity.this, error.getMessage());
                    }
                });
    }

    private LinearLayout menuCard(FoodItem item) {
        LinearLayout card = Ui.card(this);
        ImageView image = new ImageView(this);
        image.setContentDescription(item.name + " image");
        image.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 160)));
        ImageLoader.load(image, item.imageUrl, R.drawable.ic_food_plate);
        card.addView(image);
        card.addView(Ui.label(this, item.available ? "AVAILABLE" : "PAUSED"));
        card.addView(Ui.title(this, item.name + "  •  " + Ui.money(item.price)));
        card.addView(Ui.body(this, item.category + " — " + item.description + "\n"
                + (item.preparationMinutes > 0 ? item.preparationMinutes : 20) + " min prep  •  "
                + (item.deliveryFee <= 0 ? "Free delivery" : Ui.money(item.deliveryFee) + " delivery")));
        Button preview = Ui.button(this, "Preview customer view");
        preview.setOnClickListener(v -> FoodDetailActivity.open(this, item));
        card.addView(preview);
        Button edit = Ui.outlineButton(this, "Edit listing");
        edit.setOnClickListener(v -> edit(item));
        card.addView(edit);
        Button remove = Ui.outlineButton(this, "Delete listing");
        remove.setOnClickListener(v -> new MaterialAlertDialogBuilder(this)
                .setTitle("Delete " + item.name + "?")
                .setMessage("This removes the item from the public marketplace.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> firebase.foods().child(item.id).removeValue())
                .show());
        card.addView(remove);
        return card;
    }

    private void edit(FoodItem item) {
        editingId = item.id;
        editingCreatedAt = item.createdAt;
        editingRating = item.rating;
        name.setText(item.name);
        description.setText(item.description);
        price.setText(String.format(Locale.US, "%.0f", item.price));
        imageUrl.setText(item.imageUrl == null ? "" : item.imageUrl);
        preparationMinutes.setText(String.valueOf(item.preparationMinutes > 0 ? item.preparationMinutes : 20));
        deliveryFee.setText(String.format(Locale.US, "%.0f", Math.max(0, item.deliveryFee)));
        available.setChecked(item.available);
        featured.setChecked(item.featured);
        for (int i = 0; i < category.getCount(); i++) {
            if (String.valueOf(category.getItemAtPosition(i)).equalsIgnoreCase(item.category)) {
                category.setSelection(i);
                break;
            }
        }
        save.setText("Update listing");
    }

    private void clearForm() {
        editingId = null;
        editingCreatedAt = 0;
        editingRating = 0;
        name.setText("");
        description.setText("");
        price.setText("");
        imageUrl.setText("");
        preparationMinutes.setText("20");
        deliveryFee.setText("0");
        available.setChecked(true);
        featured.setChecked(false);
        save.setText("Publish item");
    }
}
