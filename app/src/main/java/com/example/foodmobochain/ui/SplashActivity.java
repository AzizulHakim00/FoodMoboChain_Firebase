package com.example.foodmobochain.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.example.foodmobochain.R;
import com.example.foodmobochain.data.FirebaseService;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {
    private final FirebaseService firebase = FirebaseService.get();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        new Handler(Looper.getMainLooper()).postDelayed(this::route, 700);
    }

    private void route() {
        FirebaseUser user = firebase.firebaseUser();
        if (user == null) {
            open(AuthActivity.class);
            return;
        }
        user.reload().addOnCompleteListener(task -> {
            FirebaseUser refreshed = firebase.firebaseUser();
            if (refreshed != null && refreshed.isEmailVerified()) {
                open(MainActivity.class);
            } else {
                firebase.auth.signOut();
                open(AuthActivity.class);
            }
        });
    }

    private void open(Class<?> destination) {
        Intent intent = new Intent(this, destination);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
