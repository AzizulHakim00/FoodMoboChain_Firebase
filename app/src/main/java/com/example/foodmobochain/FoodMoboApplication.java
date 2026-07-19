package com.example.foodmobochain;

import android.app.Application;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DatabaseException;
import com.google.firebase.database.FirebaseDatabase;

public class FoodMoboApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);
        try { FirebaseDatabase.getInstance().setPersistenceEnabled(true); }
        catch (DatabaseException ignored) { }
    }
}
