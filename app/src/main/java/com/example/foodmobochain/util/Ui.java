package com.example.foodmobochain.util;

import android.content.Context;
import android.graphics.Typeface;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.example.foodmobochain.R;

import java.text.NumberFormat;
import java.util.Locale;

public final class Ui {
    private Ui() { }

    public static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    public static TextView heading(Context context, String text) {
        TextView view = text(context, text, 22, R.color.brand_ink);
        view.setTypeface(Typeface.create("sans-serif-black", Typeface.NORMAL));
        return view;
    }

    public static TextView title(Context context, String text) {
        TextView view = text(context, text, 17, R.color.brand_ink);
        view.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        return view;
    }

    public static TextView body(Context context, String text) {
        TextView view = text(context, text, 14, R.color.brand_muted);
        view.setLineSpacing(dp(context, 3), 1f);
        return view;
    }

    public static TextView label(Context context, String text) {
        String value = text == null ? "" : text;
        TextView view = text(context, value.toUpperCase(Locale.ROOT), 11, R.color.brand_green);
        view.setLetterSpacing(0.12f);
        view.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        return view;
    }

    public static TextView text(Context context, String text, int sp, int color) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setTextSize(sp);
        view.setTextColor(ContextCompat.getColor(context, color));
        return view;
    }

    public static EditText input(Context context, String hint) {
        EditText input = new EditText(context);
        input.setHint(hint);
        input.setSingleLine(true);
        input.setTextSize(15);
        input.setTextColor(ContextCompat.getColor(context, R.color.brand_ink));
        input.setHintTextColor(ContextCompat.getColor(context, R.color.brand_muted));
        input.setBackgroundResource(R.drawable.bg_input);
        input.setPadding(dp(context, 15), 0, dp(context, 15), 0);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(context, 54));
        params.topMargin = dp(context, 10);
        input.setLayoutParams(params);
        return input;
    }

    public static EditText numberInput(Context context, String hint) {
        EditText input = input(context, hint);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        return input;
    }

    public static Button button(Context context, String text) {
        return button(context, text, true);
    }

    public static Button outlineButton(Context context, String text) {
        return button(context, text, false);
    }

    private static Button button(Context context, String text, boolean primary) {
        Button button = new Button(context);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextSize(14);
        button.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        button.setTextColor(ContextCompat.getColor(context,
                primary ? R.color.brand_white : R.color.brand_green));
        button.setBackgroundResource(primary ? R.drawable.bg_primary_button : R.drawable.bg_outline_button);
        button.setPadding(dp(context, 14), 0, dp(context, 14), 0);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(context, 52));
        params.topMargin = dp(context, 10);
        button.setLayoutParams(params);
        return button;
    }

    public static LinearLayout card(Context context) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(context, 17), dp(context, 17), dp(context, 17), dp(context, 17));
        card.setBackgroundResource(R.drawable.bg_card);
        card.setElevation(dp(context, 1));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(context, 12);
        card.setLayoutParams(params);
        return card;
    }

    public static LinearLayout softCard(Context context) {
        LinearLayout card = card(context);
        card.setBackgroundResource(R.drawable.bg_soft_card);
        return card;
    }

    public static View spacer(Context context, int heightDp) {
        View view = new View(context);
        view.setLayoutParams(new LinearLayout.LayoutParams(1, dp(context, heightDp)));
        return view;
    }

    public static void toast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    public static String money(double amount) {
        NumberFormat format = NumberFormat.getNumberInstance(new Locale("en", "BD"));
        format.setMaximumFractionDigits(0);
        return "৳" + format.format(amount);
    }
}
