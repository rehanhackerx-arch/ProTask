package com.example.myempty.protask;

import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import com.example.myempty.protask.databinding.ActivityWalletBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import androidx.annotation.NonNull;

public class WalletActivity extends BaseActivity {

    private ActivityWalletBinding binding;
    private DatabaseReference userRef;
    private ValueEventListener userListener;
    private FirebaseAuthHelper authHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWalletBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupTopHeader(
                "My Wallet",
                R.drawable.ic_settings_neon,
                v -> launchScreen(SettingsActivity.class),
                R.drawable.ic_notifications_neon,
                v -> Toast.makeText(this, "No new notifications", Toast.LENGTH_SHORT).show()
        );
        setupPremiumBottomNav(R.id.navWallet);

        authHelper = new FirebaseAuthHelper(this);
        if (authHelper.isSignedIn()) {
            String uid = authHelper.getCurrentUser().getUid();
            userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);
        }

        setupClickListeners();
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
        if (userRef == null) return;

        userListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Long coins = snapshot.child("coins").getValue(Long.class);
                    Integer tasks = snapshot.child("tasksCompleted").getValue(Integer.class);
                    Long withdrawn = snapshot.child("totalWithdrawn").getValue(Long.class);

                    updateBalanceUI(
                            coins != null ? coins.intValue() : 0,
                            tasks != null ? tasks : 0,
                            withdrawn != null ? withdrawn.intValue() : 0
                    );
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(WalletActivity.this, "Failed to sync wallet.", Toast.LENGTH_SHORT).show();
            }
        };
        userRef.addValueEventListener(userListener);
    }

    private void stopListeningForChanges() {
        if (userRef != null && userListener != null) {
            userRef.removeEventListener(userListener);
        }
    }

    private void updateBalanceUI(int coins, int tasks, int withdrawn) {
        binding.tvCoinBalance.setText(String.valueOf(coins));
        
        double inr = coins / UserManager.CONVERSION_RATE;
        binding.tvInrBalance.setText(String.format(java.util.Locale.US, "≈ ₹%.2f INR", inr));

        binding.tvTotalWithdrawals.setText(String.format(java.util.Locale.US, "%.2f", withdrawn / UserManager.CONVERSION_RATE));
        binding.tvTodayEarnings.setText(UserManager.formatCoins(coins));
        binding.tvWeeklyEarnings.setText(UserManager.formatCoins(coins));
        binding.tvTasksDone.setText(String.valueOf(tasks));
    }

    private void setupClickListeners() {
        binding.btnWithdraw.setOnClickListener(v -> launchScreen(WithdrawActivity.class));

        binding.tvViewAll.setOnClickListener(v -> {
            // Handle view all transactions
        });
    }

    private void applyAnimations() {
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.captcha_card_fade_in);
        Animation pulse = AnimationUtils.loadAnimation(this, R.anim.neon_pulse);

        binding.walletCard.startAnimation(fadeIn);
        binding.btnWithdraw.startAnimation(fadeIn);
        binding.btnTransfer.startAnimation(fadeIn);
        
        // Pulse animation for the main balance card
        binding.walletCard.startAnimation(pulse);
    }
}
