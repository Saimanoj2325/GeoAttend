package com.geoattend;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.geoattend.admin.AdminDashboardActivity;
import com.geoattend.employee.EmployeeDashboardActivity;
import com.geoattend.employee.FaceEnrollmentActivity;
import com.geoattend.model.User;
import com.geoattend.utils.DeviceBindingManager;
import com.geoattend.utils.FirebaseHelper;
import com.geoattend.utils.SecurityUtils;
import com.geoattend.utils.SessionManager;
import com.google.firebase.auth.FirebaseAuth;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LoginActivity extends AppCompatActivity {
    private EditText etEmail, etPassword;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        progressBar = findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.GONE);

        findViewById(R.id.btn_sign_in).setOnClickListener(v -> loginUser());

        findViewById(R.id.btn_go_to_register).setOnClickListener(v -> 
            startActivity(new Intent(this, RegisterActivity.class))
        );

        setupPasswordVisibilityToggle();

        if (FirebaseHelper.getAuth().getCurrentUser() != null) {
            checkUserStatusAndNavigate(FirebaseHelper.getAuth().getCurrentUser().getUid());
        }
        
        startLogoAnimation();
    }

    private void startLogoAnimation() {
        View logo = findViewById(R.id.card_logo_small);
        android.animation.ObjectAnimator breatheX = android.animation.ObjectAnimator.ofFloat(logo, "scaleX", 1f, 1.1f, 1f);
        android.animation.ObjectAnimator breatheY = android.animation.ObjectAnimator.ofFloat(logo, "scaleY", 1f, 1.1f, 1f);
        breatheX.setDuration(3000);
        breatheX.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        breatheY.setDuration(3000);
        breatheY.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        breatheX.start();
        breatheY.start();
    }

    private void setupPasswordVisibilityToggle() {
        android.widget.ImageView ivEye = findViewById(R.id.iv_eye_password);
        ivEye.setOnClickListener(v -> {
            if (etPassword.getTransformationMethod() instanceof android.text.method.PasswordTransformationMethod) {
                etPassword.setTransformationMethod(android.text.method.HideReturnsTransformationMethod.getInstance());
                ivEye.setImageResource(android.R.drawable.ic_menu_view); // Placeholder for open eye
            } else {
                etPassword.setTransformationMethod(android.text.method.PasswordTransformationMethod.getInstance());
                ivEye.setImageResource(R.drawable.ic_eye);
            }
            etPassword.setSelection(etPassword.getText().length());
        });
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Enter credentials", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        // --- ADMIN TESTING BYPASS ---
        if (email.equals("admin") && password.equals("admin")) {
            progressBar.setVisibility(View.GONE);
            startActivity(new Intent(this, AdminDashboardActivity.class));
            finish();
            return;
        }
        // ----------------------------

        FirebaseHelper.getAuth().signInWithEmailAndPassword(email, password)
            .addOnSuccessListener(authResult -> {
                checkUserStatusAndNavigate(authResult.getUser().getUid());
            })
            .addOnFailureListener(e -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Login Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
    }

    private void checkUserStatusAndNavigate(String uid) {
        progressBar.setVisibility(View.VISIBLE);
        FirebaseHelper.getUserRef(uid).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                User user = doc.toObject(User.class);
                if (user != null) {
                    handleSecurityFlow(user);
                } else {
                    progressBar.setVisibility(View.GONE);
                }
            } else {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Security check failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void handleSecurityFlow(User user) {
        DeviceBindingManager deviceManager = new DeviceBindingManager(this);
        String currentBindingId = deviceManager.getDeviceBindingId();

        // 1. Check for basic account status
        if ("DISABLED".equals(user.getStatus())) {
            Toast.makeText(this, "Your account has been disabled. Contact Admin.", Toast.LENGTH_LONG).show();
            FirebaseHelper.getAuth().signOut();
            progressBar.setVisibility(View.GONE);
            return;
        }

        // 2. Check for incomplete verification (OTP Phase)
        if (!"ACTIVE".equals(user.getStatus()) && !"admin".equalsIgnoreCase(user.getRole())) {
            Intent intent = new Intent(this, VerificationActivity.class);
            intent.putExtra("email", user.getEmail());
            startActivity(intent);
            finish();
            return;
        }

        // 3. Device Binding Enforcement
        if (user.getRegisteredDeviceId() != null && !user.getRegisteredDeviceId().equals(currentBindingId)) {
            Toast.makeText(this, "This account is bound to another device. Contact Admin.", Toast.LENGTH_LONG).show();
            FirebaseHelper.getAuth().signOut();
            progressBar.setVisibility(View.GONE);
            return;
        }

        // 4. Check for incomplete Biometric Onboarding
        if (!"admin".equalsIgnoreCase(user.getRole()) && !user.isFaceRegistered()) {
            startActivity(new Intent(this, FaceEnrollmentActivity.class));
            finish();
            return;
        }

        // Initialize Session
        new SessionManager(this).startSession(UUID.randomUUID().toString());
        progressBar.setVisibility(View.GONE);

        // 5. Final Navigation
        if ("admin".equalsIgnoreCase(user.getRole())) {
            startActivity(new Intent(this, AdminDashboardActivity.class));
        } else {
            startActivity(new Intent(this, EmployeeDashboardActivity.class));
        }
        finish();
    }

    // registerDevice is no longer needed here as it's handled in VerificationActivity during onboarding
}
