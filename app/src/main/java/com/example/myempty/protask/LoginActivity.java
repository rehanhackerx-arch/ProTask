package com.example.myempty.protask;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.example.myempty.protask.databinding.ActivityLoginBinding;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends BaseActivity {
    private ActivityLoginBinding binding;
    private FirebaseAuthHelper authHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        authHelper = new FirebaseAuthHelper(this);

        if (authHelper.isSignedIn()) {
            goHome();
            return;
        }

        animateLoginCard();
        bindGoogleButton();
        if (!authHelper.isFirebaseReady() || !authHelper.isGoogleConfigured()) {
            binding.tvLoginStatus.setText(R.string.google_login_config_missing);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != FirebaseAuthHelper.RC_GOOGLE_SIGN_IN) {
            return;
        }

        binding.tvLoginStatus.setText("Checking Google account...");
        authHelper.handleSignInResult(data, new FirebaseAuthHelper.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                binding.tvLoginStatus.setText("Signed in as " + user.getEmail());
                goHome();
            }

            @Override
            public void onFailure(String message) {
                binding.tvLoginStatus.setText(message);
                Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void bindGoogleButton() {
        binding.btnGoogleSignIn.setOnTouchListener((target, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                target.animate().scaleX(0.97f).scaleY(0.97f).setDuration(90).start();
            } else if (event.getAction() == MotionEvent.ACTION_UP
                    || event.getAction() == MotionEvent.ACTION_CANCEL) {
                target.animate().scaleX(1f).scaleY(1f).setDuration(140).start();
            }
            return false;
        });
        binding.btnGoogleSignIn.setOnClickListener(view -> {
            if (!authHelper.isFirebaseReady() || !authHelper.isGoogleConfigured()) {
                binding.tvLoginStatus.setText(R.string.google_login_config_missing);
                return;
            }
            binding.tvLoginStatus.setText("Opening Google Sign-In...");
            startActivityForResult(authHelper.getSignInIntent(), FirebaseAuthHelper.RC_GOOGLE_SIGN_IN);
        });
    }

    private void animateLoginCard() {
        binding.loginCard.setAlpha(0f);
        binding.loginCard.setTranslationY(42f);
        binding.loginCard.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(520)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        ObjectAnimator logoPulse = ObjectAnimator.ofFloat(binding.loginLogo, View.ROTATION, -3f, 3f, 0f);
        logoPulse.setDuration(1100);
        logoPulse.setInterpolator(new AccelerateDecelerateInterpolator());
        logoPulse.start();
    }

    private void goHome() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}
