package com.geoattend.admin;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.geoattend.R;
import com.geoattend.utils.FirebaseHelper;
import com.google.android.material.materialswitch.MaterialSwitch;
import java.util.HashMap;
import java.util.Map;

public class SecurityCenterActivity extends AppCompatActivity {
    private MaterialSwitch switchStrictRoot, switchStrictMock;
    private EditText etFaceThreshold;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_security_center);

        switchStrictRoot = findViewById(R.id.switch_strict_root);
        switchStrictMock = findViewById(R.id.switch_strict_mock);
        etFaceThreshold = findViewById(R.id.et_face_threshold);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_save_settings).setOnClickListener(v -> saveSettings());

        loadSettings();
    }

    private void loadSettings() {
        FirebaseHelper.getFirestore().collection("settings").document("security")
            .get().addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    Boolean strictRoot = doc.getBoolean("strictRoot");
                    Boolean strictMock = doc.getBoolean("strictMock");
                    Double threshold = doc.getDouble("faceThreshold");
                    
                    switchStrictRoot.setChecked(strictRoot != null && strictRoot);
                    switchStrictMock.setChecked(strictMock != null && strictMock);
                    etFaceThreshold.setText(String.valueOf(threshold != null ? threshold : 1.0));
                }
            });
    }

    private void saveSettings() {
        Map<String, Object> settings = new HashMap<>();
        settings.put("strictRoot", switchStrictRoot.isChecked());
        settings.put("strictMock", switchStrictMock.isChecked());
        try {
            settings.put("faceThreshold", Double.parseDouble(etFaceThreshold.getText().toString()));
        } catch (Exception e) {
            settings.put("faceThreshold", 1.0);
        }

        FirebaseHelper.getFirestore().collection("settings").document("security")
            .set(settings).addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Security Protocols Updated", Toast.LENGTH_SHORT).show();
                finish();
            });
    }
}
