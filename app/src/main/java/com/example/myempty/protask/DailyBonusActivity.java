package com.example.myempty.protask;

import android.app.Dialog;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

public class DailyBonusActivity extends BaseActivity {
    private static final long DAILY_BONUS_MS = 24L * 60L * 60L * 1000L;
    private long dailyBonusReward = 50;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView timerText;
    private TextView claimButton;
    private TextView extraBonusButton;
    private TextView streakText;
    private TextView streakBadgeText;
    private TextView bonusRewardText;
    private ProgressBar progressBonus;
    private CountDownTimer timer;
    private ValueEventListener rewardListener;

    private long lastBonusAt = 0L;
    private ValueEventListener userListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_bonus);
        setupTopHeader("Daily Bonus");

        timerText = findViewById(R.id.tvBonusTimer);
        claimButton = findViewById(R.id.btnClaimBonus);
        streakText = findViewById(R.id.tvDailyStreak);
        streakBadgeText = findViewById(R.id.tvDailyStreakBadge);
        bonusRewardText = findViewById(R.id.tvBonusReward);
        progressBonus = findViewById(R.id.progressBonus);
        extraBonusButton = findViewById(R.id.btnExtraBonus);
        
        loadRewardSettings();
        startListeningForUserData();

        findViewById(R.id.bonusRoot).startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in_up));
        findViewById(R.id.timerCard).startAnimation(AnimationUtils.loadAnimation(this, R.anim.neon_pulse));
        findViewById(R.id.timerPulse).startAnimation(AnimationUtils.loadAnimation(this, R.anim.neon_pulse));
        bindPressAnimation(claimButton);
        bindPressAnimation(extraBonusButton);
        claimButton.setOnClickListener(view -> claimBonus());
        extraBonusButton.setOnClickListener(view -> watchAdForExtraBonus());
        updateBonusState();
    }

    private void startListeningForUserData() {
        userListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.hasChild("lastBonusClaimedAt")) {
                    Long val = snapshot.child("lastBonusClaimedAt").getValue(Long.class);
                    if (val != null) {
                        lastBonusAt = val;
                        updateBonusState();
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {}
        };
        FirebaseManager.getInstance().listenToUserData(userListener);
    }

    private void loadRewardSettings() {
        rewardListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.hasChild("dailyBonusReward")) {
                    dailyBonusReward = snapshot.child("dailyBonusReward").getValue(Long.class);
                }
                bonusRewardText.setText("+" + dailyBonusReward + " pts");
                updateBonusState(); // Refresh button text if needed
            }

            @Override
            public void onCancelled(DatabaseError error) {}
        };
        FirebaseManager.getInstance().listenToRewardSettings(rewardListener);
    }

    private void claimBonus() {
        if (remainingMs() > 0L) {
            Toast.makeText(this, "Bonus is not ready yet.", Toast.LENGTH_SHORT).show();
            return;
        }

        claimButton.setEnabled(false);
        claimButton.setText("Loading Ad...");

        UnityAdsManager.getInstance().showRewardedAd(this, new UnityAdsManager.AdEventListener() {
            @Override
            public void onAdCompleted() {
                runOnUiThread(() -> {
                    // Use FirebaseManager to add reward (updates 'coins' and 'tasksCompleted')
                    FirebaseManager.getInstance().addReward(DailyBonusActivity.this, dailyBonusReward);
                    FirebaseManager.getInstance().updateLastBonusClaimed();

                    showRewardPopup("+" + dailyBonusReward + " Daily Bonus");
                    updateBonusState();
                    claimButton.setEnabled(true);
                });
            }

            @Override
            public void onAdFailed(String error) {
                runOnUiThread(() -> {
                    claimButton.setEnabled(true);
                    updateBonusState(); // Reset button text
                    Toast.makeText(DailyBonusActivity.this, "Ad Required: " + error, Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onAdStarted() {
                runOnUiThread(() -> {
                    claimButton.setText("Watching...");
                });
            }
        });
    }

    private void watchAdForExtraBonus() {
        extraBonusButton.setEnabled(false);
        extraBonusButton.setText("Loading Ad...");

        UnityAdsManager.getInstance().showRewardedAd(this, new UnityAdsManager.AdEventListener() {
            @Override
            public void onAdCompleted() {
                runOnUiThread(() -> {
                    long extraAmount = dailyBonusReward / 2; // Give half of daily bonus as extra
                    FirebaseManager.getInstance().addReward(DailyBonusActivity.this, extraAmount);
                    extraBonusButton.setEnabled(true);
                    extraBonusButton.setText("Watch Ad for Extra Points");
                    showRewardPopup("+" + extraAmount + " Extra Points");
                    Log.d("DailyBonusActivity", "Extra bonus granted: " + extraAmount);
                });
            }

            @Override
            public void onAdFailed(String error) {
                runOnUiThread(() -> {
                    extraBonusButton.setEnabled(true);
                    extraBonusButton.setText("Watch Ad for Extra Points");
                    Toast.makeText(DailyBonusActivity.this, "Ad failed: " + error, Toast.LENGTH_SHORT).show();
                    Log.e("DailyBonusActivity", "Ad failed: " + error);
                });
            }

            @Override
            public void onAdStarted() {
                runOnUiThread(() -> {
                    Log.d("DailyBonusActivity", "Ad started for extra bonus");
                });
            }
        });
    }

    private void updateBonusState() {
        if (timer != null) timer.cancel();
        long remaining = remainingMs();
        boolean ready = remaining <= 0L;
        claimButton.setEnabled(ready);
        claimButton.setAlpha(ready ? 1f : 0.58f);
        claimButton.setBackgroundResource(ready ? R.drawable.bg_neon_button_rounded : R.drawable.bg_glass_card_disabled);
        progressBonus.setProgress(ready ? 7 : 4);
        streakText.setText(ready ? "Streak ready to extend" : "Streak protected today");
        streakBadgeText.setText(ready ? "2x" : "1x");

        if (ready) {
            timerText.setText("Bonus ready");
            claimButton.setText("Claim " + dailyBonusReward + " Points");
            return;
        }

        claimButton.setText("Locked");
        timer = new CountDownTimer(remaining, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                timerText.setText(formatDuration(millisUntilFinished));
            }

            @Override
            public void onFinish() {
                timerText.setText("Bonus ready");
                claimButton.setText("Claim " + dailyBonusReward + " Points");
                claimButton.setEnabled(true);
                claimButton.setAlpha(1f);
                claimButton.setBackgroundResource(R.drawable.bg_neon_button_rounded);
                progressBonus.setProgress(7);
                streakBadgeText.setText("2x");
            }
        }.start();
    }

    private long remainingMs() {
        long nextClaim = lastBonusAt + DAILY_BONUS_MS;
        return Math.max(0L, nextClaim - System.currentTimeMillis());
    }

    private String formatDuration(long ms) {
        long totalSeconds = ms / 1000L;
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        return String.format(java.util.Locale.US, "%02d:%02d:%02d", hours, minutes, seconds);
    }

    private void showRewardPopup(String rewardText) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        TextView popup = new TextView(this);
        popup.setText(rewardText + "\nStreak updated");
        popup.setGravity(android.view.Gravity.CENTER);
        popup.setTextColor(android.graphics.Color.WHITE);
        popup.setTextSize(22f);
        popup.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        popup.setBackgroundResource(R.drawable.bg_neon_panel);
        popup.setPadding(48, 42, 48, 42);
        dialog.setContentView(popup);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.show();
        popup.startAnimation(AnimationUtils.loadAnimation(this, R.anim.reward_pop));
        handler.postDelayed(dialog::dismiss, 1350L);
    }

    @Override
    protected void onDestroy() {
        if (timer != null) timer.cancel();
        FirebaseManager.getInstance().removeRewardSettingsListener(rewardListener);
        FirebaseManager.getInstance().removeUserListener(userListener);
        super.onDestroy();
    }
}
