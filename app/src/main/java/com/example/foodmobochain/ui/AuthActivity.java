package com.example.foodmobochain.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.foodmobochain.R;
import com.example.foodmobochain.data.FirebaseService;
import com.example.foodmobochain.model.AppUser;
import com.example.foodmobochain.util.Ui;
import com.google.firebase.auth.FirebaseUser;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AuthActivity extends AppCompatActivity {
    private final FirebaseService firebase = FirebaseService.get();
    private EditText nameInput;
    private EditText emailInput;
    private EditText passwordInput;
    private Spinner roleInput;
    private Button submitButton;
    private Button modeButton;
    private TextView forgotButton;
    private ProgressBar progress;
    private boolean registrationMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        nameInput = findViewById(R.id.nameInput);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        roleInput = findViewById(R.id.roleInput);
        submitButton = findViewById(R.id.submitButton);
        modeButton = findViewById(R.id.modeButton);
        forgotButton = findViewById(R.id.forgotButton);
        progress = findViewById(R.id.authProgress);

        String[] roles = {"Buyer", "Vendor", "Student"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, roles);
        roleInput.setAdapter(adapter);

        submitButton.setOnClickListener(v -> submit());
        modeButton.setOnClickListener(v -> switchMode());
        forgotButton.setOnClickListener(v -> resetPassword());
    }

    private void switchMode() {
        registrationMode = !registrationMode;
        nameInput.setVisibility(registrationMode ? View.VISIBLE : View.GONE);
        roleInput.setVisibility(registrationMode ? View.VISIBLE : View.GONE);
        forgotButton.setVisibility(registrationMode ? View.GONE : View.VISIBLE);
        ((TextView) findViewById(R.id.authEyebrow)).setText(
                registrationMode ? "START YOUR JOURNEY" : "WELCOME BACK");
        ((TextView) findViewById(R.id.authTitle)).setText(registrationMode
                ? "Create your account\nand grow with us."
                : "Sign in and keep\nyour cravings close.");
        submitButton.setText(registrationMode ? "Create account" : "Sign in");
        modeButton.setText(registrationMode
                ? "Already registered? Sign in"
                : "New here? Create an account");
    }

    private void submit() {
        String name = nameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim().toLowerCase(Locale.ROOT);
        String password = passwordInput.getText().toString();
        if (TextUtils.isEmpty(email) || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Enter a valid email address");
            return;
        }
        if (password.length() < 6) {
            passwordInput.setError("Use at least 6 characters");
            return;
        }
        if (registrationMode && TextUtils.isEmpty(name)) {
            nameInput.setError("Your name is required");
            return;
        }
        setBusy(true);
        if (registrationMode) {
            register(name, email, password);
        } else {
            login(email, password);
        }
    }

    private void register(String name, String email, String password) {
        firebase.auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful() || task.getResult().getUser() == null) {
                        setBusy(false);
                        Ui.toast(this, message(task.getException()));
                        return;
                    }
                    FirebaseUser authUser = task.getResult().getUser();
                    String role = roleInput.getSelectedItem().toString().toLowerCase(Locale.ROOT);
                    String status = "vendor".equals(role) ? "pending" : "active";
                    AppUser profile = new AppUser(authUser.getUid(), name, email, role, status);

                    firebase.users().child(profile.uid).setValue(profile)
                            .addOnCompleteListener(saveTask -> {
                                if (!saveTask.isSuccessful()) {
                                    setBusy(false);
                                    Ui.toast(this, "Account created, but the profile could not be saved.");
                                    return;
                                }
                                if ("vendor".equals(role)) {
                                    firebase.vendorApplications().child(profile.uid).setValue(profile);
                                }
                                authUser.sendEmailVerification().addOnCompleteListener(mailTask -> {
                                    firebase.auth.signOut();
                                    setBusy(false);
                                    registrationMode = true;
                                    switchMode();
                                    Ui.toast(this, "Account created. Open the verification email, then sign in.");
                                });
                            });
                });
    }

    private void login(String email, String password) {
        firebase.auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful() || task.getResult().getUser() == null) {
                        setBusy(false);
                        Ui.toast(this, message(task.getException()));
                        return;
                    }
                    FirebaseUser user = task.getResult().getUser();
                    user.reload().addOnCompleteListener(reloadTask -> {
                        FirebaseUser refreshed = firebase.firebaseUser();
                        if (refreshed == null || !refreshed.isEmailVerified()) {
                            user.sendEmailVerification();
                            firebase.auth.signOut();
                            setBusy(false);
                            Ui.toast(this, "Verify your email first. A new verification email was sent.");
                            return;
                        }
                        syncAdminAndOpen(refreshed);
                    });
                });
    }

    private void syncAdminAndOpen(FirebaseUser user) {
        if (!firebase.isAdminEmail(user.getEmail())) {
            openDashboard();
            return;
        }
        Map<String, Object> update = new HashMap<>();
        update.put("uid", user.getUid());
        update.put("email", user.getEmail());
        update.put("name", user.getDisplayName() == null ? "FoodMoboChain Admin" : user.getDisplayName());
        update.put("role", "admin");
        update.put("status", "active");
        update.put("createdAt", System.currentTimeMillis());
        firebase.users().child(user.getUid()).updateChildren(update)
                .addOnCompleteListener(task -> openDashboard());
    }

    private void resetPassword() {
        String email = emailInput.getText().toString().trim();
        if (TextUtils.isEmpty(email)) {
            emailInput.setError("Enter your email first");
            return;
        }
        setBusy(true);
        firebase.auth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
            setBusy(false);
            Ui.toast(this, task.isSuccessful()
                    ? "Password reset email sent."
                    : message(task.getException()));
        });
    }

    private void openDashboard() {
        setBusy(false);
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void setBusy(boolean busy) {
        progress.setVisibility(busy ? View.VISIBLE : View.GONE);
        submitButton.setEnabled(!busy);
        modeButton.setEnabled(!busy);
        forgotButton.setEnabled(!busy);
    }

    private String message(Exception exception) {
        return exception == null ? "Something went wrong. Please try again." : exception.getLocalizedMessage();
    }
}
