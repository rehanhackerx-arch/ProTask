package com.example.myempty.protask;

import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import kotlin.Unit;
import kotlin.jvm.functions.Function0;

public class SpinWheelActivity extends BaseActivity {
    private static final String TAG = "SpinWheelActivity";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private SpinWheelView wheel;
    private TextView limitText;
    private TextView resultText;
    private TextView spinButton;
    private TextView extraSpinButton;
    private TextView historyText;
    private boolean isSpinning = false;
    
    private FirebaseManager firebaseManager;
    private ValueEventListener userListener;
    private int spinsLeft = 0;
    private boolean isDataLoaded = false;
    private int spinCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_spin_wheel);
        setupTopHeader("Spin Wheel");

        firebaseManager = FirebaseManager.getInstance();

        wheel = findViewById(R.id.spinWheelView);
        limitText = findViewById(R.id.tvSpinLimit);
        resultText = findViewById(R.id.tvSpinResult);
        spinButton = findViewById(R.id.btnSpin);
        extraSpinButton = findViewById(R.id.btnExtraSpin);
        historyText = findViewById(R.id.tvSpinHistory);

        // Entry Animations
        findViewById(R.id.spinRoot).startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in_up));
        findViewById(R.id.wheelStage).startAnimation(AnimationUtils.loadAnimation(this, R.anim.neon_pulse));
        findViewById(R.id.particleOne).startAnimation(AnimationUtils.loadAnimation(this, R.anim.float_particles));
        findViewById(R.id.particleTwo).startAnimation(AnimationUtils.loadAnimation(this, R.anim.float_particles));
        findViewById(R.id.particleThree).startAnimation(AnimationUtils.loadAnimation(this, R.anim.float_particles));
        findViewById(R.id.particleFour).startAnimation(AnimationUtils.loadAnimation(this, R.anim.float_particles));
        
        bindPressAnimation(spinButton);
        bindPressAnimation(extraSpinButton);
        spinButton.setOnClickListener(view -> spin());
        extraSpinButton.setOnClickListener(view -> watchAdForExtraSpins());

        firebaseManager.resetSpinsIfNeeded();
        startListening();
    }

    private void startListening() {
        userListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Integer left = snapshot.child("spins").getValue(Integer.class);
                    if (left != null) {
                        spinsLeft = left;
                    } else {
                        // Initialize if not present
                        spinsLeft = 15;
                        firebaseManager.updateSpin(spinsLeft);
                    }
                } else {
                    spinsLeft = 15;
                    firebaseManager.updateSpin(spinsLeft);
                }
                isDataLoaded = true;
                updateLimitUi();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database Error", error.toException());
            }
        };
        firebaseManager.listenToUserData(userListener);
    }

    @Override
    protected void onDestroy() {
        firebaseManager.removeUserListener(userListener);
        super.onDestroy();
    }

    private void watchAdForExtraSpins() {
        extraSpinButton.setEnabled(false);
        extraSpinButton.setText("Loading Ad...");

        UnityAdsManager.getInstance().showRewardedAd(this, new UnityAdsManager.AdEventListener() {
            @Override
            public void onAdCompleted() {
                runOnUiThread(() -> {
                    firebaseManager.updateSpin(spinsLeft + 5);
                    extraSpinButton.setEnabled(true);
                    extraSpinButton.setText("Watch Ad for +5 Spins");
                    showRewardPopup("+5 Spins Granted");
                    Log.d(TAG, "Extra spins granted: 5");
                });
            }

            @Override
            public void onAdFailed(String error) {
                runOnUiThread(() -> {
                    extraSpinButton.setEnabled(true);
                    extraSpinButton.setText("Watch Ad for +5 Spins");
                    Toast.makeText(SpinWheelActivity.this, "Ad failed: " + error, Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Ad failed: " + error);
                });
            }

            @Override
            public void onAdStarted() {
                runOnUiThread(() -> {
                    Log.d(TAG, "Ad started for extra spins");
                });
            }
        });
    }

    private void spin() {
        if (isSpinning || !isDataLoaded) return;

        if (spinsLeft <= 0) {
            Toast.makeText(this, "No spins left! Come back later.", Toast.LENGTH_SHORT).show();
            updateLimitUi();
            return;
        }

        spinButton.setEnabled(false);
        spinButton.setText("Loading Ad...");

        UnityAdsManager.getInstance().showRewardedAd(this, new UnityAdsManager.AdEventListener() {
            @Override
            public void onAdCompleted() {
                runOnUiThread(() -> {
                    startSpinLogic();
                });
            }

            @Override
            public void onAdFailed(String error) {
                runOnUiThread(() -> {
                    spinButton.setEnabled(true);
                    spinButton.setText("SPIN");
                    Toast.makeText(SpinWheelActivity.this, "Ad Required: " + error, Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onAdStarted() {
                runOnUiThread(() -> {
                    spinButton.setText("Watching...");
                });
            }
        });
    }

    private void startSpinLogic() {
        // Decrease spins_left in Firebase and increment totalSpins via Manager
        firebaseManager.updateSpin(spinsLeft - 1);

        isSpinning = true;
        spinButton.setEnabled(false);
        spinButton.setText("...");
        vibrate(35);

        int targetIndex = new java.util.Random().nextInt(SpinWheelView.Companion.getPOINTS().length);
        int pointsWon = SpinWheelView.Companion.getPOINTS()[targetIndex];
        
        wheel.spinToSlice(targetIndex, (Function0<Unit>) () -> {
            // Update points and wallet balance via FirebaseManager for sync
            firebaseManager.addReward(this, pointsWon);
            
            resultText.setText("You won " + pointsWon + " points");
            addHistory(pointsWon);
            showRewardPopup("+" + pointsWon + " Points");
            vibrate(85);
            
            isSpinning = false;
            spinButton.setText("SPIN");
            updateLimitUi();
            return Unit.INSTANCE;
        });
    }

    private void updateLimitUi() {
        limitText.setText(spinsLeft + " spins left");
        
        if (!isSpinning) {
            spinButton.setEnabled(spinsLeft > 0);
            spinButton.setAlpha(spinsLeft > 0 ? 1f : 0.55f);
        }
    }

    private void addHistory(int points) {
        spinCount++;
        String row = "Spin " + spinCount + "    +" + points + " points";
        String current = historyText.getText().toString();
        historyText.setText(current.startsWith("No spins") ? row : row + "\n" + current);
    }

    private void showRewardPopup(String rewardText) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        TextView popup = new TextView(this);
        popup.setText(rewardText + "\nWheel reward unlocked");
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

    private void vibrate(long durationMs) {
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator == null) return;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibrator.vibrate(durationMs);
        }
    }
}
