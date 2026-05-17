package com.example.myempty.protask;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.myempty.protask.databinding.ActivityProfileBinding;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import androidx.annotation.NonNull;

public class ProfileActivity extends BaseActivity {
    private static final String TAG = "ProfileActivity";
    private ActivityProfileBinding binding;
    private FirebaseManager firebaseManager;
    private ValueEventListener userListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        firebaseManager = FirebaseManager.getInstance();
        
        setupTopHeader("Profile", R.drawable.ic_settings_neon, view ->
                launchScreen(SettingsActivity.class));

        // Sync Google Profile on entry
        firebaseManager.syncGoogleProfile();

        binding.profileRoot.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in_up));
        bindPressAnimation(binding.btnWithdraw);
        bindPressAnimation(binding.btnEditProfile);
        bindPressAnimation(binding.btnLogout);

        binding.btnWithdraw.setOnClickListener(view -> launchScreen(WithdrawActivity.class));
        binding.btnEditProfile.setOnClickListener(view ->
                Toast.makeText(this, "Edit profile coming next.", Toast.LENGTH_SHORT).show());
        binding.btnLogout.setOnClickListener(view -> logout());
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
                    Log.d(TAG, "User data updated from Firebase");
                    
                    // Priority: Firebase DB values, fallback to FirebaseAuth for Google Data
                    String username = snapshot.child("username").getValue(String.class);
                    if (username == null || username.isEmpty()) {
                        username = snapshot.child("name").getValue(String.class);
                    }
                    String email = snapshot.child("email").getValue(String.class);
                    String profilePhoto = snapshot.child("profilePhoto").getValue(String.class);

                    FirebaseUser googleUser = firebaseManager.getCurrentUser();
                    if (googleUser != null) {
                        if (username == null || username.isEmpty()) username = googleUser.getDisplayName();
                        if (email == null || email.isEmpty()) email = googleUser.getEmail();
                        if (profilePhoto == null || profilePhoto.isEmpty()) {
                            profilePhoto = googleUser.getPhotoUrl() != null ? googleUser.getPhotoUrl().toString() : "";
                        }
                    }

                    Long coins = snapshot.child("coins").getValue(Long.class);
                    Integer tasks = snapshot.child("tasksCompleted").getValue(Integer.class);
                    Integer spinsLeft = snapshot.child("spins").getValue(Integer.class);
                    Long lastBonus = snapshot.child("lastBonusClaimedAt").getValue(Long.class);

                    int finalCoins = coins != null ? coins.intValue() : 0;
                    updateUI(username, email, profilePhoto, 
                            finalCoins, 
                            tasks != null ? tasks : 0,
                            finalCoins, // Using coins as score for consistency
                            spinsLeft != null ? spinsLeft : 15,
                            lastBonus != null ? lastBonus : 0L);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load profile", error.toException());
                Toast.makeText(ProfileActivity.this, "Failed to load profile.", Toast.LENGTH_SHORT).show();
            }
        };
        firebaseManager.listenToUserData(userListener);
    }

    private void stopListeningForChanges() {
        firebaseManager.removeUserListener(userListener);
    }

    private void updateUI(String name, String email, String imageUrl, int points, int tasks, int score, int spinsLeft, long lastBonus) {
        int spinsPlayed = Math.max(0, 15 - spinsLeft);

        binding.tvProfileName.setText(name != null && !name.isEmpty() ? name : "ProTask Member");
        binding.tvProfileEmail.setText(email != null ? email : "");
        
        // Animated Counters
        animateTextCount(binding.tvProfileRewards, points, " pts");
        animateTextCount(binding.tvProfileScore, score, "");
        animateTextCount(binding.tvProfileTasks, tasks, "");
        
        binding.tvProfileSpins.setText(spinsPlayed + " / 15");
        binding.tvProfileStreak.setText("Daily streak: " + dailyStreakLabel(lastBonus));
        binding.tvLevelBadge.setText(levelFor(score));

        // Smooth image loading with Glide + Neon Circle
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_profile_avatar)
                    .error(R.drawable.ic_profile_avatar)
                    .circleCrop()
                    .into(binding.ivProfile);
        } else {
            binding.ivProfile.setImageResource(R.drawable.ic_profile_avatar);
        }
    }

    private void animateTextCount(TextView textView, int targetValue, String suffix) {
        int currentValue = 0;
        try {
            String text = textView.getText().toString().replace(suffix, "").trim();
            if (!text.isEmpty()) currentValue = Integer.parseInt(text);
        } catch (Exception ignored) {}

        if (currentValue == targetValue) return;

        ValueAnimator animator = ValueAnimator.ofInt(currentValue, targetValue);
        animator.setDuration(800);
        animator.addUpdateListener(animation -> 
            textView.setText(animation.getAnimatedValue().toString() + suffix));
        animator.start();
    }

    private String levelFor(int score) {
        if (score >= 5000) return "Elite";
        if (score >= 1500) return "Pro";
        if (score >= 500) return "Rising";
        return "Starter";
    }

    private String dailyStreakLabel(long lastBonus) {
        return lastBonus > 0L ? "active" : "1 day";
    }

    private void logout() {
        new FirebaseAuthHelper(this).signOut(() -> {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });
    }
}
