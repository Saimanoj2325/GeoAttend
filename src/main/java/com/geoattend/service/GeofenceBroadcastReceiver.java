package com.geoattend.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.geoattend.model.AttendanceRecord;
import com.geoattend.utils.FirebaseHelper;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.google.firebase.Timestamp;
import java.util.List;

public class GeofenceBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "GeofenceReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            Log.e(TAG, "Geofence Error: " + geofencingEvent.getErrorCode());
            return;
        }

        int transitionType = geofencingEvent.getGeofenceTransition();

        if (transitionType == Geofence.GEOFENCE_TRANSITION_EXIT) {
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();
            if (triggeringGeofences != null) {
                for (Geofence geofence : triggeringGeofences) {
                    autoCheckOut(context, geofence.getRequestId());
                }
            }
        }
    }

    private void autoCheckOut(Context context, String geofenceId) {
        String userId = FirebaseHelper.getCurrentUserId();
        AttendanceRecord record = new AttendanceRecord(
            null, userId, "Auto System", Timestamp.now(), "AUTO_OUT", geofenceId, "Secure Geofence", null
        );
        
        record.setDeviceId(com.geoattend.utils.SecurityUtils.getAppSpecificDeviceId(context));
        record.setLivenessResult("AUTO_EXIT");
        record.setRooted(com.geoattend.utils.SecurityManager.isDeviceCompromised());
        record.setStatus("VERIFIED");

        FirebaseHelper.getAttendanceRef().add(record)
            .addOnSuccessListener(aVoid -> Log.d(TAG, "Auto Check-out recorded for " + geofenceId))
            .addOnFailureListener(e -> Log.e(TAG, "Auto Check-out failed: " + e.getMessage()));
    }
}
