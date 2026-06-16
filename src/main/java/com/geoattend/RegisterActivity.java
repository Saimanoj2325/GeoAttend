package com.geoattend;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.geoattend.model.User;
import com.geoattend.utils.FirebaseHelper;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Objects;

public class RegisterActivity extends AppCompatActivity {
    private TextInputEditText etName, etEmail, etEmployeeId, etPassword;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etName = findViewById(R.id.et_name);
        etEmail = findViewById(R.id.et_email);
        etEmployeeId = findViewById(R.id.et_employee_id);
        etPassword = findViewById(R.id.et_password);
        progressBar = findViewById(R.id.progress_bar);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_register).setOnClickListener(v -> registerUser());
    }

    private void registerUser() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String empId = etEmployeeId.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || 
            TextUtils.isEmpty(empId) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        // --- STRICT DEVICE BINDING CHECK ---
        // Check if this device is already bound to another account before allowing registration
        com.geoattend.utils.DeviceBindingManager deviceManager = new com.geoattend.utils.DeviceBindingManager(this);
        String deviceId = deviceManager.getDeviceBindingId();

        FirebaseHelper.getFirestore().collection("users")
            .whereEqualTo("registeredDeviceId", deviceId)
            .get()
            .addOnSuccessListener(query -> {
                if (query != null && !query.isEmpty()) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "This device is already associated with another account. Multiple accounts per device are strictly prohibited.", Toast.LENGTH_LONG).show();
                } else {
                    // Proceed with registration if device is clean
                    performFirebaseRegistration(name, email, empId, password);
                }
            })
            .addOnFailureListener(e -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Security check failed. Please try again later.", Toast.LENGTH_SHORT).show();
            });
    }

    private void performFirebaseRegistration(String name, String email, String empId, String password) {
        FirebaseHelper.getAuth().createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener(authResult -> {
                String uid = Objects.requireNonNull(authResult.getUser()).getUid();
                
                // Initialize user with mandatory production-ready fields
                User user = new User(uid, name, email, "employee", null, empId);
                user.setFaceRegistered(false);
                user.setDeviceEnrolled(false);
                user.setRegisteredDeviceId(null); // Explicitly null until first binding
                user.setStatus("EMAIL_PENDING");
                
                FirebaseHelper.getUserRef(uid).set(user)
                    .addOnSuccessListener(aVoid -> {
                        authResult.getUser().sendEmailVerification().addOnCompleteListener(task -> {
                            progressBar.setVisibility(View.GONE);
                            if (task.isSuccessful()) {
                                Toast.makeText(this, "Verification email sent. Please verify to continue.", Toast.LENGTH_LONG).show();
                                // Redirect to VerificationActivity immediately after registration
                                Intent intent = new Intent(RegisterActivity.this, VerificationActivity.class);
                                intent.putExtra("email", email);
                                startActivity(intent);
                                finish();
                            } else {
                                Toast.makeText(this, "Failed to send verification email.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    })
                    .addOnFailureListener(e -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Data save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
            })
            .addOnFailureListener(e -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Registration failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
    }
}
