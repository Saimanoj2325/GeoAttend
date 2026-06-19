package com.geoattend;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.geoattend.model.GeofenceItem;
import com.geoattend.model.User;
import com.geoattend.utils.FirebaseHelper;
import com.geoattend.utils.SmtpMailer;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class RegisterActivity extends AppCompatActivity {
    private EditText etFirstName, etLastName, etEmail, etEmployeeId, etPassword, etConfirmPassword;
    private Spinner spinnerOffices;
    private MaterialCheckBox cbTerms;
    private ProgressBar progressBar;
    private List<GeofenceItem> geofenceList = new ArrayList<>();
    private List<String> officeNames = new ArrayList<>();
    private ArrayAdapter<String> officeAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etFirstName = findViewById(R.id.et_first_name);
        etLastName = findViewById(R.id.et_last_name);
        etEmail = findViewById(R.id.et_email);
        etEmployeeId = findViewById(R.id.et_employee_id);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        spinnerOffices = findViewById(R.id.spinner_offices);
        cbTerms = findViewById(R.id.cb_terms);
        progressBar = findViewById(R.id.progress_bar);

        setupTermsText();
        officeAdapter = new ArrayAdapter<>(this, R.layout.item_spinner, officeNames);
        officeAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        spinnerOffices.setAdapter(officeAdapter);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_create_account).setOnClickListener(v -> registerUser());

        setupPasswordVisibilityToggle();
        loadOffices();
    }

    private void setupTermsText() {
        TextView tvTerms = findViewById(R.id.tv_terms);
        String text = "I agree to the Terms of Service and Privacy Policy";
        android.text.SpannableString ss = new android.text.SpannableString(text);

        android.text.style.ClickableSpan tosClick = new android.text.style.ClickableSpan() {
            @Override
            public void onClick(View widget) {
                // Toast.makeText(RegisterActivity.this, "Terms of Service clicked", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void updateDrawState(android.text.TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(false);
                ds.setColor(getColor(R.color.accent_blue));
                ds.setFakeBoldText(true);
            }
        };

        android.text.style.ClickableSpan ppClick = new android.text.style.ClickableSpan() {
            @Override
            public void onClick(View widget) {
                // Toast.makeText(RegisterActivity.this, "Privacy Policy clicked", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void updateDrawState(android.text.TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(false);
                ds.setColor(getColor(R.color.accent_blue));
                ds.setFakeBoldText(true);
            }
        };

        ss.setSpan(tosClick, 15, 31, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ss.setSpan(ppClick, 36, 50, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        tvTerms.setText(ss);
        tvTerms.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
        tvTerms.setHighlightColor(android.graphics.Color.TRANSPARENT);
    }

    private void setupPasswordVisibilityToggle() {
        android.widget.ImageView ivEye = findViewById(R.id.iv_eye_password);
        ivEye.setOnClickListener(v -> {
            if (etPassword.getTransformationMethod() instanceof android.text.method.PasswordTransformationMethod) {
                etPassword.setTransformationMethod(android.text.method.HideReturnsTransformationMethod.getInstance());
                ivEye.setImageResource(android.R.drawable.ic_menu_view);
            } else {
                etPassword.setTransformationMethod(android.text.method.PasswordTransformationMethod.getInstance());
                ivEye.setImageResource(R.drawable.ic_eye);
            }
            etPassword.setSelection(etPassword.getText().length());
        });
    }

    private void loadOffices() {
        FirebaseHelper.getGeofencesRef().get().addOnSuccessListener(queryDocumentSnapshots -> {
            geofenceList.clear();
            officeNames.clear();
            officeNames.add("Select Primary Office *");
            for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                GeofenceItem item = doc.toObject(GeofenceItem.class);
                if (item != null) {
                    item.setId(doc.getId());
                    geofenceList.add(item);
                    officeNames.add(item.getName());
                }
            }
            officeAdapter.notifyDataSetChanged();
        });
    }

    private void registerUser() {
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String empId = etEmployeeId.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();
        int officePos = spinnerOffices.getSelectedItemPosition();

        if (TextUtils.isEmpty(firstName) || TextUtils.isEmpty(lastName) || TextUtils.isEmpty(email) || 
            TextUtils.isEmpty(empId) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
            Toast.makeText(this, "All fields are mandatory", Toast.LENGTH_SHORT).show();
            return;
        }

        if (officePos == 0) {
            Toast.makeText(this, "Please select your primary office", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!cbTerms.isChecked()) {
            Toast.makeText(this, "Please agree to the Terms and Conditions", Toast.LENGTH_SHORT).show();
            return;
        }

        String fullName = firstName + " " + lastName;
        String assignedOfficeId = geofenceList.get(officePos - 1).getId();

        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        // --- STRICT DEVICE BINDING CHECK ---
        com.geoattend.utils.DeviceBindingManager deviceManager = new com.geoattend.utils.DeviceBindingManager(this);
        String deviceId = deviceManager.getDeviceBindingId();

        FirebaseHelper.getFirestore().collection("users")
            .whereEqualTo("registeredDeviceId", deviceId)
            .get()
            .addOnSuccessListener(query -> {
                if (query != null && !query.isEmpty()) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "This device is already associated with another account.", Toast.LENGTH_LONG).show();
                } else {
                    performFirebaseRegistration(fullName, email, empId, password, assignedOfficeId);
                }
            })
            .addOnFailureListener(e -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Security check failed. Please try again later.", Toast.LENGTH_SHORT).show();
            });
    }

    private void performFirebaseRegistration(String name, String email, String empId, String password, String officeId) {
        if (officeId == null) {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Office ID is missing. Please select office again.", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseHelper.getAuth().createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener(authResult -> {
                String uid = Objects.requireNonNull(authResult.getUser()).getUid();
                
                User user = new User(uid, name, email, "employee", null, empId);
                user.setFaceRegistered(false);
                user.setDeviceEnrolled(false);
                user.setRegisteredDeviceId(null); 
                user.setStatus("EMAIL_PENDING");
                user.setAssignedGeofenceId(officeId);
                
                String otp = String.format(java.util.Locale.getDefault(), "%06d", new Random().nextInt(1000000));
                user.setOtp(otp);
                
                // Store in main users collection
                FirebaseHelper.getUserRef(uid).set(user)
                    .addOnSuccessListener(aVoid -> {
                        // Store "Office-Wise" for easier querying as requested
                        java.util.Map<String, Object> member = new java.util.HashMap<>();
                        member.put("uid", uid);
                        member.put("name", name);
                        member.put("email", email);
                        member.put("joinedAt", com.google.firebase.Timestamp.now());
                        
                        FirebaseHelper.getGeofencesRef().document(officeId)
                                .collection("members").document(uid).set(member);

                        // Send Private SMTP Email
                        SmtpMailer.sendOtpEmail(email, otp, new SmtpMailer.MailCallback() {
                            @Override
                            public void onSuccess() {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(RegisterActivity.this, "Security code sent to your email.", Toast.LENGTH_LONG).show();
                                Intent intent = new Intent(RegisterActivity.this, VerificationActivity.class);
                                intent.putExtra("email", email);
                                startActivity(intent);
                                finish();
                            }

                            @Override
                            public void onFailure(Exception e) {
                                progressBar.setVisibility(View.GONE);
                                // Fallback: still go to verification but notify user
                                Toast.makeText(RegisterActivity.this, "Account created, but email failed. Check internet.", Toast.LENGTH_LONG).show();
                                Intent intent = new Intent(RegisterActivity.this, VerificationActivity.class);
                                intent.putExtra("email", email);
                                startActivity(intent);
                                finish();
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
