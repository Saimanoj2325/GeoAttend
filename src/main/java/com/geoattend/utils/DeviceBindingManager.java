package com.geoattend.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DeviceBindingManager {
    private static final String PREF_NAME = "secure_device_prefs";
    private static final String KEY_DEVICE_BINDING_ID = "device_binding_id";
    
    private final SharedPreferences sharedPreferences;

    public DeviceBindingManager(Context context) {
        // Note: In production, use EncryptedSharedPreferences
        this.sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Generates or retrieves a unique persistent identifier for this installation.
     */
    public String getDeviceBindingId() {
        String id = sharedPreferences.getString(KEY_DEVICE_BINDING_ID, null);
        if (id == null) {
            id = UUID.randomUUID().toString();
            sharedPreferences.edit().putString(KEY_DEVICE_BINDING_ID, id).apply();
        }
        return id;
    }

    /**
     * Collects comprehensive hardware and software metadata.
     */
    public Map<String, Object> getDeviceMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("bindingId", getDeviceBindingId());
        metadata.put("model", Build.MODEL);
        metadata.put("manufacturer", Build.MANUFACTURER);
        metadata.put("androidVersion", Build.VERSION.RELEASE);
        metadata.put("sdkVersion", Build.VERSION.SDK_INT);
        metadata.put("hardware", Build.HARDWARE);
        metadata.put("brand", Build.BRAND);
        metadata.put("appVersion", "1.0"); // Replace with BuildConfig.VERSION_NAME in production
        return metadata;
    }
}
