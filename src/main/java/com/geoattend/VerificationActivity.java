package com.geoattend;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.geoattend.employee.EmployeeDashboardActivity;
import com.geoattend.employee.FaceEnrollmentActivity;
import com.geoattend.model.User;
import com.geoattend.utils.FirebaseHelper;
import com.geoattend.utils.SecurityUtils;
import com.google.firebase.auth.FirebaseUser;
import java.util.HashMap;
import java.util.Map;

public class VerificationActivity extends AppCompatActivity {
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verification);

        progressBar = findViewById(R.id.progress_bar);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        
        findViewById(R.id.btn_check_status).setOnClickListener(v -> checkVerificationStatus());
        
        findViewById(R.id.btn_resend).setOnClickListener(v -> resendVerificationEmail());
    }

    private void checkVerificationStatus() {
        FirebaseUser user = FirebaseHelper.getAuth().getCurrentUser();
        if (user == null) return;

        progressBar.setVisibility(View.VISIBLE);
        
        // Reload user to get the latest verification status
        user.reload().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (user.isEmailVerified()) {
                    updateUserStatusAndNavigate(user.getUid());
                } else {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Email not yet verified. Please check your inbox.", Toast.LENGTH_SHORT).show();
                }
            } else {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Failed to refresh status: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUserStatusAndNavigate(String uid) {
        FirebaseHelper.getUserRef(uid).update("status", "ACTIVE")
            .addOnSuccessListener(aVoid -> fetchUserAndNavigate(uid))
            .addOnFailureListener(e -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Failed to update user status", Toast.LENGTH_SHORT).show();
            });
    }

    private void fetchUserAndNavigate(String uid) {
        FirebaseHelper.getUserRef(uid).get().addOnSuccessListener(doc -> {
            progressBar.setVisibility(View.GONE);
            User user = doc.toObject(User.class);
            if (user != null) {
                handleNavigation(user);
            }
        }).addOnFailureListener(e -> {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Failed to fetch user profile", Toast.LENGTH_SHORT).show();
        });
    }

    private void handleNavigation(User user) {
        // Enforce the onboarding flow in correct sequence:
        // 1. Device Enrollment
        // 2. Face Enrollment
        // 3. Dashboard
        
        if (user.getRegisteredDeviceId() == null) {
            com.geoattend.utils.DeviceBindingManager deviceManager = new com.geoattend.utils.DeviceBindingManager(this);
            String deviceId = deviceManager.getDeviceBindingId();
            
            // --- STRICT 1:1 DEVICE BINDING ENFORCEMENT ---
            // Secondary check: Ensure no other ACTIVE user is already using this device ID
            FirebaseHelper.getFirestore().collection("users")
                .whereEqualTo("registeredDeviceId", deviceId)
                .get()
                .addOnSuccessListener(query -> {
                    if (query != null && !query.isEmpty()) {
                        // This device belongs to another user. Prevent this user from binding.
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Security Violation: This device is already bound to another account.", Toast.LENGTH_LONG).show();
                        FirebaseHelper.getAuth().signOut();
                        finish();
                    } else {
                        // Device is clean, perform binding
                        bindDeviceToUser(user, deviceId, deviceManager);
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Security check failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
        } else {
            checkNextOnboardingStep(user);
        }
    }

    private void bindDeviceToUser(User user, String deviceId, com.geoattend.utils.DeviceBindingManager deviceManager) {
        Map<String, Object> metadata = deviceManager.getDeviceMetadata();
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("registeredDeviceId", deviceId);
        updates.put("deviceInfo", metadata);
        updates.put("isDeviceEnrolled", true);
        
        FirebaseHelper.getUserRef(user.getUid()).update(updates)
            .addOnSuccessListener(aVoid -> {
                user.setRegisteredDeviceId(deviceId);
                user.setDeviceEnrolled(true);
                checkNextOnboardingStep(user);
            })
            .addOnFailureListener(e -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Failed to bind device", Toast.LENGTH_SHORT).show();
            });
    }

    private void checkNextOnboardingStep(User user) {
        if (!user.isFaceRegistered()) {
            proceedToFaceEnrollment(user);
        } else {
            // All onboarding done
            FirebaseHelper.getUserRef(user.getUid()).update("status", "ACTIVE");
            startActivity(new Intent(this, EmployeeDashboardActivity.class));
            finishAffinity();
        }
    }

    private void proceedToFaceEnrollment(User user) {
        startActivity(new Intent(this, FaceEnrollmentActivity.class));
        finishAffinity();
    }

    private void resendVerificationEmail() {
        FirebaseUser user = FirebaseHelper.getAuth().getCurrentUser();
        if (user != null) {
            user.sendEmailVerification().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, "Verification email resent!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Failed to resend: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
