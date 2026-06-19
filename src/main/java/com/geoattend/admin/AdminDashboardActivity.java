package com.geoattend.admin;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.transition.TransitionManager;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.geoattend.LoginActivity;
import com.geoattend.R;
import com.geoattend.databinding.ActivityAdminDashboardBinding;
import com.geoattend.employee.TimelineAdapter;
import com.geoattend.model.AttendanceRecord;
import com.geoattend.model.GeofenceItem;
import com.geoattend.utils.FirebaseHelper;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import com.geoattend.utils.AdminNavigationHelper;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

public class AdminDashboardActivity extends AppCompatActivity {
    private ActivityAdminDashboardBinding binding;
    private List<TimelineAdapter.TimelineItem> flagItems = new ArrayList<>();
    private TimelineAdapter flagAdapter;
    private BottomSheetBehavior<View> sheetBehavior;
    private int screenHeight;
    private int mapHeightDefault;
    private int mapHeightFocus;
    private int mapHeightImmersive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.getRoot().post(() -> {
            screenHeight = binding.getRoot().getHeight();
            float density = getResources().getDisplayMetrics().density;
            mapHeightDefault = (int) (380 * density);
            mapHeightFocus = (int) (520 * density);
            mapHeightImmersive = (int) (screenHeight * 0.75f);

            initBottomSheet();
        });

        initAdminMap();
        AdminNavigationHelper.init(this, R.id.nav_admin_dashboard);
        initFlagsList();
        loadKPIData();
        loadRecentFlags();
        loadWeeklyStats();

        binding.btnLogout.setOnClickListener(v -> {
            FirebaseHelper.getAuth().signOut();
            startActivity(new Intent(this, LoginActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finishAffinity();
        });

        binding.btnManageZones.setOnClickListener(v -> {
            startActivity(new Intent(this, GeofenceManagerActivity.class));
        });

        binding.btnSecurityHub.setOnClickListener(v -> {
            startActivity(new Intent(this, SecurityCenterActivity.class));
        });

        loadOfficesAndEmployeesOnMap();
        
        // --- DUMMY VERIFICATION SCRIPT ---
        verifyHqEmployees();
        // ---------------------------------

        handleIntent(getIntent());

        // Micro-interaction: Smooth content entrance
        binding.adminBottomSheet.setTranslationY(200f);
        binding.adminBottomSheet.animate().translationY(0).setDuration(800).start();
        
        startPremiumAnimations();
    }

    @android.annotation.SuppressLint("ClickableViewAccessibility")
    private void initBottomSheet() {
        sheetBehavior = BottomSheetBehavior.from(binding.adminBottomSheet);
        sheetBehavior.setFitToContents(false);
        sheetBehavior.setExpandedOffset(0);
        
        sheetBehavior.setPeekHeight(screenHeight - mapHeightDefault);

        sheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    sheetBehavior.setPeekHeight(screenHeight - mapHeightDefault);
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                updateMapVisuals(slideOffset);
            }
        });

       
        binding.adminNestedScroll.setOnTouchListener(new View.OnTouchListener() {
            private float touchStartY;
            private int touchInitialPeek;

            @Override
            public boolean onTouch(View v, android.view.MotionEvent event) {
                if (sheetBehavior.getState() != BottomSheetBehavior.STATE_COLLAPSED) {
                    return false;
                }

                final int action = event.getActionMasked();
                if (action == android.view.MotionEvent.ACTION_DOWN) {
                    touchStartY = event.getRawY();
                    touchInitialPeek = sheetBehavior.getPeekHeight();
                } else if (action == android.view.MotionEvent.ACTION_MOVE) {
                    final float deltaY = event.getRawY() - touchStartY;
                    if (deltaY > 0 && binding.adminNestedScroll.getScrollY() == 0) {
                        final int newPeekHeight = (int) (touchInitialPeek - deltaY);
                        final int minPossiblePeek = screenHeight - mapHeightImmersive;
                        sheetBehavior.setPeekHeight(Math.max(minPossiblePeek, newPeekHeight));
                        final float slideOffset = (sheetBehavior.getPeekHeight() - (screenHeight - mapHeightDefault)) / (float) screenHeight;
                        updateMapVisuals(slideOffset);
                        return true;
                    }
                } else if (action == android.view.MotionEvent.ACTION_UP || action == android.view.MotionEvent.ACTION_CANCEL) {
                    if (action == android.view.MotionEvent.ACTION_UP) {
                        v.performClick();
                    }
                    final int currentVisibleMapHeight = screenHeight - sheetBehavior.getPeekHeight();
                    final int targetMapHeight;
                    if (currentVisibleMapHeight > (mapHeightFocus + mapHeightImmersive) / 2) {
                        targetMapHeight = mapHeightImmersive;
                    } else if (currentVisibleMapHeight > (mapHeightDefault + mapHeightFocus) / 2) {
                        targetMapHeight = mapHeightFocus;
                    } else {
                        targetMapHeight = mapHeightDefault;
                    }
                    animateAdminPeekHeight(targetMapHeight);
                }
                return false;
            }
        });
        
        updateMapVisuals(0f);
    }

    private void animateAdminPeekHeight(int targetMapHeight) {
        int startPeek = sheetBehavior.getPeekHeight();
        int endPeek = screenHeight - targetMapHeight;
        android.animation.ValueAnimator animator = android.animation.ValueAnimator.ofInt(startPeek, endPeek);
        animator.setDuration(300);
        animator.setInterpolator(new android.view.animation.DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            sheetBehavior.setPeekHeight((int) animation.getAnimatedValue());
            float offset = (sheetBehavior.getPeekHeight() - (screenHeight - mapHeightDefault)) / (float)screenHeight;
            updateMapVisuals(offset);
        });
        animator.start();
    }

    private void updateMapVisuals(float slideOffset) {
        if (binding == null) return;

        // Current visible map height
        final int calculatedMapHeight = screenHeight - (binding.adminBottomSheet.getTop());
        double currentZoom = getCurrentZoom(calculatedMapHeight);

        binding.adminMapView.getController().setZoom(Math.max(10.0, currentZoom));

        // Map Scaling effect
        float mapScale = 1.0f - (Math.max(0, slideOffset) * 0.12f);
        binding.adminMapView.setScaleX(mapScale);
        binding.adminMapView.setScaleY(mapScale);
        binding.adminMapView.setPivotY(0);

        // Header Alpha
        float headerAlpha = Math.max(0.2f, 1.0f - (Math.max(0, slideOffset) * 1.5f));
        binding.layoutHeader.setAlpha(headerAlpha);
        
        binding.adminMapView.invalidate();
    }

    private double getCurrentZoom(int calculatedMapHeight) {
        final int visibleMapHeight = (calculatedMapHeight < 0) ? (screenHeight - sheetBehavior.getPeekHeight()) : calculatedMapHeight;

        float density = getResources().getDisplayMetrics().density;
        float mapHeightDp = visibleMapHeight / density;
        double currentZoom;

        // Dynamic Zoom based on snap states
        // Map height ranges: 0 (Collapsed) -> 380dp (Default) -> 520dp (Focus) -> 75% (Immersive)
        // Zoom goals: 14 -> 12 -> 11.5 -> 11
        if (mapHeightDp <= 380) {
            float p = mapHeightDp / 380f;
            currentZoom = 14.0 - (p * 2.0);
        } else if (mapHeightDp <= 520) {
            float p = (mapHeightDp - 380f) / (520f - 380f);
            currentZoom = 12.0 - (p * 0.5);
        } else {
            float maxDp = mapHeightImmersive / density;
            float p = (mapHeightDp - 520f) / (maxDp - 520f);
            currentZoom = 11.5 - (p * 0.5);
        }
        return currentZoom;
    }

    private void startPremiumAnimations() {
        // Status Pulse (e.g. "3 offices live")
        android.animation.ObjectAnimator statusPulse = android.animation.ObjectAnimator.ofFloat(binding.tvStatus, "alpha", 0.4f, 1.0f, 0.4f);
        statusPulse.setDuration(2500);
        statusPulse.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        statusPulse.start();

        // Staggered Stats Entrance (using IDs directly from binding)
        animateStatCard((View)binding.tvTotalStaff.getParent(), 500);
        animateStatCard((View)binding.tvPresentToday.getParent(), 650);
        animateStatCard((View)binding.tvSecurityFlags.getParent(), 800);
    }

    private void animateStatCard(View card, long delay) {
        card.setAlpha(0f);
        card.setTranslationY(40f);
        card.animate()
            .alpha(1f)
            .translationY(0)
            .setDuration(700)
            .setStartDelay(delay)
            .setInterpolator(new android.view.animation.DecelerateInterpolator())
            .start();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent != null && intent.hasExtra("lat") && intent.hasExtra("lng")) {
            double lat = intent.getDoubleExtra("lat", 0);
            double lng = intent.getDoubleExtra("lng", 0);
            binding.adminMapView.getController().animateTo(new GeoPoint(lat, lng));
            binding.adminMapView.getController().setZoom(18.0);
        }
    }

    private void initFlagsList() {
        flagAdapter = new TimelineAdapter(flagItems);
        binding.rvFlags.setLayoutManager(new LinearLayoutManager(this));
        binding.rvFlags.setAdapter(flagAdapter);
    }

    private void initAdminMap() {
        binding.adminMapView.setMultiTouchControls(true);
        binding.adminMapView.getController().setZoom(12.0);
        binding.adminMapView.getController().setCenter(new GeoPoint(12.9716, 77.5946));
        
        com.geoattend.utils.MapUtils.applyPremiumDarkStyle(binding.adminMapView);
    }


    private void loadKPIData() {
        // Real-time Staff Count
        FirebaseHelper.getFirestore().collection("users")
                .whereEqualTo("role", "employee")
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        binding.tvTotalStaff.setText(String.valueOf(value.size()));
                    }
                });

        long startOfDay = getStartOfDay(new java.util.Date()).getTime();
        com.google.firebase.Timestamp tsStart = new com.google.firebase.Timestamp(new java.util.Date(startOfDay));
        FirebaseHelper.getAttendanceRef()
                .whereGreaterThanOrEqualTo("timestamp", tsStart)
                .addSnapshotListener((query, error) -> {
                    if (query == null) return;
                    
                    final java.util.Set<String> uniquePresentUsers = new java.util.HashSet<>();
                    final int[] flagCount = {0};
                    
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        final String type = doc.getString("type");
                        final String userId = doc.getString("userId");
                        final String status = doc.getString("status");
                        
                        if ("IN".equals(type)) {
                            uniquePresentUsers.add(userId);
                        }
                        
                        if ("FLAGGED".equals(status) || "REJECTED".equals(status)) {
                            flagCount[0]++;
                        }
                    }
                    
                    final int fPresent = uniquePresentUsers.size();
                    final int fFlags = flagCount[0];
                    runOnUiThread(() -> {
                        binding.tvPresentToday.setText(String.valueOf(fPresent));
                        binding.tvSecurityFlags.setText(String.valueOf(fFlags));
                    });
                });
    }

    private java.util.Date getStartOfDay(java.util.Date date) {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
        calendar.set(java.util.Calendar.MINUTE, 0);
        calendar.set(java.util.Calendar.SECOND, 0);
        calendar.set(java.util.Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    private void loadWeeklyStats() {
        FirebaseHelper.getFirestore().collection("users")
                .whereEqualTo("role", "employee")
                .get()
                .addOnSuccessListener(userQuery -> {
                    int totalEmployees = Math.max(1, userQuery.size());
                    
                    Calendar cal = Calendar.getInstance();
                    cal.set(Calendar.HOUR_OF_DAY, 0);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    cal.set(Calendar.MILLISECOND, 0);
                    cal.add(Calendar.DAY_OF_YEAR, -6); // Start from 6 days ago

                    final List<Long> dayStartTimes = new ArrayList<>();
                    for (int m = 0; m < 7; m++) {
                        dayStartTimes.add(cal.getTimeInMillis());
                        cal.add(Calendar.DAY_OF_YEAR, 1);
                    }

                    FirebaseHelper.getAttendanceRef()
                            .whereEqualTo("type", "IN")
                            .get()
                            .addOnSuccessListener(attendanceQuery -> {
                                final List<AttendanceRecord> records = attendanceQuery.toObjects(AttendanceRecord.class);
                                final int[] dailyCounts = new int[7];
                                
                                for (AttendanceRecord r : records) {
                                    if (r.getTimestamp() == null) continue;
                                    final long time = r.getTimestamp().toDate().getTime();
                                    int k = 0;
                                    while (k < 7) {
                                        final long dayStart = dayStartTimes.get(k);
                                        final long nextDayStart = dayStart + (24 * 60 * 60 * 1000L);
                                        if (time >= dayStart && time < nextDayStart) {
                                            dailyCounts[k]++;
                                            break;
                                        }
                                        k++;
                                    }
                                }

                                updateChart(dailyCounts, totalEmployees);
                            });
                });
    }

    private void updateChart(int[] dailyCounts, int totalEmployees) {
        View[] bars = {binding.bar1, binding.bar2, binding.bar3, binding.bar4, binding.bar5, binding.bar6, binding.bar7};
        int maxHeightPx = (int) (60 * getResources().getDisplayMetrics().density); // 60dp max height
        
        float sumPercent = 0;
        for (int i = 0; i < 7; i++) {
            float percent = (float) dailyCounts[i] / totalEmployees;
            sumPercent += percent;
            
            final int height = Math.max((int) (maxHeightPx * percent), (int) (4 * getResources().getDisplayMetrics().density));
            final View bar = bars[i];
            final float fPercent = percent;
            
            runOnUiThread(() -> {
                android.view.ViewGroup.LayoutParams params = bar.getLayoutParams();
                params.height = height;
                bar.setLayoutParams(params);
                if (fPercent > 0.8) bar.setBackgroundColor(ContextCompat.getColor(this, R.color.accent_emerald));
                else if (fPercent > 0.5) bar.setBackgroundColor(ContextCompat.getColor(this, R.color.accent_blue));
                else bar.setBackgroundColor(ContextCompat.getColor(this, R.color.surface_l1));
            });
        }
        
        int avgPercent = (int) ((sumPercent / 7) * 100);
        final String avgText = String.format(Locale.getDefault(), "%d%% avg", avgPercent);
        runOnUiThread(() -> binding.tvWeeklyAvg.setText(avgText));
    }

    private void loadRecentFlags() {
        FirebaseHelper.getAttendanceRef()
                .whereIn("status", List.of("FLAGGED", "REJECTED"))
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(10)
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        flagItems.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            AttendanceRecord r = doc.toObject(AttendanceRecord.class);
                            if (r != null && r.getTimestamp() != null) {
                                int icon = "REJECTED".equals(r.getStatus()) ? android.R.drawable.ic_delete : android.R.drawable.ic_dialog_alert;
                                String title = r.getUserName() + " (" + r.getStatus() + ")";
                                flagItems.add(new TimelineAdapter.TimelineItem(title, r.getTimestamp().toDate(), icon));
                            }
                        }
                        flagAdapter.notifyDataSetChanged();
                    }
                });
    }

    private void loadOfficesAndEmployeesOnMap() {
        FirebaseHelper.getGeofencesRef().addSnapshotListener((query, error) -> {
            if (error != null || query == null) return;
            
            binding.adminMapView.getOverlays().clear();
            int officeCount = query.size();
            binding.tvStatus.setText("● " + officeCount + " offices live");

            if (!query.isEmpty()) {
                double minLat = 90, maxLat = -90, minLng = 180, maxLng = -180;
                
                for (DocumentSnapshot doc : query.getDocuments()) {
                    GeofenceItem office = doc.toObject(GeofenceItem.class);
                    if (office != null) {
                        minLat = Math.min(minLat, office.getLatitude());
                        maxLat = Math.max(maxLat, office.getLatitude());
                        minLng = Math.min(minLng, office.getLongitude());
                        maxLng = Math.max(maxLng, office.getLongitude());
                        addOfficeToMap(office);
                    }
                }
                
                // Add Grid back after clear
                binding.adminMapView.getOverlays().add(new com.geoattend.utils.MapUtils.GridOverlay());
                
                // Professional "Auto-Fit" framing
                final BoundingBox bbox = new BoundingBox(maxLat, maxLng, minLat, minLng);
                binding.adminMapView.post(() -> {
                    try {
                        binding.adminMapView.zoomToBoundingBox(bbox, true, 120);
                    } catch (Exception e) {
                        binding.adminMapView.getController().setCenter(new GeoPoint(bbox.getCenterWithDateLine()));
                    }
                });
            }
            binding.adminMapView.invalidate();
        });
    }

    private void verifyHqEmployees() {
        FirebaseHelper.getGeofencesRef().whereEqualTo("name", "HQ").get().addOnSuccessListener(query -> {
            if (!query.isEmpty()) {
                String hqId = query.getDocuments().get(0).getId();
                android.util.Log.d("GeoAttend_Debug", "Found HQ with ID: " + hqId);
                
                FirebaseHelper.getFirestore().collection("users").get().addOnSuccessListener(allUsers -> {
                    android.util.Log.d("GeoAttend_Debug", "Total users in database: " + allUsers.size());
                    int matchCount = 0;
                    for (DocumentSnapshot doc : allUsers.getDocuments()) {
                        String userOfficeId = doc.getString("assignedGeofenceId");
                        String name = doc.getString("name");
                        String role = doc.getString("role");
                        
                        android.util.Log.d("GeoAttend_Debug", "User: " + name + " | Role: " + role + " | OfficeID: " + userOfficeId);
                        
                        if (hqId.equals(userOfficeId)) {
                            matchCount++;
                        }
                    }
                    android.util.Log.d("GeoAttend_Debug", "Matches found for HQ (" + hqId + "): " + matchCount);
                });
            } else {
                android.util.Log.e("GeoAttend_Debug", "No geofence named 'HQ' found!");
            }
        });
    }

    private void addOfficeToMap(GeofenceItem office) {
        GeoPoint center = new GeoPoint(office.getLatitude(), office.getLongitude());
        Marker marker = new Marker(binding.adminMapView);
        marker.setPosition(center);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        
        Drawable icon = ContextCompat.getDrawable(this, R.drawable.ic_marker_orange);
        if (icon != null) {
            marker.setIcon(icon);
        }
        binding.adminMapView.getOverlays().add(marker);

        Polygon circle = new Polygon(binding.adminMapView);
        circle.setPoints(Polygon.pointsAsCircle(center, office.getRadius()));
        circle.getFillPaint().setColor(0x0AEE9D0B); // Faint orange fill
        circle.getOutlinePaint().setColor(0x40EE9D0B); // Semi-transparent orange stroke
        circle.getOutlinePaint().setStrokeWidth(1.5f);
        binding.adminMapView.getOverlays().add(circle);
    }
}
