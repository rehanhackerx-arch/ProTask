package com.example.myempty.protask;

import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

public class WatchAdsActivity extends BaseActivity {
    private static final String TAG = "WatchAdsActivity";
    private long adReward = 25;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView statusText;
    private TextView watchButton;
    private TextView pointsText;
    private TextView perVideoRewardText;
    private TextView shimmerView;
    private ProgressBar adProgress;
    private int sessionPoints = 0;
    
    private FirebaseManager firebaseManager;
    private ValueEventListener rewardListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watch_ads);
        setupTopHeader("Watch Ads");

        firebaseManager = FirebaseManager.getInstance();

        statusText = findViewById(R.id.tvAdStatus);
        watchButton = findViewById(R.id.btnWatchAd);
        pointsText = findViewById(R.id.tvAdPointsEarned);
        perVideoRewardText = findViewById(R.id.tvPerVideoReward);
        shimmerView = findViewById(R.id.viewShimmer);
        adProgress = findViewById(R.id.progressAd);
        
        updateRewardUi();

        findViewById(R.id.watchRoot).startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in_up));
        findViewById(R.id.ivRewardIcon).startAnimation(AnimationUtils.loadAnimation(this, R.anim.neon_pulse));
        shimmerView.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shimmer_pulse));
        bindPressAnimation(watchButton);
        watchButton.setOnClickListener(view -> showRewardedAd());

        listenToRewards();
    }

    private void listenToRewards() {
        rewardListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Long val = snapshot.child("adReward").getValue(Long.class);
                    if (val != null) {
                        adReward = val;
                        updateRewardUi();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Reward sync failed", error.toException());
            }
        };
        firebaseManager.listenToRewardSettings(rewardListener);
    }

    private void updateRewardUi() {
        ((TextView) findViewById(R.id.tvRewardAmount)).setText("+" + adReward + " POINTS");
        statusText.setText("Ready to earn +" + adReward + " points");
        perVideoRewardText.setText("+" + adReward);
    }

    @Override
    protected void onDestroy() {
        firebaseManager.removeRewardSettingsListener(rewardListener);
        super.onDestroy();
    }

    private void showRewardedAd() {
        watchButton.setEnabled(false);
        watchButton.setText("Loading Ad...");
        statusText.setText("Connecting to ad server...");
        
        UnityAdsManager.getInstance().showRewardedAd(this, new UnityAdsManager.AdEventListener() {
            @Override
            public void onAdCompleted() {
                runOnUiThread(() -> {
                    firebaseManager.addReward(WatchAdsActivity.this, adReward);
                    sessionPoints += adReward;
                    pointsText.setText(String.valueOf(sessionPoints));
                    statusText.setText("Reward granted. " + adReward + " points added.");
                    watchButton.setText("Watch Rewarded Ad");
                    watchButton.setEnabled(true);
                    showRewardPopup("+" + adReward + " Points");
                    Log.d(TAG, "Reward granted: " + adReward);
                });
            }

            @Override
            public void onAdFailed(String error) {
                runOnUiThread(() -> {
                    statusText.setText("Ad failed: " + error);
                    watchButton.setText("Retry Ad");
                    watchButton.setEnabled(true);
                    Log.e(TAG, "Ad failed: " + error);
                    Toast.makeText(WatchAdsActivity.this, "Ad failed: " + error, Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onAdStarted() {
                runOnUiThread(() -> {
                    statusText.setText("Ad in progress...");
                    watchButton.setText("Watching...");
                    Log.d(TAG, "Ad started");
                });
            }
        });
    }

    private void showRewardPopup(String rewardText) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        TextView popup = new TextView(this);
        popup.setText(rewardText + "\nAdded to wallet");
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
}
