package com.example.foodmobochain.util;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class FavoritesStore {
    private static final String PREFS = "foodmobochain_favourites";
    private static final String KEY_IDS = "food_ids";

    private FavoritesStore() { }

    private static SharedPreferences preferences(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean contains(Context context, String foodId) {
        return foodId != null && preferences(context).getStringSet(KEY_IDS, Collections.emptySet())
                .contains(foodId);
    }

    public static boolean toggle(Context context, String foodId) {
        if (foodId == null) return false;
        Set<String> values = new HashSet<>(preferences(context)
                .getStringSet(KEY_IDS, Collections.emptySet()));
        boolean added;
        if (values.contains(foodId)) {
            values.remove(foodId);
            added = false;
        } else {
            values.add(foodId);
            added = true;
        }
        preferences(context).edit().putStringSet(KEY_IDS, values).apply();
        return added;
    }

    public static Set<String> ids(Context context) {
        return new HashSet<>(preferences(context).getStringSet(KEY_IDS, Collections.emptySet()));
    }
}
