package com.example.foodmobochain.data;

import androidx.annotation.Nullable;

import com.example.foodmobochain.model.AppUser;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public final class FirebaseService {
    public static final String ADMIN_EMAIL = "mdomor01815@gmail.com";
    public static final String DATABASE_URL =
            "https://foodmobochaindb-c36f5-default-rtdb.asia-southeast1.firebasedatabase.app";

    public final FirebaseAuth auth = FirebaseAuth.getInstance();
    public final DatabaseReference root = FirebaseDatabase.getInstance(DATABASE_URL).getReference();

    private static final FirebaseService INSTANCE = new FirebaseService();

    private FirebaseService() { }

    public static FirebaseService get() { return INSTANCE; }

    @Nullable
    public FirebaseUser firebaseUser() { return auth.getCurrentUser(); }

    @Nullable
    public String uid() {
        FirebaseUser user = firebaseUser();
        return user == null ? null : user.getUid();
    }

    public boolean isAdminUser() {
        FirebaseUser user = firebaseUser();
        return user != null && user.isEmailVerified() && user.getEmail() != null
                && ADMIN_EMAIL.equalsIgnoreCase(user.getEmail().trim());
    }

    public DatabaseReference users() { return root.child("users"); }
    public DatabaseReference stores() { return root.child("stores"); }
    public DatabaseReference banners() { return root.child("banners"); }
    public DatabaseReference foods() { return root.child("foods"); }
    public DatabaseReference carts() { return root.child("carts"); }
    public DatabaseReference orders() { return root.child("orders"); }
    public DatabaseReference addresses() { return root.child("addresses"); }
    public DatabaseReference supportTickets() { return root.child("supportTickets"); }
    public DatabaseReference rentalCarts() { return root.child("rentalCarts"); }
    public DatabaseReference rentalBookings() { return root.child("rentalBookings"); }
    public DatabaseReference rentalReservations() { return root.child("rentalReservations"); }
    public DatabaseReference training() { return root.child("training"); }
    public DatabaseReference newsfeed() { return root.child("newsfeed"); }
    public DatabaseReference reviews() { return root.child("reviews"); }
    public DatabaseReference flags() { return root.child("flags"); }
    public DatabaseReference vendorApplications() { return root.child("vendorApplications"); }

    public interface UserCallback { void onResult(@Nullable AppUser user); }

    public void loadCurrentUser(UserCallback callback) {
        String uid = uid();
        if (uid == null) {
            callback.onResult(null);
            return;
        }
        users().child(uid).get().addOnCompleteListener(task -> {
            AppUser user = task.isSuccessful() && task.getResult().exists()
                    ? task.getResult().getValue(AppUser.class)
                    : null;
            callback.onResult(user);
        });
    }
}
