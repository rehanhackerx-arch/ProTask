package com.example.myempty.protask;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    protected void setupTopHeader(String title) {
        setupTopHeader(title, 0, null, 0, null);
    }

    protected void setupTopHeader(
            String title,
            @DrawableRes int primaryIconRes,
            @Nullable View.OnClickListener primaryClickListener
    ) {
        setupTopHeader(title, primaryIconRes, primaryClickListener, 0, null);
    }

    protected void setupTopHeader(
            String title,
            @DrawableRes int primaryIconRes,
            @Nullable View.OnClickListener primaryClickListener,
            @DrawableRes int secondaryIconRes,
            @Nullable View.OnClickListener secondaryClickListener
    ) {
        View header = findViewById(R.id.topHeaderContainer);
        TextView titleView = findViewById(R.id.topHeaderTitle);
        ImageView backView = findViewById(R.id.topHeaderBack);
        ImageView primaryView = findViewById(R.id.topHeaderPrimaryAction);
        ImageView secondaryView = findViewById(R.id.topHeaderSecondaryAction);

        if (header == null || titleView == null || backView == null) {
            return;
        }

        titleView.setText(title);
        applyHeaderInsets(header);
        bindPressAnimation(backView);
        backView.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        configureAction(primaryView, primaryIconRes, primaryClickListener);
        configureAction(secondaryView, secondaryIconRes, secondaryClickListener);
    }

    protected void launchScreen(Intent intent) {
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    protected void launchScreen(Class<?> destination) {
        launchScreen(new Intent(this, destination));
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    protected void bindPressAnimation(View view) {
        if (view == null) {
            return;
        }
        view.setOnTouchListener((target, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                target.animate().scaleX(0.94f).scaleY(0.94f).setDuration(90).start();
            } else if (event.getAction() == MotionEvent.ACTION_UP
                    || event.getAction() == MotionEvent.ACTION_CANCEL) {
                target.animate().scaleX(1f).scaleY(1f).setDuration(140).start();
            }
            return false;
        });
    }

    protected void setupPremiumBottomNav(int activeItemId) {
        View home = findViewById(R.id.navHome);
        View wallet = findViewById(R.id.navWallet);
        View withdraw = findViewById(R.id.navWithdraw);
        View refer = findViewById(R.id.navRefer);

        selectPremiumNavItem(activeItemId);
        bindPremiumNavClick(home, R.id.navHome, MainActivity.class);
        bindPremiumNavClick(wallet, R.id.navWallet, WalletActivity.class);
        bindPremiumNavClick(withdraw, R.id.navWithdraw, WithdrawActivity.class);
        bindPremiumNavClick(refer, R.id.navRefer, ReferEarnActivity.class);
    }

    private void bindPremiumNavClick(View item, int itemId, Class<?> destination) {
        if (item == null) {
            return;
        }
        item.setOnTouchListener((target, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                target.animate().scaleX(0.96f).scaleY(0.96f).setDuration(90).start();
            } else if (event.getAction() == MotionEvent.ACTION_UP
                    || event.getAction() == MotionEvent.ACTION_CANCEL) {
                target.animate().scaleX(target.isSelected() ? 1.08f : 1f)
                        .scaleY(target.isSelected() ? 1.08f : 1f)
                        .setDuration(140)
                        .start();
            }
            return false;
        });
        item.setOnClickListener(view -> {
            selectPremiumNavItem(itemId);
            if (isCurrentDestination(destination)) {
                Toast.makeText(this, "You are on " + navTitle(itemId), Toast.LENGTH_SHORT).show();
                return;
            }
            launchScreen(destination);
        });
    }

    private boolean isCurrentDestination(Class<?> destination) {
        return destination.isAssignableFrom(getClass());
    }

    private String navTitle(int itemId) {
        if (itemId == R.id.navWallet) return "Wallet";
        if (itemId == R.id.navWithdraw) return "Withdraw";
        if (itemId == R.id.navRefer) return "Refer";
        return "Home";
    }

    private void selectPremiumNavItem(int activeItemId) {
        int[] itemIds = {R.id.navHome, R.id.navWallet, R.id.navWithdraw, R.id.navRefer};
        int[] iconIds = {R.id.navHomeIcon, R.id.navWalletIcon, R.id.navWithdrawIcon, R.id.navReferIcon};
        int[] labelIds = {R.id.navHomeLabel, R.id.navWalletLabel, R.id.navWithdrawLabel, R.id.navReferLabel};
        int[] indicatorIds = {R.id.navHomeIndicator, R.id.navWalletIndicator, R.id.navWithdrawIndicator, R.id.navReferIndicator};

        for (int i = 0; i < itemIds.length; i++) {
            View item = findViewById(itemIds[i]);
            ImageView icon = findViewById(iconIds[i]);
            TextView label = findViewById(labelIds[i]);
            View indicator = findViewById(indicatorIds[i]);
            boolean selected = itemIds[i] == activeItemId;

            if (item != null) {
                item.setSelected(selected);
                item.animate()
                        .scaleX(selected ? 1.08f : 1f)
                        .scaleY(selected ? 1.08f : 1f)
                        .setDuration(220)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .start();
            }
            if (icon != null) {
                icon.setSelected(selected);
                icon.setImageTintList(ColorStateList.valueOf(Color.parseColor(selected ? "#FFFFFF" : "#9CA3C7")));
                icon.setAlpha(selected ? 1f : 0.72f);
            }
            if (label != null) {
                label.setSelected(selected);
                label.setTextColor(Color.parseColor(selected ? "#FFFFFF" : "#B8BEDA"));
                label.setShadowLayer(selected ? 12f : 3f, 0f, 0f,
                        Color.parseColor(selected ? "#CCB989FF" : "#227B2CFF"));
            }
            if (indicator != null) {
                indicator.setSelected(selected);
                indicator.setVisibility(selected ? View.VISIBLE : View.INVISIBLE);
                indicator.animate()
                        .alpha(selected ? 1f : 0f)
                        .scaleX(selected ? 1f : 0.35f)
                        .setDuration(220)
                        .start();
            }
        }
        movePremiumNavGlow(activeItemId);
    }

    private void movePremiumNavGlow(int activeItemId) {
        View glow = findViewById(R.id.navGlow);
        View selectedItem = findViewById(activeItemId);
        if (glow == null || selectedItem == null) {
            return;
        }
        selectedItem.post(() -> {
            float targetX = selectedItem.getX() + (selectedItem.getWidth() - glow.getWidth()) / 2f;
            glow.animate()
                    .x(targetX)
                    .alpha(0.9f)
                    .setDuration(260)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
        });
    }

    private void configureAction(
            @Nullable ImageView actionView,
            @DrawableRes int iconRes,
            @Nullable View.OnClickListener clickListener
    ) {
        if (actionView == null) {
            return;
        }
        if (iconRes == 0 || clickListener == null) {
            actionView.setVisibility(View.GONE);
            actionView.setOnClickListener(null);
            return;
        }

        actionView.setVisibility(View.VISIBLE);
        actionView.setImageResource(iconRes);
        bindPressAnimation(actionView);
        actionView.setOnClickListener(clickListener);
    }

    private void applyHeaderInsets(View header) {
        final int paddingLeft = header.getPaddingLeft();
        final int paddingTop = header.getPaddingTop();
        final int paddingRight = header.getPaddingRight();
        final int paddingBottom = header.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(header, (view, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars());
            view.setPadding(
                    paddingLeft,
                    paddingTop + insets.top,
                    paddingRight,
                    paddingBottom
            );
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(header);
    }
}
