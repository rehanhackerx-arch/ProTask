package com.example.myempty.protask;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import com.example.myempty.protask.databinding.ActivityMainBinding;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import androidx.annotation.NonNull;

public class MainActivity extends BaseActivity {
    private static final int MONTHLY_TARGET_POINTS = 2000;

    private ActivityMainBinding binding;
    private CircularProgressIndicator progressScore;
    private TextView tvEarnedRewards;
    private TextView tvProgressPercent;
    private TextView tvScore;
    private TextView tvLevel;
    private TextView tvTasksCompleted;
    private TextView tvUsername;
    private ImageView imgProfile;
    private TextView tvProfilePlaceholder;
    
    private FirebaseManager firebaseManager;
    private ValueEventListener userListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseManager = FirebaseManager.getInstance();
        firebaseManager.resetSpinsIfNeeded();

        progressScore = binding.progressScore;
        tvEarnedRewards = binding.tvEarnedRewards;
        tvProgressPercent = binding.tvProgressPercent;
        tvScore = binding.tvScore;
        tvLevel = binding.tvLevel;
        tvTasksCompleted = binding.tvTasksCompleted;
        tvUsername = binding.tvUsername;
        imgProfile = binding.imgProfile;
        tvProfilePlaceholder = binding.tvProfilePlaceholder;

        bindTask(R.id.cardCaptcha, CaptchaActivity.class);
        bindTask(R.id.cardSpin, SpinWheelActivity.class);
        bindTask(R.id.cardAds, WatchAdsActivity.class);
        bindTask(R.id.cardBonus, DailyBonusActivity.class);
        bindTask(R.id.btnProfile, ProfileActivity.class);

        bindAnimatedClick(R.id.btnNotifications, view ->
                Toast.makeText(this, "No new notifications", Toast.LENGTH_SHORT).show());
        setupPremiumBottomNav(R.id.navHome);
        bindAnimatedClick(R.id.cardRefer, view -> launchScreen(ReferEarnActivity.class));

        animateEntry();
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
                    // Fetch data from Firebase Realtime sync
                    Long coins = snapshot.child("coins").getValue(Long.class);
                    Integer tasks = snapshot.child("tasksCompleted").getValue(Integer.class);
                    String username = snapshot.child("username").getValue(String.class);
                    String profilePhoto = snapshot.child("profilePhoto").getValue(String.class);

                    // Fallbacks as per requirements
                    if (username == null || username.isEmpty()) {
                        username = snapshot.child("displayName").getValue(String.class);
                    }
                    if (username == null || username.isEmpty()) {
                        username = snapshot.child("name").getValue(String.class);
                    }
                    if (username == null || username.isEmpty()) {
                        if (firebaseManager.getCurrentUser() != null) {
                            username = firebaseManager.getCurrentUser().getDisplayName();
                        }
                    }
                    if (username == null || username.isEmpty()) {
                        username = "ProTask";
                    }

                    if (profilePhoto == null || profilePhoto.isEmpty()) {
                        profilePhoto = snapshot.child("photoUrl").getValue(String.class);
                    }

                    updateUI(coins != null ? coins.intValue() : 0, tasks != null ? tasks : 0, username, profilePhoto);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Failed to load data.", Toast.LENGTH_SHORT).show();
            }
        };
        firebaseManager.listenToUserData(userListener);
    }

    private void stopListeningForChanges() {
        firebaseManager.removeUserListener(userListener);
    }

    private void bindTask(int viewId, Class<?> destination) {
        bindAnimatedClick(viewId, view -> {
            launchScreen(destination);
        });
    }

    private void bindAnimatedClick(int viewId, View.OnClickListener clickListener) {
        View view = findViewById(viewId);
        if (view == null) return;

        view.setOnTouchListener((target, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                target.animate().scaleX(0.96f).scaleY(0.96f).setDuration(90).start();
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                target.animate().scaleX(1f).scaleY(1f).setDuration(140).start();
            }
            return false;
        });
        view.setOnClickListener(clickListener);
    }

    private void updateUI(int rewards, int tasks, String username, String profilePhotoUrl) {
        // Use wallet points (rewards) for progress as per requirements
        int progress = Math.min(100, Math.round((rewards * 100f) / MONTHLY_TARGET_POINTS));
        
        tvUsername.setText(username);
        tvEarnedRewards.setText(rewards + " pts");
        tvProgressPercent.setText(progress + "%");
        tvLevel.setText(levelFor(rewards));
        tvTasksCompleted.setText(tasks + " Tasks");
        tvScore.setText("Score " + rewards + " / " + MONTHLY_TARGET_POINTS);

        if (profilePhotoUrl != null && !profilePhotoUrl.isEmpty()) {
            Glide.with(this)
                .load(profilePhotoUrl)
                .circleCrop()
                .into(imgProfile);
            imgProfile.setVisibility(View.VISIBLE);
            tvProfilePlaceholder.setVisibility(View.GONE);
        } else {
            imgProfile.setVisibility(View.GONE);
            tvProfilePlaceholder.setVisibility(View.VISIBLE);
            if (username != null && !username.isEmpty()) {
                tvProfilePlaceholder.setText(username.substring(0, Math.min(2, username.length())).toUpperCase());
            }
        }

        ObjectAnimator animator = ObjectAnimator.ofInt(progressScore, "progress", progressScore.getProgress(), progress);
        animator.setDuration(900);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.start();
    }

    private String levelFor(int score) {
        if (score >= 5000) return "Elite";
        if (score >= 1500) return "Pro";
        if (score >= 500) return "Rising";
        return "Starter";
    }

    private void animateEntry() {
        View scoreCard = findViewById(R.id.cardScore);
        View bottomNav = findViewById(R.id.bottomNav);
        if (scoreCard != null) {
            scoreCard.setAlpha(0f);
            scoreCard.setTranslationY(28f);
            scoreCard.animate().alpha(1f).translationY(0f).setDuration(550).start();
        }
        if (bottomNav != null) {
            bottomNav.setTranslationY(120f);
            bottomNav.animate().translationY(0f).setStartDelay(180).setDuration(520).start();
        }
    }
}
