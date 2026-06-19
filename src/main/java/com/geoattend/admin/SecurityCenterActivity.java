package com.geoattend.admin;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.geoattend.R;
import com.geoattend.utils.FirebaseHelper;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import com.geoattend.utils.AdminNavigationHelper;

public class SecurityCenterActivity extends AppCompatActivity {
    private MaterialSwitch switchStrictRoot, switchStrictMock, switchLiveness, switchVpn, switchNoCapture;
    private Slider sliderFace;
    private TextView tvFaceThreshold;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_security_center);

        AdminNavigationHelper.init(this, R.id.nav_settings);

        switchStrictRoot = findViewById(R.id.switch_strict_root);
        switchStrictMock = findViewById(R.id.switch_strict_mock);
        switchLiveness = findViewById(R.id.switch_liveness);
        switchVpn = findViewById(R.id.switch_vpn);
        switchNoCapture = findViewById(R.id.switch_no_capture);
        sliderFace = findViewById(R.id.slider_face);
        tvFaceThreshold = findViewById(R.id.tv_face_threshold);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_save_settings).setOnClickListener(v -> saveSettings());

        sliderFace.addOnChangeListener((slider, value, fromUser) -> {
            tvFaceThreshold.setText(String.format(Locale.getDefault(), "%.2f", value));
        });

        loadSettings();
    }

    private void loadSettings() {
        FirebaseHelper.getFirestore().collection("settings").document("security")
            .get().addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    Boolean strictRoot = doc.getBoolean("strictRoot");
                    Boolean strictMock = doc.getBoolean("strictMock");
                    Boolean liveness = doc.getBoolean("liveness");
                    Boolean vpn = doc.getBoolean("vpn");
                    Boolean noCapture = doc.getBoolean("noCapture");
                    Double threshold = doc.getDouble("faceThreshold");
                    
                    if (strictRoot != null) switchStrictRoot.setChecked(strictRoot);
                    if (strictMock != null) switchStrictMock.setChecked(strictMock);
                    if (liveness != null) switchLiveness.setChecked(liveness);
                    if (vpn != null) switchVpn.setChecked(vpn);
                    if (noCapture != null) switchNoCapture.setChecked(noCapture);
                    
                    float fThreshold = threshold != null ? threshold.floatValue() : 0.72f;
                    sliderFace.setValue(fThreshold);
                    tvFaceThreshold.setText(String.format(Locale.getDefault(), "%.2f", fThreshold));
                }
            });
    }

    private void saveSettings() {
        Map<String, Object> settings = new HashMap<>();
        settings.put("strictRoot", switchStrictRoot.isChecked());
        settings.put("strictMock", switchStrictMock.isChecked());
        settings.put("liveness", switchLiveness.isChecked());
        settings.put("vpn", switchVpn.isChecked());
        settings.put("noCapture", switchNoCapture.isChecked());
        settings.put("faceThreshold", (double) sliderFace.getValue());

        FirebaseHelper.getFirestore().collection("settings").document("security")
            .set(settings).addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Security Protocols Updated", Toast.LENGTH_SHORT).show();
                finish();
            });
    }
}
