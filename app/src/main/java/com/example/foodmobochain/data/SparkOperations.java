package com.example.foodmobochain.data;

import androidx.annotation.Nullable;

import com.example.foodmobochain.model.AppUser;
import com.example.foodmobochain.model.CartLine;
import com.example.foodmobochain.model.FoodItem;
import com.example.foodmobochain.model.FoodOrder;
import com.example.foodmobochain.model.RentalBooking;
import com.example.foodmobochain.model.RentalCart;
import com.example.foodmobochain.model.Review;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public final class SparkOperations {
    private static final long DAY_MS = 24L * 60L * 60L * 1000L;

    private SparkOperations() { }

    public interface Callback<T> {
        void onComplete(@Nullable T result, @Nullable Exception error);
    }

    public static void syncAdminProfile(FirebaseService firebase, Callback<Boolean> callback) {
        FirebaseUser authUser = firebase.firebaseUser();
        if (authUser == null || !firebase.isAdminUser()) {
            callback.onComplete(false, null);
            return;
        }
        Map<String, Object> update = new HashMap<>();
        update.put("uid", authUser.getUid());
        update.put("email", authUser.getEmail());
        update.put("role", "admin");
        update.put("status", "active");
        update.put("updatedAt", System.currentTimeMillis());
        firebase.users().child(authUser.getUid()).updateChildren(update)
                .addOnCompleteListener(task -> callback.onComplete(
                        task.isSuccessful(), task.isSuccessful() ? null : task.getException()));
    }

    public static void placeOrders(FirebaseService firebase, String address, Callback<Integer> callback) {
        placeOrders(firebase, address, "", callback);
    }

    public static void placeOrders(FirebaseService firebase, String address, String deliveryNote,
                                   Callback<Integer> callback) {
        String uid = firebase.uid();
        FirebaseUser authUser = firebase.firebaseUser();
        if (uid == null || authUser == null || !authUser.isEmailVerified()) {
            callback.onComplete(null, new IllegalStateException("Sign in with a verified email first."));
            return;
        }
        firebase.loadCurrentUser(profile -> {
            if (profile == null) {
                callback.onComplete(null, new IllegalStateException("Your profile could not be loaded."));
                return;
            }
            firebase.carts().child(uid).get().addOnCompleteListener(cartTask -> {
                if (!cartTask.isSuccessful() || !cartTask.getResult().exists()) {
                    callback.onComplete(null, new IllegalStateException("Your bag is empty."));
                    return;
                }
                DataSnapshot cartSnapshot = cartTask.getResult();
                firebase.foods().get().addOnCompleteListener(foodTask -> {
                    if (!foodTask.isSuccessful()) {
                        callback.onComplete(null, foodTask.getException());
                        return;
                    }
                    try {
                        Map<String, FoodOrder> grouped = new LinkedHashMap<>();
                        for (DataSnapshot child : cartSnapshot.getChildren()) {
                            CartLine requested = child.getValue(CartLine.class);
                            String foodId = child.getKey();
                            if (requested == null || foodId == null || requested.quantity < 1 || requested.quantity > 20) {
                                throw new IllegalArgumentException("The bag contains an invalid item.");
                            }
                            FoodItem official = foodTask.getResult().child(foodId).getValue(FoodItem.class);
                            if (official == null || !official.inStock() || official.vendorId == null) {
                                throw new IllegalArgumentException("An item is no longer available. Refresh your bag.");
                            }
                            String storeId = official.storeId == null || official.storeId.trim().isEmpty()
                                    ? official.vendorId : official.storeId;
                            String groupKey = official.vendorId + "::" + storeId;
                            FoodOrder order = grouped.get(groupKey);
                            if (order == null) {
                                String orderId = firebase.orders().push().getKey();
                                if (orderId == null) throw new IllegalStateException("Could not create an order ID.");
                                order = new FoodOrder();
                                order.id = orderId;
                                order.buyerId = uid;
                                order.vendorId = official.vendorId;
                                order.storeId = storeId;
                                order.storeName = official.vendorName;
                                order.buyerName = profile.name;
                                order.address = address;
                                order.deliveryNote = deliveryNote == null ? "" : deliveryNote.trim();
                                order.paymentMethod = "cash_on_delivery";
                                order.status = "placed";
                                order.createdAt = System.currentTimeMillis();
                                order.updatedAt = order.createdAt;
                                grouped.put(groupKey, order);
                            }
                            order.items.put(foodId, new CartLine(official, requested.quantity));
                        }
                        if (grouped.isEmpty()) throw new IllegalStateException("Your bag is empty.");

                        Map<String, Object> updates = new HashMap<>();
                        for (FoodOrder order : grouped.values()) {
                            updates.put("orders/" + order.id, order);
                        }
                        updates.put("carts/" + uid, null);
                        int count = grouped.size();
                        firebase.root.updateChildren(updates).addOnCompleteListener(saveTask -> callback.onComplete(
                                saveTask.isSuccessful() ? count : null,
                                saveTask.isSuccessful() ? null : saveTask.getException()));
                    } catch (Exception exception) {
                        callback.onComplete(null, exception);
                    }
                });
            });
        });
    }

    public static void advanceOrderStatus(FirebaseService firebase, FoodOrder order, Callback<String> callback) {
        if (order == null || order.id == null) {
            callback.onComplete(null, new IllegalArgumentException("Order information is missing."));
            return;
        }
        String next = nextStatus(order.status);
        if (next == null) {
            callback.onComplete(null, new IllegalStateException("This order has no next status."));
            return;
        }
        Map<String, Object> update = new HashMap<>();
        update.put("status", next);
        update.put("updatedAt", System.currentTimeMillis());
        firebase.orders().child(order.id).updateChildren(update).addOnCompleteListener(task -> callback.onComplete(
                task.isSuccessful() ? next : null,
                task.isSuccessful() ? null : task.getException()));
    }

    public static void submitReview(FirebaseService firebase, FoodOrder order, int stars,
                                    String comment, Callback<String> callback) {
        String uid = firebase.uid();
        if (uid == null || order == null || order.id == null) {
            callback.onComplete(null, new IllegalArgumentException("Order information is missing."));
            return;
        }
        String targetUserId;
        if (uid.equals(order.buyerId)) {
            targetUserId = order.vendorId;
        } else if (uid.equals(order.vendorId)) {
            targetUserId = order.buyerId;
        } else {
            callback.onComplete(null, new SecurityException("Only the buyer or vendor can review this order."));
            return;
        }
        if (targetUserId == null) {
            callback.onComplete(null, new IllegalArgumentException("Review target is missing."));
            return;
        }
        String reviewId = order.id + "_" + uid;
        Review review = new Review();
        review.id = reviewId;
        review.orderId = order.id;
        review.authorId = uid;
        review.targetUserId = targetUserId;
        review.stars = stars;
        review.comment = comment;
        review.createdAt = System.currentTimeMillis();
        firebase.reviews().child(reviewId).setValue(review).addOnCompleteListener(task -> callback.onComplete(
                task.isSuccessful() ? reviewId : null,
                task.isSuccessful() ? null : task.getException()));
    }

    public static void setVendorStatus(FirebaseService firebase, AppUser vendor, String status,
                                       Callback<String> callback) {
        if (!firebase.isAdminUser() || vendor == null || vendor.uid == null
                || !("approved".equals(status) || "rejected".equals(status))) {
            callback.onComplete(null, new SecurityException("Administrator permission is required."));
            return;
        }
        Map<String, Object> update = new HashMap<>();
        update.put("users/" + vendor.uid + "/role", "vendor");
        update.put("users/" + vendor.uid + "/status", status);
        update.put("users/" + vendor.uid + "/updatedAt", System.currentTimeMillis());
        update.put("vendorApplications/" + vendor.uid + "/status", status);
        update.put("vendorApplications/" + vendor.uid + "/reviewedAt", System.currentTimeMillis());
        firebase.root.updateChildren(update).addOnCompleteListener(task -> callback.onComplete(
                task.isSuccessful() ? status : null,
                task.isSuccessful() ? null : task.getException()));
    }

    public static void bookRental(FirebaseService firebase, RentalCart cart, String location,
                                  long startAt, int days, boolean delivery,
                                  Callback<String> callback) {
        String uid = firebase.uid();
        FirebaseUser authUser = firebase.firebaseUser();
        if (uid == null || authUser == null || !authUser.isEmailVerified() || cart == null || cart.id == null) {
            callback.onComplete(null, new IllegalStateException("A verified account and valid cart are required."));
            return;
        }
        String bookingId = firebase.rentalBookings().push().getKey();
        if (bookingId == null) {
            callback.onComplete(null, new IllegalStateException("Could not create a booking ID."));
            return;
        }
        RentalBooking booking = new RentalBooking();
        booking.id = bookingId;
        booking.cartId = cart.id;
        booking.cartName = cart.name;
        booking.userId = uid;
        booking.requestedLocation = location;
        booking.startAt = startAt;
        booking.days = days;
        booking.endAt = startAt + (days * DAY_MS);
        booking.delivery = delivery;
        booking.dailyRate = cart.dailyRate;
        booking.total = cart.dailyRate * days + (delivery ? 300 : 0);
        booking.status = "holding";
        booking.createdAt = System.currentTimeMillis();
        booking.updatedAt = booking.createdAt;

        firebase.rentalBookings().child(bookingId).setValue(booking).addOnCompleteListener(createTask -> {
            if (!createTask.isSuccessful()) {
                callback.onComplete(null, createTask.getException());
                return;
            }
            Map<String, Object> reservations = reservationUpdates(cart.id, bookingId, uid, startAt, days);
            firebase.root.updateChildren(reservations).addOnCompleteListener(reserveTask -> {
                if (!reserveTask.isSuccessful()) {
                    firebase.rentalBookings().child(bookingId).removeValue();
                    callback.onComplete(null, new IllegalStateException(
                            "One or more selected dates are already reserved."));
                    return;
                }
                Map<String, Object> statusUpdate = new HashMap<>();
                statusUpdate.put("status", "requested");
                statusUpdate.put("updatedAt", System.currentTimeMillis());
                firebase.rentalBookings().child(bookingId).updateChildren(statusUpdate)
                        .addOnCompleteListener(statusTask -> {
                            if (statusTask.isSuccessful()) {
                                callback.onComplete(bookingId, null);
                            } else {
                                Map<String, Object> cleanup = reservationUpdates(cart.id, bookingId, uid, startAt, days);
                                for (String key : new java.util.ArrayList<>(cleanup.keySet())) cleanup.put(key, null);
                                cleanup.put("rentalBookings/" + bookingId, null);
                                firebase.root.updateChildren(cleanup);
                                callback.onComplete(null, statusTask.getException());
                            }
                        });
            });
        });
    }

    private static Map<String, Object> reservationUpdates(String cartId, String bookingId,
                                                           String uid, long startAt, int days) {
        Map<String, Object> updates = new HashMap<>();
        SimpleDateFormat keyFormat = new SimpleDateFormat("yyyyMMdd", Locale.US);
        keyFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        long createdAt = System.currentTimeMillis();
        for (int day = 0; day < days; day++) {
            String dateKey = keyFormat.format(new Date(startAt + (day * DAY_MS)));
            Map<String, Object> reservation = new HashMap<>();
            reservation.put("bookingId", bookingId);
            reservation.put("userId", uid);
            reservation.put("createdAt", createdAt);
            updates.put("rentalReservations/" + cartId + "/" + dateKey, reservation);
        }
        return updates;
    }

    private static String nextStatus(String status) {
        if ("placed".equals(status)) return "accepted";
        if ("accepted".equals(status)) return "preparing";
        if ("preparing".equals(status)) return "packed";
        if ("packed".equals(status)) return "out_for_delivery";
        if ("out_for_delivery".equals(status)) return "delivered";
        return null;
    }
}
