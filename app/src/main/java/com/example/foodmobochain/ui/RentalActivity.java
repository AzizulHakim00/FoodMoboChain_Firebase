package com.example.foodmobochain.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.example.foodmobochain.model.RentalBooking;
import com.example.foodmobochain.model.RentalCart;
import com.example.foodmobochain.util.Ui;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class RentalActivity extends BaseScreenActivity {
    private LinearLayout cartList;
    private LinearLayout bookingList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupScreen("Food cart rental", "Choose a cart, date, duration and delivery", true);
        LinearLayout note = Ui.softCard(this);
        note.addView(Ui.label(this, "REAL-TIME AVAILABILITY"));
        note.addView(Ui.heading(this, "Start small. Sell smart."));
        note.addView(Ui.body(this, "A transaction lock prevents two users from booking the same cart for an overlapping active period."));
        content.addView(note);
        content.addView(Ui.heading(this, "Available carts"));
        cartList = new LinearLayout(this);
        cartList.setOrientation(LinearLayout.VERTICAL);
        content.addView(cartList);
        content.addView(Ui.spacer(this, 18));
        content.addView(Ui.heading(this, "My rental requests"));
        bookingList = new LinearLayout(this);
        bookingList.setOrientation(LinearLayout.VERTICAL);
        content.addView(bookingList);
        listenToCarts();
        listenToBookings();
    }

    private void listenToCarts() {
        firebase.rentalCarts().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                cartList.removeAllViews();
                int count = 0;
                for (DataSnapshot child : snapshot.getChildren()) {
                    RentalCart cart = child.getValue(RentalCart.class);
                    if (cart == null || !cart.available) continue;
                    if (cart.id == null) cart.id = child.getKey();
                    cartList.addView(cartCard(cart));
                    count++;
                }
                if (count == 0) cartList.addView(Ui.body(RentalActivity.this,
                        "No carts are published yet. The admin can create starter carts."));
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {
                Ui.toast(RentalActivity.this, error.getMessage());
            }
        });
    }

    private LinearLayout cartCard(RentalCart cart) {
        LinearLayout card = Ui.card(this);
        card.addView(Ui.label(this, cart.location == null ? "DHAKA" : cart.location));
        card.addView(Ui.title(this, cart.name));
        card.addView(Ui.body(this, cart.description));
        card.addView(Ui.title(this, Ui.money(cart.dailyRate) + " / day"));
        Button book = Ui.button(this, "Request this cart");
        book.setOnClickListener(v -> showBookingDialog(cart));
        card.addView(book);
        return card;
    }

    private void showBookingDialog(RentalCart cart) {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(Ui.dp(this, 20), 0, Ui.dp(this, 20), 0);
        EditText location = Ui.input(this, "Business location");
        EditText date = Ui.input(this, "Start date (yyyy-MM-dd)");
        EditText days = Ui.numberInput(this, "Rental duration in days");
        CheckBox delivery = new CheckBox(this);
        delivery.setText("Deliver cart to my location (+৳300)");
        form.addView(location);
        form.addView(date);
        form.addView(days);
        form.addView(delivery);
        new MaterialAlertDialogBuilder(this)
                .setTitle(cart.name)
                .setMessage("The start date must not overlap an active booking.")
                .setView(form)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Request", (dialog, which) -> book(cart,
                        location.getText().toString().trim(),
                        date.getText().toString().trim(),
                        days.getText().toString().trim(), delivery.isChecked()))
                .show();
    }

    private void book(RentalCart cart, String location, String dateText, String daysText, boolean delivery) {
        if (TextUtils.isEmpty(location) || TextUtils.isEmpty(dateText) || TextUtils.isEmpty(daysText)) {
            Ui.toast(this, "Location, start date and duration are required.");
            return;
        }
        int days;
        long startAt;
        try {
            days = Integer.parseInt(daysText);
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            format.setLenient(false);
            Date date = format.parse(dateText);
            if (date == null) throw new ParseException("Invalid date", 0);
            startAt = date.getTime();
        } catch (NumberFormatException | ParseException exception) {
            Ui.toast(this, "Use a valid date and a whole-number duration.");
            return;
        }
        if (days < 1 || days > 90) {
            Ui.toast(this, "Rental duration must be between 1 and 90 days.");
            return;
        }
        long endAt = startAt + days * 86_400_000L;
        String uid = firebase.uid();
        String bookingId = firebase.rentalBookings().push().getKey();
        if (uid == null || bookingId == null || cart.id == null) return;

        firebase.root.child("rentalLocks").child(cart.id).runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                Long reservedUntil = currentData.child("reservedUntil").getValue(Long.class);
                if (reservedUntil != null && startAt < reservedUntil) return Transaction.abort();
                currentData.child("bookingId").setValue(bookingId);
                currentData.child("userId").setValue(uid);
                currentData.child("reservedUntil").setValue(endAt);
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed, DataSnapshot snapshot) {
                if (!committed) {
                    Ui.toast(RentalActivity.this, "That cart is already reserved for the selected period.");
                    return;
                }
                RentalBooking booking = new RentalBooking();
                booking.id = bookingId;
                booking.cartId = cart.id;
                booking.cartName = cart.name;
                booking.userId = uid;
                booking.requestedLocation = location;
                booking.status = "requested";
                booking.startAt = startAt;
                booking.endAt = endAt;
                booking.days = days;
                booking.delivery = delivery;
                booking.total = cart.dailyRate * days + (delivery ? 300 : 0);
                booking.createdAt = System.currentTimeMillis();
                Map<String, Object> updates = new HashMap<>();
                updates.put("rentalBookings/" + bookingId, booking);
                updates.put("userRentalBookings/" + uid + "/" + bookingId, booking);
                firebase.root.updateChildren(updates).addOnCompleteListener(task ->
                        Ui.toast(RentalActivity.this, task.isSuccessful()
                                ? "Rental request submitted." : "The booking could not be saved."));
            }
        });
    }

    private void listenToBookings() {
        String uid = firebase.uid();
        if (uid == null) return;
        firebase.root.child("userRentalBookings").child(uid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        bookingList.removeAllViews();
                        int count = 0;
                        SimpleDateFormat format = new SimpleDateFormat("dd MMM yyyy", Locale.US);
                        for (DataSnapshot child : snapshot.getChildren()) {
                            RentalBooking booking = child.getValue(RentalBooking.class);
                            if (booking == null) continue;
                            LinearLayout card = Ui.card(RentalActivity.this);
                            card.addView(Ui.label(RentalActivity.this, booking.status));
                            card.addView(Ui.title(RentalActivity.this, booking.cartName));
                            card.addView(Ui.body(RentalActivity.this,
                                    format.format(new Date(booking.startAt)) + "  •  " + booking.days
                                            + " days  •  " + Ui.money(booking.total)));
                            bookingList.addView(card);
                            count++;
                        }
                        if (count == 0) bookingList.addView(Ui.body(RentalActivity.this, "No rental requests yet."));
                    }

                    @Override public void onCancelled(@NonNull DatabaseError error) {
                        Ui.toast(RentalActivity.this, error.getMessage());
                    }
                });
    }
}
