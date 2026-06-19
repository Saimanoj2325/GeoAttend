package com.geoattend;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.geoattend.employee.EmployeeDashboardActivity;
import com.geoattend.employee.FaceEnrollmentActivity;
import com.geoattend.model.User;
import com.geoattend.utils.FirebaseHelper;
import com.geoattend.utils.SmtpMailer;
import com.google.firebase.auth.FirebaseUser;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class VerificationActivity extends AppCompatActivity {
    private ProgressBar progressBar;
    private EditText[] otpFields = new EditText[6];
    private TextView tvTimer, btnResend;
    private CountDownTimer countDownTimer;
    private String userEmail;

    private com.google.android.material.button.MaterialButton btnVerify;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verification);

        userEmail = getIntent().getStringExtra("email");
        if (userEmail != null) {
            TextView tvSubtitle = findViewById(R.id.tv_subtitle);
            tvSubtitle.setText("We sent a 6-digit verification code to\n" + userEmail);
        }

        progressBar = findViewById(R.id.progress_bar);
        tvTimer = findViewById(R.id.tv_timer);
        btnResend = findViewById(R.id.btn_resend);
        btnVerify = findViewById(R.id.btn_verify);
        btnVerify.setEnabled(false);
        btnVerify.setAlpha(0.5f);

        otpFields[0] = findViewById(R.id.otp1);
        otpFields[1] = findViewById(R.id.otp2);
        otpFields[2] = findViewById(R.id.otp3);
        otpFields[3] = findViewById(R.id.otp4);
        otpFields[4] = findViewById(R.id.otp5);
        otpFields[5] = findViewById(R.id.otp6);

        for (EditText et : otpFields) et.setText("");
        setupOtpFields();

        findViewById(R.id.btn_go_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_verify).setOnClickListener(v -> verifyOtp());
        btnResend.setOnClickListener(v -> resendOtp());

        startResendTimer();
        
        // DEV TIP: For this project, we'll show the OTP in a toast since we don't have an email server
        showOtpHint();
    }

    private void setupOtpFields() {
        for (int i = 0; i < 6; i++) {
            final int index = i;
            otpFields[i].addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() == 1) {
                        otpFields[index].animate().scaleX(1.1f).scaleY(1.1f).setDuration(100).withEndAction(() -> 
                            otpFields[index].animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
                        ).start();
                        
                        if (index < 5) {
                            otpFields[index + 1].requestFocus();
                        }
                    }
                    checkOtpCompletion();
                }
                @Override public void afterTextChanged(Editable s) {}
            });
            
            otpFields[i].setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == android.view.KeyEvent.KEYCODE_DEL && otpFields[index].getText().length() == 0 && index > 0) {
                    otpFields[index - 1].requestFocus();
                    return true;
                }
                return false;
            });
        }
    }

    private void checkOtpCompletion() {
        boolean complete = true;
        for (EditText et : otpFields) {
            if (et.getText().length() == 0) {
                complete = false;
                break;
            }
        }
        btnVerify.setEnabled(complete);
        btnVerify.animate().alpha(complete ? 1.0f : 0.5f).setDuration(200).start();
    }

    private void showOtpHint() {
        String uid = FirebaseHelper.getCurrentUserId();
        if ("test_user".equals(uid)) return;

        FirebaseHelper.getUserRef(uid).get().addOnSuccessListener(doc -> {
            if (isFinishing() || isDestroyed()) return;
            User user = doc.toObject(User.class);
            if (user != null && user.getOtp() != null) {
                Toast.makeText(this, "[DEMO] Your Verification Code is: " + user.getOtp(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void startResendTimer() {
        btnResend.setEnabled(false);
        btnResend.setAlpha(0.5f);
        countDownTimer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long minutes = (millisUntilFinished / 1000) / 60;
                long seconds = (millisUntilFinished / 1000) % 60;
                tvTimer.setText(String.format(Locale.getDefault(), "Code expires in %02d:%02d", minutes, seconds));
            }

            @Override
            public void onFinish() {
                tvTimer.setText("Code expired");
                btnResend.setEnabled(true);
                btnResend.setAlpha(1.0f);
            }
        }.start();
    }

    private void verifyOtp() {
        StringBuilder sb = new StringBuilder();
        for (EditText et : otpFields) sb.append(et.getText().toString());
        String enteredOtp = sb.toString();

        if (enteredOtp.length() < 6) {
            Toast.makeText(this, "Please enter 6-digit code", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        String uid = FirebaseHelper.getCurrentUserId();
        
        FirebaseHelper.getUserRef(uid).get().addOnSuccessListener(doc -> {
            User user = doc.toObject(User.class);
            if (user != null && enteredOtp.equals(user.getOtp())) {
                // OTP Correct
                FirebaseHelper.getUserRef(uid).update("status", "ACTIVE");
                handleNavigation(user);
            } else {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Invalid verification code", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Verification failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void resendOtp() {
        if (userEmail == null) {
            Toast.makeText(this, "Email not found. Please try logging in again.", Toast.LENGTH_SHORT).show();
            return;
        }
        progressBar.setVisibility(View.VISIBLE);
        String newOtp = String.format(Locale.getDefault(), "%06d", new Random().nextInt(1000000));
        String uid = FirebaseHelper.getCurrentUserId();
        
        FirebaseHelper.getUserRef(uid).update("otp", newOtp).addOnSuccessListener(aVoid -> {
            SmtpMailer.sendOtpEmail(userEmail, newOtp, new SmtpMailer.MailCallback() {
                @Override
                public void onSuccess() {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(VerificationActivity.this, "New code sent!", Toast.LENGTH_SHORT).show();
                    startResendTimer();
                }

                @Override
                public void onFailure(Exception e) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(VerificationActivity.this, "Failed to send email. Try again.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void handleNavigation(User user) {
        if (user.getRegisteredDeviceId() == null) {
            com.geoattend.utils.DeviceBindingManager deviceManager = new com.geoattend.utils.DeviceBindingManager(this);
            String deviceId = deviceManager.getDeviceBindingId();
            
            FirebaseHelper.getFirestore().collection("users")
                .whereEqualTo("registeredDeviceId", deviceId)
                .get()
                .addOnSuccessListener(query -> {
                    if (query != null && !query.isEmpty()) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Security Violation: This device is already bound.", Toast.LENGTH_LONG).show();
                        FirebaseHelper.getAuth().signOut();
                        finish();
                    } else {
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

    @androidx.camera.core.ExperimentalGetImage
    private void checkNextOnboardingStep(User user) {
        if (!user.isFaceRegistered()) {
            startActivity(new Intent(this, FaceEnrollmentActivity.class));
            finishAffinity();
        } else {
            FirebaseHelper.getUserRef(user.getUid()).update("status", "ACTIVE");
            startActivity(new Intent(this, EmployeeDashboardActivity.class));
            finishAffinity();
        }
    }
}
