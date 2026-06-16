package com.geoattend;

import android.app.Application;
import android.preference.PreferenceManager;
import com.google.firebase.FirebaseApp;
import org.osmdroid.config.Configuration;

public class GeoAttendApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize Firebase
        FirebaseApp.initializeApp(this);
        
        // Initialize OSMDroid Configuration
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
    }
}
