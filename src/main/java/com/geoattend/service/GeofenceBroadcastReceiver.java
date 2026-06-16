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
                    autoCheckOut(geofence.getRequestId());
                }
            }
        }
    }

    private void autoCheckOut(String geofenceId) {
        String userId = FirebaseHelper.getCurrentUserId();
        AttendanceRecord record = new AttendanceRecord(
            null, userId, "Employee", Timestamp.now(), "AUTO_OUT", geofenceId, "Auto Geofence", null
        );

        FirebaseHelper.getAttendanceRef().add(record)
            .addOnSuccessListener(aVoid -> Log.d(TAG, "Auto Check-out recorded for " + geofenceId))
            .addOnFailureListener(e -> Log.e(TAG, "Auto Check-out failed: " + e.getMessage()));
    }
}
