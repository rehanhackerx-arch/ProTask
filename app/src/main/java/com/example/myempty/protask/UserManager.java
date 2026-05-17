package com.example.myempty.protask;

import android.widget.TextView;

import java.util.Locale;

public class UserManager {
    // Conversion rate: 1000 points = 1 INR
    public static final double CONVERSION_RATE = 1000.0;

    public static String formatCoins(int coins) {
        return String.format(Locale.US, "%,d", Math.max(0, coins));
    }

    public static String formatInr(int coins) {
        double inr = Math.max(0, coins) / CONVERSION_RATE;
        return String.format(Locale.US, "≈ ₹%.2f INR", inr);
    }

    public static void updateBalanceUI(TextView textView, int coins, String suffix) {
        if (textView == null) return;
        String formattedCoins = formatCoins(coins);
        textView.setText(suffix == null || suffix.isEmpty() ? formattedCoins : formattedCoins + suffix);
    }
}
