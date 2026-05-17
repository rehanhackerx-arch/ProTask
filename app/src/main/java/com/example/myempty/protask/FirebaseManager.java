package com.example.myempty.protask;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class FirebaseManager {
    private static final String TAG = "FirebaseManager";
    private static FirebaseManager instance;
    private final DatabaseReference usersRef;
    private final DatabaseReference rewardSettingsRef;
    private final DatabaseReference withdrawalRequestsRef;
    private final FirebaseAuth auth;

    private FirebaseManager() {
        auth = FirebaseAuth.getInstance();
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        usersRef = db.getReference("users");
        rewardSettingsRef = db.getReference("rewardSettings");
        withdrawalRequestsRef = db.getReference("withdrawRequests");
        Log.d(TAG, "Firebase connected and paths initialized");
    }

    public static synchronized FirebaseManager getInstance() {
        if (instance == null) {
            instance = new FirebaseManager();
        }
        return instance;
    }

    public String getUid() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    /**
     * Updates user coins and stats in Firebase.
     */
    public void addReward(Context context, long amount) {
        String uid = getUid();
        if (uid == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("coins", ServerValue.increment(amount));
        updates.put("tasksCompleted", ServerValue.increment(1));
        updates.put("lastActive", ServerValue.TIMESTAMP);

        usersRef.child(uid).updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Coins updated: +" + amount);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Coins update failed", e));
    }

    public void resetSpinsIfNeeded() {
        String uid = getUid();
        if (uid == null) return;

        usersRef.child(uid).get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                Long lastReset = snapshot.child("lastSpinReset").getValue(Long.class);
                long currentTime = System.currentTimeMillis();
                
                if (lastReset == null || (currentTime - lastReset) >= (24 * 60 * 60 * 1000)) {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("spins", 15);
                    updates.put("lastSpinReset", ServerValue.TIMESTAMP);
                    usersRef.child(uid).updateChildren(updates);
                    Log.d(TAG, "Spins reset to 15");
                }
            }
        });
    }

    public void updateLastBonusClaimed() {
        String uid = getUid();
        if (uid == null) return;
        usersRef.child(uid).child("lastBonusClaimedAt").setValue(ServerValue.TIMESTAMP);
    }

    public void updateCaptchaSolved(int count, String day) {
        String uid = getUid();
        if (uid == null) return;
        Map<String, Object> updates = new HashMap<>();
        updates.put("captchaSolvedToday", count);
        updates.put("lastCaptchaDay", day);
        usersRef.child(uid).updateChildren(updates);
    }

    /**
     * Logs a withdrawal request in Firebase.
     */
    public void submitWithdrawalRequest(String method, String paymentDetail, long amount, double inrAmount, OnCompleteListener<Void> onComplete) {
        String uid = getUid();
        if (uid == null) return;

        String requestId = withdrawalRequestsRef.push().getKey();
        if (requestId == null) return;

        Map<String, Object> requestData = new HashMap<>();
        FirebaseUser user = getCurrentUser();
        requestData.put("uid", uid);
        requestData.put("username", user != null ? user.getDisplayName() : "Unknown");
        requestData.put("email", user != null ? user.getEmail() : "Unknown");
        requestData.put("method", method);
        requestData.put("number", paymentDetail);
        requestData.put("amount", amount);
        requestData.put("timestamp", ServerValue.TIMESTAMP);
        requestData.put("status", "pending");

        withdrawalRequestsRef.child(requestId).setValue(requestData).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Map<String, Object> updates = new HashMap<>();
                updates.put("coins", ServerValue.increment(-amount));
                usersRef.child(uid).updateChildren(updates).addOnCompleteListener(onComplete);
            } else {
                if (onComplete != null) {
                    onComplete.onComplete(task);
                }
            }
        });
    }

    /**
     * Updates spin count in Firebase.
     */
    public void updateSpin(int spinsLeft) {
        String uid = getUid();
        if (uid == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("spins", spinsLeft);
        updates.put("lastActive", ServerValue.TIMESTAMP);

        usersRef.child(uid).updateChildren(updates)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Spins updated"))
                .addOnFailureListener(e -> Log.e(TAG, "Spins update failed", e));
    }

    public void listenToRewardSettings(ValueEventListener listener) {
        rewardSettingsRef.addValueEventListener(listener);
        Log.d(TAG, "Reward synced listener added");
    }

    public void removeRewardSettingsListener(ValueEventListener listener) {
        if (listener != null) {
            rewardSettingsRef.removeEventListener(listener);
        }
    }

    public void listenToUserData(ValueEventListener listener) {
        String uid = getUid();
        if (uid == null || listener == null) return;
        usersRef.child(uid).addValueEventListener(listener);
        Log.d(TAG, "User data loaded listener added for: " + uid);
    }

    public void removeUserListener(ValueEventListener listener) {
        String uid = getUid();
        if (uid != null && listener != null) {
            usersRef.child(uid).removeEventListener(listener);
        }
    }

    /**
     * Syncs Google Profile data to Firebase Realtime Database.
     */
    public void syncGoogleProfile() {
        FirebaseUser user = getCurrentUser();
        if (user == null) return;

        usersRef.child(user.getUid()).get().addOnSuccessListener(snapshot -> {
            Map<String, Object> updates = new HashMap<>();
            updates.put("name", user.getDisplayName());
            updates.put("username", user.getDisplayName());
            updates.put("email", user.getEmail());
            if (user.getPhotoUrl() != null) {
                updates.put("profilePhoto", user.getPhotoUrl().toString());
            }
            updates.put("lastActive", ServerValue.TIMESTAMP);

            if (!snapshot.exists()) {
                // Initialize default values for new user
                updates.put("coins", 0);
                updates.put("tasksCompleted", 0);
                updates.put("spins", 15);
                updates.put("lastSpinReset", ServerValue.TIMESTAMP);
            }

            usersRef.child(user.getUid()).updateChildren(updates)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Profile synced with Firebase DB"))
                    .addOnFailureListener(e -> Log.e(TAG, "Profile sync failed", e));
        });
    }
}
