package com.example.myempty.protask;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class FirebaseAuthHelper {
    public static final int RC_GOOGLE_SIGN_IN = 6427;

    public interface AuthCallback {
        void onSuccess(@NonNull FirebaseUser user);
        void onFailure(@NonNull String message);
    }

    private final Context context;
    private final GoogleSignInClient googleSignInClient;

    public FirebaseAuthHelper(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.googleSignInClient = GoogleSignIn.getClient(context, buildGoogleOptions(context));
    }

    public boolean isFirebaseReady() {
        return !FirebaseApp.getApps(context).isEmpty();
    }

    public boolean isGoogleConfigured() {
        return getWebClientIdResId() != 0;
    }

    private int getWebClientIdResId() {
        return context.getResources().getIdentifier(
                "default_web_client_id",
                "string",
                context.getPackageName()
        );
    }

    public boolean isSignedIn() {
        return isFirebaseReady() && FirebaseAuth.getInstance().getCurrentUser() != null;
    }

    @Nullable
    public FirebaseUser getCurrentUser() {
        if (!isFirebaseReady()) {
            return null;
        }
        return FirebaseAuth.getInstance().getCurrentUser();
    }

    public Intent getSignInIntent() {
        return googleSignInClient.getSignInIntent();
    }

    public void handleSignInResult(@Nullable Intent data, @NonNull AuthCallback callback) {
        if (!isFirebaseReady()) {
            callback.onFailure(context.getString(R.string.google_login_config_missing));
            return;
        }

        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            if (account == null || account.getIdToken() == null) {
                callback.onFailure("Google account token was not returned.");
                return;
            }
            firebaseAuthWithGoogle(account, callback);
        } catch (ApiException exception) {
            callback.onFailure("Google sign-in failed: " + exception.getStatusCode());
        }
    }

    public void signOut(@NonNull Runnable onComplete) {
        googleSignInClient.signOut().addOnCompleteListener(task -> {
            if (isFirebaseReady()) {
                FirebaseAuth.getInstance().signOut();
            }
            onComplete.run();
        });
    }

    public void saveRewardSnapshot(int score, int walletPoints) {
        FirebaseUser user = getCurrentUser();
        if (user == null) {
            return;
        }
        Map<String, Object> values = new HashMap<>();
        values.put("score", score);
        values.put("walletPoints", walletPoints);
        values.put("updatedAt", System.currentTimeMillis());
        FirebaseDatabase.getInstance().getReference("users")
                .child(user.getUid())
                .updateChildren(values);
    }

    private void firebaseAuthWithGoogle(
            @NonNull GoogleSignInAccount account,
            @NonNull AuthCallback callback
    ) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        FirebaseAuth.getInstance().signInWithCredential(credential)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    if (user == null) {
                        callback.onFailure("Firebase user was not returned.");
                        return;
                    }
                    
                    // Check if user already exists to prevent overwriting balance
                    FirebaseDatabase.getInstance().getReference("users").child(user.getUid())
                            .get().addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    DataSnapshot snapshot = task.getResult();
                                    if (snapshot != null && snapshot.exists()) {
                                        // User exists, only update profile info
                                        updateExistingUser(user);
                                    } else {
                                        // New user, create full profile with defaults
                                        createNewUser(user);
                                    }
                                    callback.onSuccess(user);
                                } else {
                                    // Fallback to updateChildren if get() fails for some reason
                                    saveUserProfile(user);
                                    callback.onSuccess(user);
                                }
                            });
                })
                .addOnFailureListener(error -> callback.onFailure(error.getMessage() == null
                        ? "Firebase authentication failed."
                        : error.getMessage()));
    }

    private void createNewUser(@NonNull FirebaseUser user) {
        Map<String, Object> profile = new HashMap<>();
        profile.put("uid", user.getUid());
        profile.put("name", safe(user.getDisplayName(), "ProTask User"));
        profile.put("email", safe(user.getEmail(), ""));
        Uri photoUrl = user.getPhotoUrl();
        profile.put("profilePhoto", photoUrl == null ? "" : photoUrl.toString());
        profile.put("lastActive", System.currentTimeMillis());
        profile.put("status", "active");
        
        // Initialize defaults ONLY for new users
        profile.put("coins", 0);
        profile.put("spins", 15);
        profile.put("tasksCompleted", 0);
        profile.put("lastBonusClaimedAt", 0L);
        profile.put("captchaSolvedToday", 0);
        profile.put("lastCaptchaDay", "");

        FirebaseDatabase.getInstance().getReference("users")
                .child(user.getUid())
                .updateChildren(profile);
    }

    private void updateExistingUser(@NonNull FirebaseUser user) {
        Map<String, Object> profile = new HashMap<>();
        profile.put("name", safe(user.getDisplayName(), "ProTask User"));
        profile.put("email", safe(user.getEmail(), ""));
        Uri photoUrl = user.getPhotoUrl();
        profile.put("profilePhoto", photoUrl == null ? "" : photoUrl.toString());
        profile.put("lastActive", System.currentTimeMillis());
        
        // DO NOT overwrite coins, spins, or tasksCompleted here

        FirebaseDatabase.getInstance().getReference("users")
                .child(user.getUid())
                .updateChildren(profile);
    }

    private void saveUserProfile(@NonNull FirebaseUser user) {
        // Legacy fallback method - now calls createNewUser/updateExistingUser logic
        // but since we handle it in firebaseAuthWithGoogle, this is just for safety.
        updateExistingUser(user);
    }

    private GoogleSignInOptions buildGoogleOptions(@NonNull Context context) {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("625119739748-2shurcorldg92a4tb6glu2s3lmdm9u5k.apps.googleusercontent.com")
                .requestEmail()
                .build();
        return gso;
    }

    private String safe(@Nullable String value, @NonNull String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }
}
