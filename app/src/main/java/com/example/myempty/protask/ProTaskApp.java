package com.example.myempty.protask;

import android.app.Application;

public class ProTaskApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        UnityAdsManager.getInstance().initialize(this);
    }
}
