package com.geoattend;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import com.geoattend.databinding.ActivitySplashBinding;

public class SplashActivity extends AppCompatActivity {
    private ActivitySplashBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Micro-interactions: Subtle entrance animations
        binding.ivPremiumLogo.setAlpha(0f);
        binding.ivPremiumLogo.setScaleX(0.8f);
        binding.ivPremiumLogo.setScaleY(0.8f);
        
        binding.logoContainer.setAlpha(0f);
        binding.logoContainer.setTranslationY(20f);
        
        binding.btnGetStarted.setAlpha(0f);
        binding.btnGetStarted.setTranslationY(40f);

        binding.ivPremiumLogo.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(1200)
                .setStartDelay(300)
                .start();

        binding.logoContainer.animate()
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
                .start();

        binding.btnGetStarted.setOnClickListener(v -> {
            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });
    }
}
