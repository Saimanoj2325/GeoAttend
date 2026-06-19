package com.geoattend.utils;

import android.content.Context;
import android.location.Location;
import android.os.Build;
import android.provider.Settings;
import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class SecurityManager {

    /**
     * Enhanced Root Detection (Layered)
     */
    public static boolean isDeviceCompromised() {
        return checkRootMethod1() || checkRootMethod2() || checkRootMethod3();
    }

    private static boolean checkRootMethod1() {
        String[] paths = {
            "/system/app/Superuser.apk", "/sbin/su", "/system/bin/su", "/system/xbin/su",
            "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su",
            "/system/bin/failsafe/su", "/data/local/su", "/system/bin/.ext/.su"
        };
        for (String path : paths) {
            if (new File(path).exists()) return true;
        }
        return false;
    }

    private static boolean checkRootMethod2() {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"/system/xbin/which", "su"});
            BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
            return in.readLine() != null;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean checkRootMethod3() {
        String buildTags = android.os.Build.TAGS;
        return buildTags != null && buildTags.contains("test-keys");
    }

    /**
     * Layered Mock Location Detection
     */
    public static int getMockRiskScore(Location location, Context context) {
        int score = 0;
        
        if (location == null) return 0;

        // 1. Basic Android API Check
        boolean isMock = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? 
            location.isMock() : location.isFromMockProvider();
        if (isMock) score += 50;

        // 2. Developer Settings Check
        if (isDeveloperOptionsEnabled(context)) {
            score += 20;
        }

        // 3. Provider Check
        if (!location.getProvider().equalsIgnoreCase("gps")) {
            score += 10;
        }

        return score;
    }

    public static boolean isDeveloperOptionsEnabled(Context context) {
        return Settings.Global.getInt(context.getContentResolver(),
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) != 0;
    }

    /**
     * Check if USB Debugging (ADB) is enabled
     */
    public static boolean isUsbDebuggingEnabled(Context context) {
        return Settings.Global.getInt(context.getContentResolver(),
                Settings.Global.ADB_ENABLED, 0) != 0;
    }

    /**
     * App Integrity Check (Play Integrity placeholder)
     * In production, this would return the Integrity Token to be verified on backend.
     */
    public static String getIntegrityVerdict() {
        // Return "MEETS_DEVICE_INTEGRITY" or "COMPROMISED"
        // This logic moves to the Backend with the Play Integrity Token.
        return "PENDING_VERIFICATION";
    }
}
