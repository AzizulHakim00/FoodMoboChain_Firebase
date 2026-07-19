package com.example.foodmobochain.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.foodmobochain.R;
import com.example.foodmobochain.data.FirebaseService;
import com.example.foodmobochain.util.SessionGuard;

public abstract class BaseScreenActivity extends AppCompatActivity {
    protected final FirebaseService firebase = FirebaseService.get();
    protected LinearLayout content;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen);
        content = findViewById(R.id.screenContent);
    }

    protected void setupScreen(String title, String subtitle, boolean showBack) {
        ((TextView) findViewById(R.id.screenTitle)).setText(title);
        ((TextView) findViewById(R.id.screenSubtitle)).setText(subtitle);
        TextView back = findViewById(R.id.backButton);
        back.setVisibility(showBack ? View.VISIBLE : View.GONE);
        back.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (firebase.firebaseUser() != null && SessionGuard.expired()) {
            firebase.auth.signOut();
            Intent intent = new Intent(this, AuthActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            return;
        }
        SessionGuard.touch();
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        SessionGuard.touch();
    }

    protected void open(Class<?> activity) {
        startActivity(new Intent(this, activity));
    }
}
