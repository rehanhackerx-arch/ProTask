package com.example.myempty.protask;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import com.example.myempty.protask.databinding.ActivityWithdrawBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import androidx.annotation.NonNull;

import java.util.Locale;
import java.util.regex.Pattern;

public class WithdrawActivity extends BaseActivity {

    private ActivityWithdrawBinding binding;
    private FirebaseManager firebaseManager;
    private ValueEventListener userListener;
    private int currentPoints = 0;
    private String selectedMethod = "UPI";
    private static final int MIN_WITHDRAW_POINTS = 50000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWithdrawBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupTopHeader("Withdraw Funds");
        setupPremiumBottomNav(R.id.navWithdraw);

        firebaseManager = FirebaseManager.getInstance();

        setupClickListeners();
        setupInputWatchers();
        applyAnimations();
    }

    @Override
    protected void onStart() {
        super.onStart();
        startListeningForChanges();
    }

    @Override
    protected void onStop() {
        stopListeningForChanges();
        super.onStop();
    }

    private void startListeningForChanges() {
        userListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Long points = snapshot.child("coins").getValue(Long.class);
                    currentPoints = points != null ? points.intValue() : 0;
                    updateBalanceUI(currentPoints);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(WithdrawActivity.this, "Failed to sync balance.", Toast.LENGTH_SHORT).show();
            }
        };
        firebaseManager.listenToUserData(userListener);
    }

    private void stopListeningForChanges() {
        firebaseManager.removeUserListener(userListener);
    }

    private void updateBalanceUI(int points) {
        binding.tvWithdrawCoinBalance.setText(String.format(Locale.US, "%,d Coins", points));
        double inr = points / UserManager.CONVERSION_RATE;
        binding.tvWithdrawInrBalance.setText(String.format(Locale.US, "≈ ₹%.2f", inr));
    }

    private void setupInputWatchers() {
        binding.etAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    if (s.length() > 0) {
                        int amount = Integer.parseInt(s.toString());
                        double inr = amount / UserManager.CONVERSION_RATE;
                        binding.tvInrConversion.setText(String.format(Locale.US, "INR Value: ₹%.2f", inr));
                    } else {
                        binding.tvInrConversion.setText("INR Value: ₹0.00");
                    }
                } catch (Exception e) {
                    binding.tvInrConversion.setText("INR Value: ₹0.00");
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupClickListeners() {
        binding.methodUpi.setOnClickListener(v -> selectPaymentMethod("UPI", binding.methodUpi, "Enter UPI ID"));
        binding.methodPaytm.setOnClickListener(v -> selectPaymentMethod("Paytm", binding.methodPaytm, "Enter Paytm Number"));
        binding.methodGPay.setOnClickListener(v -> selectPaymentMethod("GPay", binding.methodGPay, "Enter GPay Number"));

        binding.btnSubmitWithdraw.setOnClickListener(v -> validateAndSubmit());
    }

    private void validateAndSubmit() {
        String detail = binding.etPaymentDetail.getText().toString().trim();
        String amountStr = binding.etAmount.getText().toString().trim();

        if (detail.isEmpty()) {
            binding.etPaymentDetail.setError("Details required");
            return;
        }

        // Simple Mobile Number Validation for Indian numbers (optional check for GPay/Paytm)
        if (selectedMethod.equals("GPay") || selectedMethod.equals("Paytm")) {
            if (!Pattern.compile("^[6-9]\\d{9}$").matcher(detail).matches()) {
                binding.etPaymentDetail.setError("Invalid 10-digit number");
                return;
            }
        }

        if (amountStr.isEmpty()) {
            binding.etAmount.setError("Enter amount");
            return;
        }

        int amount = Integer.parseInt(amountStr);
        if (amount < MIN_WITHDRAW_POINTS) {
            binding.etAmount.setError("Min. " + MIN_WITHDRAW_POINTS + " Points");
            return;
        }

        if (amount > currentPoints) {
            Toast.makeText(this, "Insufficient balance!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Loading state
        binding.btnSubmitWithdraw.setEnabled(false);
        binding.btnSubmitWithdraw.setText("Processing...");

        double inrAmount = amount / UserManager.CONVERSION_RATE;

        firebaseManager.submitWithdrawalRequest(selectedMethod, detail, amount, inrAmount, task -> {
            binding.btnSubmitWithdraw.setEnabled(true);
            binding.btnSubmitWithdraw.setText("Withdraw Now");
            
            if (task.isSuccessful()) {
                showSuccessDialog(amount, inrAmount);
                binding.etAmount.setText("");
                binding.etPaymentDetail.setText("");
            } else {
                Toast.makeText(this, "Request failed. Try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showSuccessDialog(int points, double inr) {
        // Simple Success Dialog for now
        new MaterialAlertDialogBuilder(this, R.style.ProTaskDialog)
                .setTitle("Request Submitted!")
                .setMessage(String.format(Locale.US, "Your request for %,d points (₹%.2f) has been submitted via %s. It will be processed soon.", points, inr, selectedMethod))
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void selectPaymentMethod(String method, View selectedView, String label) {
        selectedMethod = method;
        binding.tvInputLabel.setText(label);

        // Reset all backgrounds
        binding.methodUpi.setBackgroundResource(R.drawable.bg_glass_card);
        binding.methodPaytm.setBackgroundResource(R.drawable.bg_glass_card);
        binding.methodGPay.setBackgroundResource(R.drawable.bg_glass_card);

        // Highlight selected
        selectedView.setBackgroundResource(R.drawable.bg_wallet_glow);

        // Scale animation
        Animation scaleUp = AnimationUtils.loadAnimation(this, R.anim.reward_pop);
        selectedView.startAnimation(scaleUp);
    }

    private void applyAnimations() {
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.captcha_card_fade_in);
        binding.btnSubmitWithdraw.startAnimation(fadeIn);
    }
}
