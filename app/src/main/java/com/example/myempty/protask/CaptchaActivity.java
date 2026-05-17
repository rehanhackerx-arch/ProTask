package com.example.myempty.protask;

import android.animation.ObjectAnimator;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class CaptchaActivity extends BaseActivity {
    private static final int CAPTCHA_LENGTH = 6;
    private long captchaReward = 10;
    private static final int DAILY_TARGET = 10;
    private static final long CAPTCHA_TTL_MS = 45_000L;
    private static final String PREFS = "protask_captcha";
    private static final String KEY_DAY = "captcha_day";
    private static final String KEY_COUNT = "captcha_count";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();

    private TextView captchaText;
    private TextView walletPoints;
    private TextView rewardValue;
    private TextView dailyCount;
    private TextView progressLabel;
    private TextView typingProgressLabel;
    private TextView timerText;
    private TextView statusText;
    private TextView popupReward;
    private TextView popupMessage;
    private ProgressBar progressTyping;
    private ProgressBar progressDaily;
    private ProgressBar progressTimer;
    private View captchaCard;
    private View inputCard;
    private View popupOverlay;
    private View popupCard;
    private View shimmerView;
    private View refreshButton;
    private TextInputLayout inputLayout;
    private TextInputEditText inputField;
    private MaterialButton verifyButton;
    private MaterialButton watchAdButton;

    private CountDownTimer captchaTimer;
    private ToneGenerator toneGenerator;
    private Vibrator vibrator;
    private String currentCaptcha = "";
    private boolean rewardPopupVisible = false;
    private ValueEventListener rewardListener;

    private int solvedToday = 0;
    private ValueEventListener userListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_captcha);
        setupTopHeader("Captcha Typing");

        bindViews();
        loadRewardSettings();
        startListeningForUserData();
        bindInteractions();
        startEntryAnimations();
        refreshWalletPoints();
        updateDailyProgress(false);
        generateCaptcha();
    }

    private void startListeningForUserData() {
        userListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Integer count = snapshot.child("captchaSolvedToday").getValue(Integer.class);
                    String day = snapshot.child("lastCaptchaDay").getValue(String.class);
                    
                    if (day != null && day.equals(todayKey())) {
                        solvedToday = count != null ? count : 0;
                    } else {
                        solvedToday = 0;
                    }
                    updateDailyProgress(false);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        FirebaseManager.getInstance().listenToUserData(userListener);
    }

    private void loadRewardSettings() {
        rewardListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.hasChild("captchaReward")) {
                    captchaReward = snapshot.child("captchaReward").getValue(Long.class);
                }
                rewardValue.setText("+" + captchaReward);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        FirebaseManager.getInstance().listenToRewardSettings(rewardListener);
    }

    private void bindViews() {
        captchaText = findViewById(R.id.tvCaptcha);
        walletPoints = findViewById(R.id.tvPointsBalance);
        rewardValue = findViewById(R.id.tvRewardValue);
        dailyCount = findViewById(R.id.tvDailyCount);
        progressLabel = findViewById(R.id.tvProgressLabel);
        typingProgressLabel = findViewById(R.id.tvTypingProgress);
        timerText = findViewById(R.id.tvCaptchaTimer);
        statusText = findViewById(R.id.tvCaptchaStatus);
        popupReward = findViewById(R.id.tvPopupReward);
        popupMessage = findViewById(R.id.tvPopupMessage);
        progressTyping = findViewById(R.id.progressTyping);
        progressDaily = findViewById(R.id.progressDaily);
        progressTimer = findViewById(R.id.progressTimer);
        captchaCard = findViewById(R.id.cardCaptchaHero);
        inputCard = findViewById(R.id.cardInputSection);
        popupOverlay = findViewById(R.id.rewardPopupOverlay);
        popupCard = findViewById(R.id.rewardPopupCard);
        shimmerView = findViewById(R.id.viewCardShimmer);
        refreshButton = findViewById(R.id.btnRefreshCaptcha);
        inputLayout = findViewById(R.id.tilCaptchaInput);
        inputField = findViewById(R.id.etCaptchaInput);
        verifyButton = findViewById(R.id.btnVerifyCaptcha);
        watchAdButton = findViewById(R.id.btnWatchAdCaptcha);

        rewardValue.setText("+" + captchaReward);
        popupOverlay.setVisibility(View.GONE);
        toneGenerator = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
    }

    private void bindInteractions() {
        bindPressAnimation(refreshButton);
        bindPressAnimation(verifyButton);
        bindPressAnimation(watchAdButton);

        refreshButton.setOnClickListener(v -> {
            refreshButton.startAnimation(AnimationUtils.loadAnimation(this, R.anim.captcha_refresh_spin));
            playSoftFeedback();
            statusText.setText("Fresh captcha generated.");
            inputLayout.setError(null);
            generateCaptcha();
        });
        verifyButton.setOnClickListener(v -> verifyCaptcha());
        watchAdButton.setOnClickListener(v -> watchAdForBonus());

        inputField.setOnFocusChangeListener((view, hasFocus) -> {
            inputCard.animate()
                .scaleX(hasFocus ? 1.01f : 1f)
                .scaleY(hasFocus ? 1.01f : 1f)
                .setDuration(180L)
                .start();
        });
        inputField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateTypingProgress(s == null ? 0 : s.length());
                inputLayout.setError(null);
                if (s != null && s.length() > 0) {
                    statusText.setText("Typing in progress. Match the exact characters above.");
                } else {
                    statusText.setText("Type the glowing captcha exactly as shown.");
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void watchAdForBonus() {
        watchAdButton.setEnabled(false);
        watchAdButton.setText("Loading...");

        UnityAdsManager.getInstance().showRewardedAd(this, new UnityAdsManager.AdEventListener() {
            @Override
            public void onAdCompleted() {
                runOnUiThread(() -> {
                    long bonus = captchaReward / 2;
                    FirebaseManager.getInstance().addReward(CaptchaActivity.this, bonus);
                    watchAdButton.setEnabled(true);
                    watchAdButton.setText("Watch Ad for Bonus Points");
                    showRewardPopup("+" + bonus + " Bonus Points");
                    refreshWalletPoints();
                });
            }

            @Override
            public void onAdFailed(String error) {
                runOnUiThread(() -> {
                    watchAdButton.setEnabled(true);
                    watchAdButton.setText("Watch Ad for Bonus Points");
                    Toast.makeText(CaptchaActivity.this, "Ad failed: " + error, Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onAdStarted() {
            }
        });
    }

    private void startEntryAnimations() {
        findViewById(R.id.captchaRoot).startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in_up));
        captchaCard.startAnimation(AnimationUtils.loadAnimation(this, R.anim.captcha_card_fade_in));
        inputCard.startAnimation(AnimationUtils.loadAnimation(this, R.anim.captcha_card_fade_in));
        findViewById(R.id.cardRewardProgress).startAnimation(AnimationUtils.loadAnimation(this, R.anim.captcha_card_fade_in));
        shimmerView.startAnimation(AnimationUtils.loadAnimation(this, R.anim.captcha_shimmer));
        verifyButton.startAnimation(AnimationUtils.loadAnimation(this, R.anim.captcha_button_pulse));
        watchAdButton.startAnimation(AnimationUtils.loadAnimation(this, R.anim.captcha_card_fade_in));
    }

    private void generateCaptcha() {
        cancelTimer();
        currentCaptcha = buildCaptcha();
        captchaText.setText(spacedCaptcha(currentCaptcha));
        inputField.setText(null);
        updateTypingProgress(0);
        statusText.setText("Type the glowing captcha exactly as shown.");
        startCaptchaTimer();
    }

    private String buildCaptcha() {
        final String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < CAPTCHA_LENGTH; i++) {
            builder.append(chars.charAt(random.nextInt(chars.length())));
        }
        return builder.toString();
    }

    private String spacedCaptcha(String raw) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            if (i > 0) builder.append(' ');
            builder.append(raw.charAt(i));
        }
        return builder.toString();
    }

    private void verifyCaptcha() {
        String typed = sanitizeInput();
        if (typed.isEmpty()) {
            inputLayout.setError("Enter the captcha to continue.");
            runErrorAnimation("Captcha input is empty.");
            return;
        }
        if (!typed.equalsIgnoreCase(currentCaptcha)) {
            inputLayout.setError("Captcha does not match.");
            runErrorAnimation("Mismatch detected. Check the neon code and try again.");
            return;
        }

        cancelTimer();
        verifyButton.setEnabled(false);
        verifyButton.setText("Loading Ad...");

        UnityAdsManager.getInstance().showRewardedAd(this, new UnityAdsManager.AdEventListener() {
            @Override
            public void onAdCompleted() {
                runOnUiThread(() -> {
                    // Use FirebaseManager to add reward (updates 'coins' and 'tasksCompleted')
                    FirebaseManager.getInstance().addReward(CaptchaActivity.this, captchaReward);

                    incrementDailySolvedCount();
                    refreshWalletPoints();
                    updateDailyProgress(true);
                    playSuccessFeedback();
                    statusText.setText("Captcha verified. +" + captchaReward + " points added.");
                    showRewardPopup("+" + captchaReward + " Points");
                    verifyButton.setEnabled(true);
                    verifyButton.setText("Verify Captcha");
                    handler.postDelayed(CaptchaActivity.this::generateCaptcha, 1500L);
                });
            }

            @Override
            public void onAdFailed(String error) {
                runOnUiThread(() -> {
                    verifyButton.setEnabled(true);
                    verifyButton.setText("Verify Captcha");
                    Toast.makeText(CaptchaActivity.this, "Ad Required for Reward: " + error, Toast.LENGTH_SHORT).show();
                    runErrorAnimation("Ad failed or skipped. No reward granted.");
                    // Re-start timer so they can try again or wait for ad
                    startCaptchaTimer();
                });
            }

            @Override
            public void onAdStarted() {
                runOnUiThread(() -> {
                    verifyButton.setText("Watching...");
                });
            }
        });
    }

    private void startCaptchaTimer() {
        progressTimer.setProgress(100);
        captchaTimer = new CountDownTimer(CAPTCHA_TTL_MS, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = Math.max(1L, millisUntilFinished / 1000L);
                timerText.setText(seconds + "s");
                int progress = (int) ((millisUntilFinished * 100L) / CAPTCHA_TTL_MS);
                progressTimer.setProgress(progress);
            }

            @Override
            public void onFinish() {
                timerText.setText("0s");
                progressTimer.setProgress(0);
                inputLayout.setError("Captcha expired.");
                runErrorAnimation("Captcha expired. A new secure code is loading.");
                handler.postDelayed(() -> {
                    inputLayout.setError(null);
                    generateCaptcha();
                }, 900L);
            }
        }.start();
    }

    private void cancelTimer() {
        if (captchaTimer != null) {
            captchaTimer.cancel();
            captchaTimer = null;
        }
    }

    private void updateTypingProgress(int enteredChars) {
        int clamped = Math.min(enteredChars, CAPTCHA_LENGTH);
        int targetProgress = (clamped * 100) / CAPTCHA_LENGTH;
        typingProgressLabel.setText(clamped + "/" + CAPTCHA_LENGTH + " typed");
        animateProgress(progressTyping, targetProgress);
        verifyButton.setAlpha(clamped > 0 ? 1f : 0.88f);
    }

    private void refreshWalletPoints() {
        FirebaseManager.getInstance().listenToUserData(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Long coins = snapshot.child("coins").getValue(Long.class);
                    walletPoints.setText((coins != null ? coins : 0) + " pts");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        refreshWalletPoints();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    private void updateDailyProgress(boolean animate) {
        dailyCount.setText(solvedToday + "/" + DAILY_TARGET);
        progressLabel.setText(solvedToday + " of " + DAILY_TARGET + " complete");
        int targetProgress = Math.min(100, (solvedToday * 100) / DAILY_TARGET);
        if (animate) {
            animateProgress(progressDaily, targetProgress);
        } else {
            progressDaily.setProgress(targetProgress);
        }
    }

    private void incrementDailySolvedCount() {
        solvedToday++;
        FirebaseManager.getInstance().updateCaptchaSolved(solvedToday, todayKey());
    }

    private String sanitizeInput() {
        Editable editable = inputField.getText();
        return editable == null ? "" : editable.toString().replace(" ", "").trim();
    }

    private void showRewardPopup(String rewardText) {
        if (rewardPopupVisible) return;
        rewardPopupVisible = true;
        popupReward.setText(rewardText);
        popupMessage.setText("Captcha completed. Neon reward transferred to your wallet.");
        popupOverlay.setVisibility(View.VISIBLE);
        popupCard.startAnimation(AnimationUtils.loadAnimation(this, R.anim.captcha_popup_success));
        handler.postDelayed(() -> {
            popupOverlay.setVisibility(View.GONE);
            rewardPopupVisible = false;
        }, 1500L);
    }

    private void runErrorAnimation(String message) {
        statusText.setText(message);
        inputCard.startAnimation(AnimationUtils.loadAnimation(this, R.anim.captcha_error_shake));
        playErrorFeedback();
    }

    private void playSoftFeedback() {
        if (toneGenerator != null) {
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 90);
        }
        vibrate(18L);
    }

    private void playSuccessFeedback() {
        if (toneGenerator != null) {
            toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 180);
        }
        vibrate(65L);
    }

    private void playErrorFeedback() {
        if (toneGenerator != null) {
            toneGenerator.startTone(ToneGenerator.TONE_PROP_NACK, 180);
        }
        vibrate(40L);
    }

    private void vibrate(long durationMs) {
        if (vibrator == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibrator.vibrate(durationMs);
        }
    }

    private void animateProgress(ProgressBar progressBar, int targetProgress) {
        ObjectAnimator animator = ObjectAnimator.ofInt(progressBar, "progress", progressBar.getProgress(), targetProgress);
        animator.setDuration(220L);
        animator.start();
    }

    private String todayKey() {
        return new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshWalletPoints();
        updateDailyProgress(false);
    }

    @Override
    protected void onDestroy() {
        cancelTimer();
        handler.removeCallbacksAndMessages(null);
        if (toneGenerator != null) {
            toneGenerator.release();
            toneGenerator = null;
        }
        FirebaseManager.getInstance().removeRewardSettingsListener(rewardListener);
        FirebaseManager.getInstance().removeUserListener(userListener);
        super.onDestroy();
    }

}
