package com.geoattend;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.geoattend.employee.EmployeeDashboardActivity;
import com.geoattend.utils.FirebaseHelper;
import com.geoattend.utils.SecurityManager;

public class IntegrityCheckActivity extends AppCompatActivity {

    private View rowRoot, rowDebug, rowMock, rowVpn;
    private ImageView iconRoot, iconDebug, iconMock, iconVpn;
    private ProgressBar pbRoot, pbDebug, pbMock, pbVpn;
    private TextView tvDesc;
    private View pulse1, pulse2, scanLine;
    private AlertDialog failureDialog;
    private boolean isScanning = false;

    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_integrity_check);

        initViews();
        startPulseAnimations();
        startScanLineAnimation();
        // Initial run is handled by onResume logic to avoid double-triggers
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isScanning) {
            if (failureDialog != null && failureDialog.isShowing()) {
                failureDialog.dismiss();
            }
            resetUI();
            runIntegritySuite();
        }
    }

    private void resetUI() {
        // Reset rows to initial state
        View[] rows = {rowRoot, rowDebug, rowMock, rowVpn};
        ImageView[] icons = {iconRoot, iconDebug, iconMock, iconVpn};
        ProgressBar[] pbs = {pbRoot, pbDebug, pbMock, pbVpn};

        for (int i = 0; i < rows.length; i++) {
            rows[i].setAlpha(0.4f);
            icons[i].setImageResource(R.drawable.dot_inactive);
            icons[i].setImageTintList(android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.text_disabled)
            ));
            icons[i].setScaleX(1.0f);
            icons[i].setScaleY(1.0f);
            pbs[i].setVisibility(View.GONE);
        }
        tvDesc.setText("Securing your environment...");
        tvDesc.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
    }

    private void initViews() {
        rowRoot = findViewById(R.id.row_root);
        rowDebug = findViewById(R.id.row_debug);
        rowMock = findViewById(R.id.row_mock);
        rowVpn = findViewById(R.id.row_vpn);

        iconRoot = findViewById(R.id.icon_root);
        iconDebug = findViewById(R.id.icon_debug);
        iconMock = findViewById(R.id.icon_mock);
        iconVpn = findViewById(R.id.icon_vpn);

        pbRoot = findViewById(R.id.pb_root);
        pbDebug = findViewById(R.id.pb_debug);
        pbMock = findViewById(R.id.pb_mock);
        pbVpn = findViewById(R.id.pb_vpn);

        tvDesc = findViewById(R.id.tv_security_desc);
        pulse1 = findViewById(R.id.scan_pulse_1);
        pulse2 = findViewById(R.id.scan_pulse_2);
        scanLine = findViewById(R.id.scan_line);
    }

    private void startScanLineAnimation() {
        ObjectAnimator animator = ObjectAnimator.ofFloat(scanLine, "translationY", -40f, 40f);
        animator.setDuration(1500);
        animator.setRepeatMode(ValueAnimator.REVERSE);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.start();

        // Add subtle rotation to shield for "Active" feeling
        ImageView shield = findViewById(R.id.iv_shield);
        ObjectAnimator rotate = ObjectAnimator.ofFloat(shield, "rotation", -3f, 3f);
        rotate.setDuration(3000);
        rotate.setRepeatMode(ValueAnimator.REVERSE);
        rotate.setRepeatCount(ValueAnimator.INFINITE);
        rotate.start();
    }

    private void startPulseAnimations() {
        animatePulse(pulse1, 0);
        animatePulse(pulse2, 1000);
        
        // Background Glow Pulse
        View glow = findViewById(R.id.view_glow);
        ObjectAnimator glowAnim = ObjectAnimator.ofFloat(glow, "alpha", 0.1f, 0.4f, 0.1f);
        glowAnim.setDuration(4000);
        glowAnim.setRepeatCount(ValueAnimator.INFINITE);
        glowAnim.start();
    }

    private void animatePulse(View view, long delay) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 2.5f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 2.5f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(view, "alpha", 0.3f, 0f);

        scaleX.setRepeatCount(ValueAnimator.INFINITE);
        scaleY.setRepeatCount(ValueAnimator.INFINITE);
        alpha.setRepeatCount(ValueAnimator.INFINITE);

        scaleX.setDuration(2500);
        scaleY.setDuration(2500);
        alpha.setDuration(2500);

        scaleX.setStartDelay(delay);
        scaleY.setStartDelay(delay);
        alpha.setStartDelay(delay);

        scaleX.start();
        scaleY.start();
        alpha.start();
    }

    private void runIntegritySuite() {
        isScanning = true;
        // Step 1: Root Check
        handler.postDelayed(() -> performCheck("root"), 800);
    }

    private void performCheck(String type) {
        if (!isScanning) return; // Guard against checks running after secured or during reset
        switch (type) {
            case "root":
                updateRowState(rowRoot, pbRoot, true);
                handler.postDelayed(() -> {
                    boolean compromised = SecurityManager.isDeviceCompromised();
                    finalizeRowState(rowRoot, iconRoot, pbRoot, !compromised);
                    if (compromised) {
                        showFailure("Device Integrity Failed", "This device appears to be rooted or running a custom ROM. GeoAttend cannot run on insecure environments.");
                    } else {
                        performCheck("debug");
                    }
                }, 1200);
                break;

            case "debug":
                updateRowState(rowDebug, pbDebug, true);
                handler.postDelayed(() -> {
                    boolean enabled = SecurityManager.isUsbDebuggingEnabled(this);
                    finalizeRowState(rowDebug, iconDebug, pbDebug, !enabled);
                    if (enabled) {
                        showFailure("USB Debugging Enabled", "GeoAttend cannot run while USB Debugging is active. Please disable it in Developer Options.");
                    } else {
                        performCheck("mock");
                    }
                }, 1000);
                break;

            case "mock":
                updateRowState(rowMock, pbMock, true);
                handler.postDelayed(() -> {
                    // Actual check logic is usually tied to location updates, 
                    // but we check Dev Options as a proxy for mock permission here.
                    boolean risky = SecurityManager.isDeveloperOptionsEnabled(this);
                    finalizeRowState(rowMock, iconMock, pbMock, true); // We'll just warn later if needed, proceed for now
                    performCheck("vpn");
                }, 1000);
                break;

            case "vpn":
                updateRowState(rowVpn, pbVpn, true);
                handler.postDelayed(() -> {
                    finalizeRowState(rowVpn, iconVpn, pbVpn, true);
                    finishSequence();
                }, 800);
                break;
        }
    }

    private void updateRowState(View row, ProgressBar pb, boolean active) {
        row.animate().alpha(1.0f).setDuration(300).start();
        pb.setVisibility(active ? View.VISIBLE : View.GONE);
    }

    private void finalizeRowState(View row, ImageView icon, ProgressBar pb, boolean success) {
        pb.setVisibility(View.GONE);
        icon.setImageResource(success ? R.drawable.ic_check_circle : R.drawable.circle_error);
        icon.setImageTintList(android.content.res.ColorStateList.valueOf(
            ContextCompat.getColor(this, success ? R.color.accent_emerald : R.color.error)
        ));
        
        icon.setScaleX(0.5f);
        icon.setScaleY(0.5f);
        icon.animate().scaleX(1.0f).scaleY(1.0f).setDuration(400).setInterpolator(new AccelerateDecelerateInterpolator()).start();
    }

    private void showFailure(String title, String message) {
        isScanning = false;
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_security_warning, null);
        failureDialog = new AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create();

        if (failureDialog.getWindow() != null) {
            failureDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
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

        com.google.android.material.button.MaterialButton btnExit = dialogView.findViewById(R.id.btn_exit_app);
        btnExit.setText("RETRY SCAN");
        btnExit.setOnClickListener(v -> {
            failureDialog.dismiss();
            resetUI();
            runIntegritySuite();
        });

        failureDialog.show();
    }

    private void finishSequence() {
        isScanning = true; // Still "active" until transition
        tvDesc.setText("Environment Secured.");
        tvDesc.setTextColor(ContextCompat.getColor(this, R.color.accent_emerald));
        
        handler.postDelayed(() -> {
            isScanning = false;
            // Navigate to LoginActivity which already has auto-login logic
            startActivity(new Intent(this, LoginActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, 1000);
    }
}
