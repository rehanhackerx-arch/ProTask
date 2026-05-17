package com.example.myempty.protask;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setupTopHeader("Settings");

        bindSetting(R.id.rowNotifications, "Notification settings coming next.");
        bindSetting(R.id.rowSecurity, "Security settings coming next.");
        bindSetting(R.id.rowTheme, "Theme options coming next.");
        bindSetting(R.id.rowSupport, "Support center coming next.");
    }

    private void bindSetting(int viewId, String message) {
        TextView view = findViewById(viewId);
        if (view == null) {
            return;
        }
        bindPressAnimation(view);
        view.setOnClickListener(v -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }
}
