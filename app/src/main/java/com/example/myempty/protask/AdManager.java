package com.example.myempty.protask;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.ads.FullScreenContentCallback;

public class AdManager {
    private static final String TAG = "AdManager";
    
    // Test Rewarded Ad Unit ID
    private static final String REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917";
    
    private static AdManager instance;
    private RewardedAd rewardedAd;
    private boolean isLoading = false;
    private int retryCount = 0;
    private static final int MAX_RETRY_COUNT = 5;

    public interface AdCallback {
        void onAdReward();
        void onAdFailed(String message);
        void onAdClosed();
        void onAdLoading();
    }

    private AdManager() {}

    public static synchronized AdManager getInstance() {
        if (instance == null) {
            instance = new AdManager();
        }
        return instance;
    }

    public void init(Context context) {
        MobileAds.initialize(context, initializationStatus -> {
            Log.d(TAG, "AdMob Initialized");
            loadRewardedAd(context);
        });
    }

    public void loadRewardedAd(Context context) {
        if (isLoading || rewardedAd != null) return;

        isLoading = true;
        AdRequest adRequest = new AdRequest.Builder().build();
        
        RewardedAd.load(context, REWARDED_AD_UNIT_ID, adRequest, new RewardedAdLoadCallback() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                Log.e(TAG, "Rewarded Ad failed to load: " + loadAdError.getMessage());
                rewardedAd = null;
                isLoading = false;
                
                // Retry logic with exponential backoff (simplified)
                if (retryCount < MAX_RETRY_COUNT) {
                    retryCount++;
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> 
                        loadRewardedAd(context), 5000 * retryCount);
                }
            }

            @Override
            public void onAdLoaded(@NonNull RewardedAd ad) {
                Log.d(TAG, "Rewarded Ad was loaded.");
                rewardedAd = ad;
                isLoading = false;
                retryCount = 0;
                
                rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdDismissedFullScreenContent() {
                        Log.d(TAG, "Ad dismissed fullscreen content.");
                        rewardedAd = null;
                        // Preload next ad immediately
                        loadRewardedAd(context);
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(@NonNull com.google.android.gms.ads.AdError adError) {
                        Log.e(TAG, "Ad failed to show fullscreen content: " + adError.getMessage());
                        rewardedAd = null;
                        loadRewardedAd(context);
                    }
                });
            }
        });
    }

    public boolean isAdReady() {
        return rewardedAd != null;
    }

    public void showRewardedAd(Activity activity, AdCallback callback) {
        if (rewardedAd != null) {
            rewardedAd.show(activity, new OnUserEarnedRewardListener() {
                @Override
                public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
                    Log.d(TAG, "User earned reward: " + rewardItem.getAmount());
                    if (callback != null) callback.onAdReward();
                }
            });
            
            rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Ad dismissed.");
                    rewardedAd = null;
                    if (callback != null) callback.onAdClosed();
                    loadRewardedAd(activity.getApplicationContext());
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull com.google.android.gms.ads.AdError adError) {
                    Log.e(TAG, "Ad failed to show: " + adError.getMessage());
                    rewardedAd = null;
                    if (callback != null) callback.onAdFailed(adError.getMessage());
                    loadRewardedAd(activity.getApplicationContext());
                }

                @Override
                public void onAdShowedFullScreenContent() {
                    Log.d(TAG, "Ad showed fullscreen content.");
                }
            });
        } else {
            Log.d(TAG, "The rewarded ad wasn't ready yet.");
            if (callback != null) {
                if (isLoading) {
                    callback.onAdLoading();
                } else {
                    callback.onAdFailed("Ad not ready. Please try again later.");
                    loadRewardedAd(activity.getApplicationContext());
                }
            }
        }
    }
}
