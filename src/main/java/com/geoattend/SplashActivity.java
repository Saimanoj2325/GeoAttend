package com.geoattend;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.geoattend.databinding.ActivitySplashBinding;
import com.geoattend.utils.SecurityManager;

public class SplashActivity extends AppCompatActivity {
    private ActivitySplashBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Mandatory Security Check on Startup
        if (SecurityManager.isUsbDebuggingEnabled(this)) {
            showSecurityWarning("USB Debugging Enabled", "GeoAttend cannot run while USB Debugging is active. Please disable it in Developer Options.");
            return;
        } else if (SecurityManager.isDeviceCompromised()) {
            showSecurityWarning("Device Compromised", "This device appears to be rooted or running a custom ROM. GeoAttend cannot run on insecure environments.");
            return;
        }

        // Micro-interactions: Subtle entrance animations
        binding.cardLogo.setAlpha(0f);
        binding.cardLogo.setScaleX(0.8f);
        binding.cardLogo.setScaleY(0.8f);
        
        binding.layoutTags.setAlpha(0f);
        binding.layoutTags.setTranslationY(20f);
        
        binding.btnGetStarted.setAlpha(0f);
        binding.btnGetStarted.setTranslationY(40f);

        binding.cardLogo.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(1200)
                .setStartDelay(300)
                .start();

        binding.layoutTags.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(1000)
                .setStartDelay(800)
                .start();

        binding.btnGetStarted.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(800)
                .setStartDelay(1500)
                .withEndAction(this::startPulseAnimation)
                .start();

        binding.btnGetStarted.setOnClickListener(v -> {
            startActivity(new Intent(SplashActivity.this, IntegrityCheckActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });
    }

    private void showSecurityWarning(String title, String message) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_security_warning, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvTitle = dialogView.findViewById(R.id.tv_warning_title);
        TextView tvMsg = dialogView.findViewById(R.id.tv_warning_message);
        tvTitle.setText(title);
        tvMsg.setText(message);

        dialogView.findViewById(R.id.btn_fix_settings).setOnClickListener(v -> {
            try {
                startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
            } catch (Exception e) {
                startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
            }
        });

        dialogView.findViewById(R.id.btn_exit_app).setOnClickListener(v -> finish());

        dialog.show();

        // Premium Entrance Animation
        dialogView.setAlpha(0f);
        dialogView.setTranslationY(100f);
        dialogView.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(500)
            .setInterpolator(new android.view.animation.OvershootInterpolator(0.7f))
            .withEndAction(() -> {
                View glow = dialogView.findViewById(R.id.warning_icon_glow);
                if (glow != null) {
                    android.animation.ObjectAnimator pulse = android.animation.ObjectAnimator.ofFloat(glow, "alpha", 0.1f, 0.4f, 0.1f);
                    pulse.setDuration(1500);
                    pulse.setRepeatCount(android.animation.ValueAnimator.INFINITE);
                    pulse.start();
                }
            })
            .start();
    }

    private void startPulseAnimation() {
        android.animation.ObjectAnimator scaleX = android.animation.ObjectAnimator.ofFloat(binding.cardLogo, "scaleX", 1f, 1.05f, 1f);
        android.animation.ObjectAnimator scaleY = android.animation.ObjectAnimator.ofFloat(binding.cardLogo, "scaleY", 1f, 1.05f, 1f);
        
        scaleX.setDuration(2000);
        scaleX.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        scaleY.setDuration(2000);
        scaleY.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        
        scaleX.start();
        scaleY.start();
    }
}
