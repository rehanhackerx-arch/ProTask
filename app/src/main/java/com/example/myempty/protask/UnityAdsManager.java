package com.example.myempty.protask;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.unity3d.ads.IUnityAdsInitializationListener;
import com.unity3d.ads.IUnityAdsLoadListener;
import com.unity3d.ads.IUnityAdsShowListener;
import com.unity3d.ads.UnityAds;
import com.unity3d.ads.UnityAdsShowOptions;

/**
 * Professional Centralized Unity Ads Manager
 * Handles Initialization, Preloading, Showing, and Auto-reloading of Rewarded Ads.
 */
public class UnityAdsManager {
    private static final String TAG = "UnityAdsManager";
    private static final String GAME_ID = "6084497";
    private static final boolean TEST_MODE = false;
    private static final String REWARDED_PLACEMENT_ID = "Rewarded_Android";

    private static UnityAdsManager instance;
    private boolean isInitialized = false;
    private boolean isAdLoading = false;
    private boolean isAdReady = false;
    private int retryCount = 0;
    private static final int MAX_RETRY_COUNT = 5;

    private ProgressDialog loadingDialog;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private UnityAdsManager() {}

    public static synchronized UnityAdsManager getInstance() {
        if (instance == null) {
            instance = new UnityAdsManager();
        }
        return instance;
    }

    /**
     * Initialize Unity Ads SDK
     */
    public void initialize(Context context) {
        if (isInitialized) {
            Log.d(TAG, "Unity Ads already initialized.");
            if (!isAdReady && !isAdLoading) {
                loadRewardedAd();
            }
            return;
        }

        Log.d(TAG, "Initializing Unity Ads with Game ID: " + GAME_ID);
        UnityAds.initialize(context.getApplicationContext(), GAME_ID, TEST_MODE, new IUnityAdsInitializationListener() {
            @Override
            public void onInitializationComplete() {
                Log.d(TAG, "Unity Ads Initialization Complete");
                isInitialized = true;
                retryCount = 0;
                loadRewardedAd();
            }

            @Override
            public void onInitializationFailed(UnityAds.UnityAdsInitializationError error, String message) {
                Log.e(TAG, "Unity Ads Initialization Failed: [" + error + "] " + message);
                isInitialized = false;
                handleInitializationFailure(context);
            }
        });
    }

    private void handleInitializationFailure(Context context) {
        if (retryCount < MAX_RETRY_COUNT) {
            retryCount++;
            long delay = (long) Math.pow(2, retryCount) * 1000; // Exponential backoff
            Log.d(TAG, "Retrying initialization in " + delay + "ms (Attempt " + retryCount + ")");
            mainHandler.postDelayed(() -> initialize(context), delay);
        }
    }

    /**
     * Check if ad is ready to show
     */
    public boolean isAdReady() {
        return isInitialized && isAdReady;
    }

    /**
     * Preload Rewarded Ad
     */
    public void loadRewardedAd() {
        if (!isInitialized) {
            Log.w(TAG, "Cannot load ad: Unity Ads not initialized.");
            return;
        }

        if (isAdLoading || isAdReady) {
            Log.d(TAG, "Ad already loading or ready.");
            return;
        }

        isAdLoading = true;
        Log.d(TAG, "Loading Rewarded Ad: " + REWARDED_PLACEMENT_ID);
        UnityAds.load(REWARDED_PLACEMENT_ID, new IUnityAdsLoadListener() {
            @Override
            public void onUnityAdsAdLoaded(String placementId) {
                Log.d(TAG, "Ad successfully loaded: " + placementId);
                isAdReady = true;
                isAdLoading = false;
                dismissLoadingDialog();
            }

            @Override
            public void onUnityAdsFailedToLoad(String placementId, UnityAds.UnityAdsLoadError error, String message) {
                Log.e(TAG, "Ad failed to load: [" + error + "] " + message);
                isAdReady = false;
                isAdLoading = false;
                dismissLoadingDialog();
                
                // Auto-retry with backoff
                mainHandler.postDelayed(() -> loadRewardedAd(), 15000);
            }
        });
    }

    public interface AdEventListener {
        void onAdCompleted();
        void onAdFailed(String error);
        void onAdStarted();
    }

    /**
     * Show Rewarded Ad with optional loading dialog if not ready
     */
    public void showRewardedAd(Activity activity, AdEventListener listener) {
        if (!isInitialized) {
            Log.w(TAG, "Show requested but not initialized. Initializing now.");
            initialize(activity);
            if (listener != null) listener.onAdFailed("Unity Ads initializing. Try again in a moment.");
            return;
        }

        if (!isAdReady) {
            Log.d(TAG, "Ad not ready. Showing loading dialog and requesting load.");
            showLoadingDialog(activity);
            loadRewardedAd();
            
            // Wait up to 5 seconds for ad to load
            mainHandler.postDelayed(() -> {
                if (!isAdReady) {
                    dismissLoadingDialog();
                    if (listener != null) listener.onAdFailed("Ad is taking too long to load. Please check your internet.");
                } else {
                    // Ad became ready during wait, show it
                    showRewardedAdInternal(activity, listener);
                }
            }, 5000);
            return;
        }

        showRewardedAdInternal(activity, listener);
    }

    private void showRewardedAdInternal(Activity activity, AdEventListener listener) {
        dismissLoadingDialog();
        isAdReady = false; // Reset state
        
        Log.d(TAG, "Showing Rewarded Ad: " + REWARDED_PLACEMENT_ID);
        UnityAds.show(activity, REWARDED_PLACEMENT_ID, new UnityAdsShowOptions(), new IUnityAdsShowListener() {
            @Override
            public void onUnityAdsShowFailure(String placementId, UnityAds.UnityAdsShowError error, String message) {
                Log.e(TAG, "Ad failed to show: [" + error + "] " + message);
                if (listener != null) listener.onAdFailed(message);
                loadRewardedAd(); // Preload next
            }

            @Override
            public void onUnityAdsShowStart(String placementId) {
                Log.d(TAG, "Ad playback started: " + placementId);
                if (listener != null) listener.onAdStarted();
            }

            @Override
            public void onUnityAdsShowClick(String placementId) {
                Log.d(TAG, "Ad clicked: " + placementId);
            }

            @Override
            public void onUnityAdsShowComplete(String placementId, UnityAds.UnityAdsShowCompletionState state) {
                Log.d(TAG, "Ad playback completed: " + placementId + " with state: " + state);
                if (state == UnityAds.UnityAdsShowCompletionState.COMPLETED) {
                    if (listener != null) listener.onAdCompleted();
                } else {
                    if (listener != null) listener.onAdFailed("Ad skipped or failed to complete.");
                }
                loadRewardedAd(); // Preload next immediately
            }
        });
    }

    private void showLoadingDialog(Activity activity) {
        try {
            if (loadingDialog != null && loadingDialog.isShowing()) {
                loadingDialog.dismiss();
            }
            loadingDialog = new ProgressDialog(activity);
            loadingDialog.setMessage("Loading Video Ad...");
            loadingDialog.setCancelable(false);
            loadingDialog.show();
        } catch (Exception e) {
            Log.e(TAG, "Failed to show loading dialog", e);
        }
    }

    private void dismissLoadingDialog() {
        mainHandler.post(() -> {
            try {
                if (loadingDialog != null && loadingDialog.isShowing()) {
                    loadingDialog.dismiss();
                }
                loadingDialog = null;
            } catch (Exception e) {
                Log.e(TAG, "Failed to dismiss loading dialog", e);
                loadingDialog = null;
            }
        });
    }
}
