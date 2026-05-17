package com.example.myempty.protask;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.AccelerateDecelerateInterpolator;

import com.example.myempty.protask.databinding.ActivitySplashBinding;

public class SplashActivity extends BaseActivity {
    private ActivitySplashBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ObjectAnimator pulse = ObjectAnimator.ofFloat(binding.splashLogo, "scaleX", 0.92f, 1.08f, 1f);
        pulse.setDuration(900);
        pulse.setInterpolator(new AccelerateDecelerateInterpolator());
        pulse.start();
        ObjectAnimator.ofFloat(binding.splashLogo, "scaleY", 0.92f, 1.08f, 1f).setDuration(900).start();

        new Handler(Looper.getMainLooper()).postDelayed(this::routeNext, 950);
    }

    private void routeNext() {
        FirebaseAuthHelper authHelper = new FirebaseAuthHelper(this);
        Intent intent = new Intent(this, authHelper.isSignedIn() ? MainActivity.class : LoginActivity.class);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}
