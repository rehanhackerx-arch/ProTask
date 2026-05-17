package com.example.myempty.protask;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

public class ReferEarnActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_refer_earn);
        setupTopHeader("Refer & Earn");
        setupPremiumBottomNav(R.id.navRefer);

        TextView shareButton = findViewById(R.id.btnShareReferral);
        bindPressAnimation(shareButton);
        shareButton.setOnClickListener(v -> shareReferral());
    }

    private void shareReferral() {
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.setType("text/plain");
        sendIntent.putExtra(Intent.EXTRA_TEXT, "Join ProTask and earn rewards daily. Use referral code PROTASK250.");
        startActivity(Intent.createChooser(sendIntent, "Refer & Earn"));
    }
}
