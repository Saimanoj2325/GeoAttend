package com.geoattend.utils;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import com.geoattend.R;
import com.geoattend.admin.AdminDashboardActivity;
import com.geoattend.admin.AttendanceAnalyticsActivity;
import com.geoattend.admin.NotificationSenderActivity;
import com.geoattend.admin.UserManagementActivity;
import com.geoattend.admin.SecurityCenterActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class AdminNavigationHelper {

    /**
     * Initializes the bottom navigation for admin activities.
     * Uses return false in listener to prevent visual selection change in the source activity
     * before the target activity starts, ensuring consistent state management.
     */
    public static void init(Activity activity, int currentItemId) {
        BottomNavigationView nav = activity.findViewById(R.id.admin_bottom_navigation);
        if (nav == null) return;

        // Force correct state without triggering listener
        nav.setOnItemSelectedListener(null);
        nav.setSelectedItemId(currentItemId);
        
        // Pulse the active icon
        View activeIcon = nav.findViewById(currentItemId);
        if (activeIcon != null) {
            android.animation.ObjectAnimator pulseX = android.animation.ObjectAnimator.ofFloat(activeIcon, "scaleX", 1f, 1.15f, 1f);
            android.animation.ObjectAnimator pulseY = android.animation.ObjectAnimator.ofFloat(activeIcon, "scaleY", 1f, 1.15f, 1f);
            pulseX.setDuration(2000);
            pulseX.setRepeatCount(android.animation.ValueAnimator.INFINITE);
            pulseY.setDuration(2000);
            pulseY.setRepeatCount(android.animation.ValueAnimator.INFINITE);
            pulseX.start();
            pulseY.start();
        }

        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            
            // If clicking the already active tab, just stay there
            if (id == currentItemId) return true;

            // Optional: Premium feedback animation on the clicked icon
            View itemView = nav.findViewById(id);
            if (itemView != null) {
                itemView.animate().scaleX(0.85f).scaleY(0.85f).setDuration(80)
                        .withEndAction(() -> itemView.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start())
                        .start();
            }

            Intent intent = null;
            if (id == R.id.nav_admin_dashboard) {
                intent = new Intent(activity, AdminDashboardActivity.class);
            } else if (id == R.id.nav_employees) {
                intent = new Intent(activity, UserManagementActivity.class);
            } else if (id == R.id.nav_analytics) {
                intent = new Intent(activity, AttendanceAnalyticsActivity.class);
            } else if (id == R.id.nav_messages) {
                intent = new Intent(activity, NotificationSenderActivity.class);
            } else if (id == R.id.nav_settings) {
                // Mapping the "SECURITY" tab to SecurityCenterActivity
                intent = new Intent(activity, SecurityCenterActivity.class);
            }

            if (intent != null) {
                // REORDER_TO_FRONT ensures we reuse activity instances
                // NO_ANIMATION + overridePendingTransition(0,0) makes it feel like fragments
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NO_ANIMATION);
                activity.startActivity(intent);
                activity.overridePendingTransition(0, 0);
                
                // IMPORTANT: Return false so the selection doesn't update in this activity.
                // The new activity will have its own BottomNav with the correct item selected in its onCreate.
                return false;
            }
            return false;
        });
    }
}
