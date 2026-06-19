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
