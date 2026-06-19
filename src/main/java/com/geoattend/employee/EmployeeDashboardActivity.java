package com.geoattend.employee;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.geoattend.AttendanceHistoryActivity;
import com.geoattend.LoginActivity;
import com.geoattend.R;
import com.geoattend.databinding.ActivityEmployeeDashboardBinding;
import com.geoattend.model.AttendanceRecord;
import com.geoattend.model.GeofenceItem;
import com.geoattend.model.User;
import com.geoattend.utils.FirebaseHelper;
import com.geoattend.utils.SecurityManager;
import com.geoattend.utils.SessionManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import androidx.core.widget.NestedScrollView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

@androidx.camera.core.ExperimentalGetImage
public class EmployeeDashboardActivity extends AppCompatActivity {

    private ActivityEmployeeDashboardBinding binding;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private MyLocationNewOverlay myLocationOverlay;
    private Polygon pulseCircle;
    private GeofenceItem assignedGeofence;
    private List<Location> gpsSamples = new ArrayList<>();
    private boolean isCheckedIn = false;
    private boolean isCheckedOut = false;
    private boolean hasInitialMapCentered = false;
    private List<GeofenceItem> availableOffices = new ArrayList<>();
    private Timer workingTimer;
    private Date checkInTime;
    private Date checkOutTime;
    private Location lastKnownLocation;
    private float lastDistanceToOffice = 0f;

    private List<TimelineAdapter.TimelineItem> timelineItems = new ArrayList<>();
    private TimelineAdapter timelineAdapter;

    // Map Snap State Variables
    private int screenHeight;
    private int mapHeightDefault;
    private int mapHeightFocus;
    private int mapHeightImmersive;
    private BottomSheetBehavior<View> sheetBehavior;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEmployeeDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Calculate screen dimensions for snap states
        binding.getRoot().post(() -> {
            screenHeight = binding.getRoot().getHeight();
            float density = getResources().getDisplayMetrics().density;
            mapHeightDefault = (int) (320 * density);
            mapHeightFocus = (int) (450 * density);
            mapHeightImmersive = (int) (screenHeight * 0.75f);
            
            initBottomSheet();
        });

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        
        initMap();
        initNavigation();
        initTimeline();
        updateDateTime();
        checkAttendanceState();
        
        binding.btnCheckIn.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            performSecureCheckIn();
        });
        binding.btnCheckOut.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            performSecureCheckOut();
        });
        
        binding.cardAssignedOffice.setOnClickListener(v -> showOfficeSelectionDialog());

        binding.cardRecenter.setOnClickListener(v -> {
            if (myLocationOverlay.getMyLocation() != null) {
                binding.mapView.getController().animateTo(myLocationOverlay.getMyLocation());
            } else {
                fitMapToUserAndOffice();
            }
        });
        
        binding.cardMenu.setOnClickListener(v -> {
            // Placeholder for new map feature
            Toast.makeText(this, "Map Features Coming Soon", Toast.LENGTH_SHORT).show();
        });

        checkPermissions();
        fetchUserData();
        loadInsights();
        loadWeeklyProgress();
        startStatAnimations();

        // Micro-interaction: Smooth entrance for bottom sheet area
        binding.attendanceBottomSheet.setTranslationY(200f);
        binding.attendanceBottomSheet.animate().translationY(0).setDuration(800).start();
    }

    private void startStatAnimations() {
        // Fire Streak Pulse
        android.animation.ObjectAnimator fireScaleX = android.animation.ObjectAnimator.ofFloat(binding.ivFireStreak, "scaleX", 1f, 1.2f, 1f);
        android.animation.ObjectAnimator fireScaleY = android.animation.ObjectAnimator.ofFloat(binding.ivFireStreak, "scaleY", 1f, 1.2f, 1f);
        fireScaleX.setDuration(1200);
        fireScaleX.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        fireScaleY.setDuration(1200);
        fireScaleY.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        fireScaleX.start();
        fireScaleY.start();

        // Calendar Rotate
        android.animation.ObjectAnimator calRotate = android.animation.ObjectAnimator.ofFloat(binding.ivCalendarStat, "rotation", -5f, 5f);
        calRotate.setDuration(2000);
        calRotate.setRepeatMode(android.animation.ValueAnimator.REVERSE);
        calRotate.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        calRotate.start();
        
        // Check-In Button Pulse (if enabled)
        android.animation.ObjectAnimator btnPulseX = android.animation.ObjectAnimator.ofFloat(binding.btnCheckIn, "scaleX", 1f, 1.03f, 1f);
        android.animation.ObjectAnimator btnPulseY = android.animation.ObjectAnimator.ofFloat(binding.btnCheckIn, "scaleY", 1f, 1.03f, 1f);
        btnPulseX.setDuration(2500);
        btnPulseX.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        btnPulseY.setDuration(2500);
        btnPulseY.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        btnPulseX.start();
        btnPulseY.start();
    }

    private void initTimeline() {
        timelineAdapter = new TimelineAdapter(timelineItems);
        binding.rvTimeline.setLayoutManager(new LinearLayoutManager(this));
        binding.rvTimeline.setAdapter(timelineAdapter);
    }

    private void initMap() {
        binding.mapView.setMultiTouchControls(true);
        binding.mapView.getController().setZoom(18.0);
        
        com.geoattend.utils.MapUtils.applyPremiumDarkStyle(binding.mapView);

        // Custom My Location Overlay with premium dot
        myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), binding.mapView);
        myLocationOverlay.enableMyLocation();
        
        Drawable personIcon = ContextCompat.getDrawable(this, R.drawable.ic_marker_blue);
        if (personIcon != null) {
            myLocationOverlay.setPersonIcon(com.geoattend.utils.BitmapUtils.drawableToBitmap(personIcon));
            myLocationOverlay.setDirectionIcon(com.geoattend.utils.BitmapUtils.drawableToBitmap(personIcon));
        }

        binding.mapView.getOverlays().add(myLocationOverlay);

        // Map Pulse Animation (Simulated with a polygon that animates)
        addPulseOverlay();
    }

    private void addPulseOverlay() {
        pulseCircle = new Polygon();
        pulseCircle.getOutlinePaint().setStrokeWidth(0f);
        binding.mapView.getOverlays().add(pulseCircle);

        android.animation.ValueAnimator animator = android.animation.ValueAnimator.ofFloat(0, 1);
        animator.setDuration(5000); // 5 seconds interval
        animator.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        animator.setInterpolator(new android.view.animation.DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            if (myLocationOverlay.getMyLocation() == null || assignedGeofence == null) return;
            float val = (float) animation.getAnimatedValue();
            pulseCircle.setPoints(Polygon.pointsAsCircle(myLocationOverlay.getMyLocation(), 5 + (val * 45)));
            
            boolean isInside = lastDistanceToOffice <= assignedGeofence.getRadius();
            int r = isInside ? 30 : 59;
            int g = isInside ? 201 : 130;
            int b = isInside ? 142 : 246;
            
            pulseCircle.getFillPaint().setColor(android.graphics.Color.argb((int)((1 - val) * 40), r, g, b));
            binding.mapView.invalidate();
        });
        animator.start();
    }

    private void initNavigation() {
        binding.bottomNavigation.setSelectedItemId(R.id.nav_dashboard);
        
        // Pulse the active icon
        View activeIcon = binding.bottomNavigation.findViewById(R.id.nav_dashboard);
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

        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            // Premium Click Animation
            View itemView = findViewById(item.getItemId());
            if (itemView != null) {
                itemView.animate()
                    .scaleX(0.85f)
                    .scaleY(0.85f)
                    .alpha(0.7f)
                    .setDuration(100)
                    .withEndAction(() -> 
                        itemView.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .alpha(1.0f)
                            .setDuration(150)
                            .start()
                    ).start();
            }

            int id = item.getItemId();
            if (id == R.id.nav_dashboard) return true;

            if (id == R.id.nav_attendance) {
                startActivity(new Intent(this, AttendanceHistoryActivity.class));
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            } else if (id == R.id.nav_notifications) {
                startActivity(new Intent(this, NotificationsActivity.class));
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            }
            return true;
        });
    }

    private void initBottomSheet() {
        sheetBehavior = BottomSheetBehavior.from(binding.attendanceBottomSheet);
        
        // Use fitToContents = false to allow custom offsets
        sheetBehavior.setFitToContents(false);
        sheetBehavior.setExpandedOffset(0);
        
        // Initial map height is Default (320dp)
        sheetBehavior.setPeekHeight(screenHeight - mapHeightDefault);
        
        updateMapTransformation(0f);

        sheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    // Reset to default map height if it was expanded via overscroll
                    sheetBehavior.setPeekHeight(screenHeight - mapHeightDefault);
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                // slideOffset: 0 (collapsed/peek) to 1 (expanded)
                updateMapTransformation(slideOffset);
            }
        });

        // Listen to internal scroll and touch for Bi-directional interaction
        binding.nestedScrollView.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            if (sheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                // Dynamic detail transformation when expanded
                float extraOffset = Math.min(scrollY / 1000f, 0.1f);
                updateMapTransformation(1.0f + extraOffset);
            }
        });

        // Handle overscroll for expansion (Immersive & Focus)
        binding.nestedScrollView.setOnTouchListener(new View.OnTouchListener() {
            private float startY;
            private int initialPeek;

            @Override
            public boolean onTouch(View v, android.view.MotionEvent event) {
                if (sheetBehavior.getState() != BottomSheetBehavior.STATE_COLLAPSED) return false;

                switch (event.getAction()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        startY = event.getRawY();
                        initialPeek = sheetBehavior.getPeekHeight();
                        break;
                    case android.view.MotionEvent.ACTION_MOVE:
                        float deltaY = event.getRawY() - startY;
                        if (deltaY > 0 && binding.nestedScrollView.getScrollY() == 0) {
                            // Pulling down while at top -> Expand Map Height
                            int newPeek = (int) (initialPeek - deltaY);
                            int minPeek = screenHeight - mapHeightImmersive;
                            sheetBehavior.setPeekHeight(Math.max(minPeek, newPeek));
                            updateMapTransformation((sheetBehavior.getPeekHeight() - (screenHeight - mapHeightDefault)) / (float)(initialPeek));
                            return true;
                        }
                        break;
                    case android.view.MotionEvent.ACTION_UP:
                    case android.view.MotionEvent.ACTION_CANCEL:
                        int currentMapHeight = screenHeight - sheetBehavior.getPeekHeight();
                        int targetHeight;
                        
                        if (currentMapHeight > (mapHeightFocus + mapHeightImmersive) / 2) {
                            targetHeight = mapHeightImmersive;
                        } else if (currentMapHeight > (mapHeightDefault + mapHeightFocus) / 2) {
                            targetHeight = mapHeightFocus;
                        } else {
                            targetHeight = mapHeightDefault;
                        }
                        
                        animatePeekHeight(targetHeight);
                        break;
                }
                return false;
            }
        });
    }

    private void animatePeekHeight(int targetMapHeight) {
        int startPeek = sheetBehavior.getPeekHeight();
        int endPeek = screenHeight - targetMapHeight;
        
        android.animation.ValueAnimator animator = android.animation.ValueAnimator.ofInt(startPeek, endPeek);
        animator.setDuration(300);
        animator.setInterpolator(new android.view.animation.DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            sheetBehavior.setPeekHeight((int) animation.getAnimatedValue());
            // Map slideOffset for transformation
            float offset = (sheetBehavior.getPeekHeight() - (screenHeight - mapHeightDefault)) / (float)screenHeight;
            updateMapTransformation(offset);
        });
        animator.start();
    }

    private void updateMapTransformation(float slideOffset) {
        if (binding == null) return;

        // Current visible map height
        int currentMapHeight = screenHeight - (binding.attendanceBottomSheet.getTop());
        if (currentMapHeight < 0) currentMapHeight = screenHeight - sheetBehavior.getPeekHeight();

        // 1. Dynamic Zoom logic based on snap states
        // Map height ranges: 0 (Collapsed) -> 320dp (Default) -> 450dp (Focus) -> 75% (Immersive)
        // Zoom goals: 18 -> 16 -> 15.5 -> 15
        
        float mapHeightDp = currentMapHeight / getResources().getDisplayMetrics().density;
        double currentZoom;
        
        if (mapHeightDp <= 320) {
            // 0 to 320: Zoom 18 to 16
            float p = mapHeightDp / 320f;
            currentZoom = 18.0 - (p * 2.0);
        } else if (mapHeightDp <= 450) {
            // 320 to 450: Zoom 16 to 15.5
            float p = (mapHeightDp - 320f) / (450f - 320f);
            currentZoom = 16.0 - (p * 0.5);
        } else {
            // 450 to Immersive: Zoom 15.5 to 15
            float maxDp = mapHeightImmersive / getResources().getDisplayMetrics().density;
            float p = (mapHeightDp - 450f) / (maxDp - 450f);
            currentZoom = 15.5 - (p * 0.5);
        }
        
        binding.mapView.getController().setZoom(Math.max(14.0, currentZoom));

        // 2. Center and Focus logic
        if (assignedGeofence != null && myLocationOverlay != null && myLocationOverlay.getMyLocation() != null) {
            GeoPoint userPos = myLocationOverlay.getMyLocation();
            GeoPoint officePos = new GeoPoint(assignedGeofence.getLatitude(), assignedGeofence.getLongitude());
            
            double midLat = (userPos.getLatitude() + officePos.getLatitude()) / 2.0;
            double midLng = (userPos.getLongitude() + officePos.getLongitude()) / 2.0;
            
            // Offset logic: when sheet is expanded (slideOffset > 0), move center down
            double latOffset = Math.max(0, slideOffset) * 0.0015;
            binding.mapView.getController().setCenter(new GeoPoint(midLat - latOffset, midLng));
        }

        // 3. Adaptive Overlay Visibility
        // Gradually fade out chips as map expands (slideOffset 0 to 0.8)
        float chipAlpha = Math.max(0, 1.0f - (Math.max(0, slideOffset) * 1.25f));
        float chipTranslationY = Math.max(0, slideOffset) * -40f; // Translate up slightly

        binding.layoutMapChips.setAlpha(chipAlpha);
        binding.layoutMapChips.setTranslationY(chipTranslationY);
        
        // Header also fades out slightly in immersive mode
        float headerAlpha = Math.max(0.2f, 1.0f - (Math.max(0, slideOffset) * 0.8f));
        binding.layoutHeader.setAlpha(headerAlpha);

        // 4. Map Visual Connection
        float mapScale = 1.0f - (Math.max(0, slideOffset) * 0.12f);
        binding.mapView.setScaleX(mapScale);
        binding.mapView.setScaleY(mapScale);
        binding.mapView.setPivotY(0);
        
        binding.mapView.setAlpha(1.0f - (Math.max(0, slideOffset) * 0.3f));
        binding.mapView.invalidate();
    }

    private void updateDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE • hh:mm a", Locale.getDefault());
        binding.tvDateTime.setText(sdf.format(new Date()));
        
        // Dynamic Greeting
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String greeting = "Good Night,";
        if (hour >= 5 && hour < 12) greeting = "Good Morning,";
        else if (hour >= 12 && hour < 17) greeting = "Good Afternoon,";
        else if (hour >= 17 && hour < 21) greeting = "Good Evening,";
        
        binding.tvGreeting.setText(greeting);

        // Header Animations (Staggered)
        binding.tvGreeting.setAlpha(0f);
        binding.tvGreeting.setTranslationY(20f);
        binding.tvWelcome.setAlpha(0f);
        binding.tvWelcome.setTranslationY(20f);
        binding.tvDateTime.setAlpha(0f);
        binding.tvDateTime.setTranslationY(20f);

        binding.tvGreeting.animate().alpha(1f).translationY(0).setDuration(500).setStartDelay(300).start();
        binding.tvWelcome.animate().alpha(1f).translationY(0).setDuration(500).setStartDelay(600).start();
        binding.tvDateTime.animate().alpha(1f).translationY(0).setDuration(500).setStartDelay(900).start();
    }

    private void checkAttendanceState() {
        loadWeeklyInsights();
        // Optimized to avoid composite index requirement
        FirebaseHelper.getAttendanceRef()
                .whereEqualTo("userId", FirebaseHelper.getCurrentUserId())
                .addSnapshotListener((query, error) -> {
                    if (error != null) {
                        Log.e("Dashboard", "Attendance state error: " + error.getMessage());
                        return;
                    }

                    if (query != null) {
                        List<AttendanceRecord> records = query.toObjects(AttendanceRecord.class);
                        
                        // Sort records by timestamp descending manually
                        records.sort((a, b) -> {
                            if (a.getTimestamp() == null || b.getTimestamp() == null) return 0;
                            return b.getTimestamp().compareTo(a.getTimestamp());
                        });

                        boolean newIsCheckedIn = false;
                        boolean newIsCheckedOut = false;
                        Date newCheckInTime = null;
                        Date newCheckOutTime = null;
                        
                        timelineItems.clear();
                        long startOfDay = getStartOfDay(new Date()).getTime();

                        // Records are DESCENDING (latest first), but we want to process them chronologically to determine state
                        for (int i = records.size() - 1; i >= 0; i--) {
                            AttendanceRecord r = records.get(i);
                            if (r.getTimestamp() == null || r.getTimestamp().toDate().getTime() < startOfDay) continue;

                            if ("IN".equals(r.getType())) {
                                newIsCheckedIn = true;
                                newIsCheckedOut = false;
                                newCheckInTime = r.getTimestamp().toDate();
                                timelineItems.add(new TimelineAdapter.TimelineItem("Checked In", newCheckInTime, R.drawable.ic_check_circle));
                            } else if ("OUT".equals(r.getType()) || "AUTO_OUT".equals(r.getType())) {
                                newIsCheckedIn = false;
                                newIsCheckedOut = true;
                                newCheckOutTime = r.getTimestamp().toDate();
                                timelineItems.add(new TimelineAdapter.TimelineItem("Checked Out", newCheckOutTime, R.drawable.ic_check_circle));
                            }
                        }
                        
                        boolean finalIsCheckedIn = newIsCheckedIn;
                        boolean finalIsCheckedOut = newIsCheckedOut;
                        Date finalCheckInTime = newCheckInTime;
                        Date finalCheckOutTime = newCheckOutTime;

                        runOnUiThread(() -> {
                            isCheckedIn = finalIsCheckedIn;
                            isCheckedOut = finalIsCheckedOut;
                            checkInTime = finalCheckInTime;
                            checkOutTime = finalCheckOutTime;
                            timelineAdapter.notifyDataSetChanged();
                            handleStateUI();
                            loadInsights(); // Refresh global insights
                            // Trigger proximity UI update to refresh button enabled state
                            if (lastKnownLocation != null) updateProximityUI(lastKnownLocation);
                        });
                    } else {
                        runOnUiThread(() -> {
                            isCheckedIn = false;
                            isCheckedOut = false;
                            timelineItems.clear();
                            timelineAdapter.notifyDataSetChanged();
                            handleStateUI();
                        });
                    }
                });
    }

    private void handleStateUI() {
        TransitionManager.beginDelayedTransition(binding.attendanceBottomSheet);
        
        if (isCheckedOut) {
            binding.btnCheckIn.setVisibility(View.GONE);
            binding.btnCheckOut.setVisibility(View.GONE);
            binding.timerContainer.setVisibility(View.GONE);
            binding.completedContainer.setVisibility(View.VISIBLE);
            
            binding.tvHeroGreeting.setText("Attendance Successfully\nRecorded.");
            binding.attendanceMainCard.setStrokeColor(ContextCompat.getColor(this, R.color.accent_emerald));
            binding.attendanceMainCard.setStrokeWidth((int)(2 * getResources().getDisplayMetrics().density));
            
            // Populate Post-Checkout Stats
            if (checkInTime != null) {
                SimpleDateFormat timeSdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                binding.tvCheckedInAt.setText(timeSdf.format(checkInTime));
                
                Date actualOutTime = checkOutTime != null ? checkOutTime : new Date();
                binding.tvCheckedOutAt.setText(timeSdf.format(actualOutTime)); 

                long diff = actualOutTime.getTime() - checkInTime.getTime();
                long hours = diff / (1000 * 60 * 60);
                long minutes = (diff / (1000 * 60)) % 60;
                binding.tvWorkedToday.setText(String.format(Locale.getDefault(), "%02dh %02dm", hours, minutes));
            }

            stopTimer();
            loadWeeklyInsights(); // Refresh stats for snapshot
        } else if (isCheckedIn) {
            binding.btnCheckIn.setVisibility(View.GONE);
            binding.btnCheckOut.setVisibility(View.VISIBLE);
            binding.btnCheckOut.setEnabled(true);
            binding.timerContainer.setVisibility(View.VISIBLE);
            binding.completedContainer.setVisibility(View.GONE);
            
            binding.attendanceMainCard.setStrokeColor(ContextCompat.getColor(this, R.color.divider));
            binding.attendanceMainCard.setStrokeWidth((int)(1 * getResources().getDisplayMetrics().density));

            binding.tvHeroGreeting.setText("Stay Focused,\nYou're on the clock.");
            
            startTimer();
            calculateEstimatedCheckout();
        } else {
            binding.btnCheckIn.setVisibility(View.VISIBLE);
            binding.btnCheckIn.setText("CHECK IN");
            binding.btnCheckOut.setVisibility(View.GONE);
            binding.timerContainer.setVisibility(View.GONE);
            binding.completedContainer.setVisibility(View.GONE);
            
            binding.attendanceMainCard.setStrokeColor(ContextCompat.getColor(this, R.color.divider));
            binding.attendanceMainCard.setStrokeWidth((int)(1 * getResources().getDisplayMetrics().density));

            binding.tvHeroGreeting.setText("Ready to start\nyour day?");
            
            stopTimer();
        }
    }

    private void loadWeeklyInsights() {
        String uid = FirebaseHelper.getCurrentUserId();
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
        Date startOfWeek = cal.getTime();

        FirebaseHelper.getAttendanceRef()
            .whereEqualTo("userId", uid)
            .whereGreaterThanOrEqualTo("timestamp", new com.google.firebase.Timestamp(startOfWeek))
            .addSnapshotListener((query, error) -> {
                if (error != null || query == null) return;
                List<AttendanceRecord> records = query.toObjects(AttendanceRecord.class);
                
                int presentCount = 0;
                int lateCount = 0;
                java.util.Set<String> uniqueDays = new java.util.HashSet<>();

                for (AttendanceRecord r : records) {
                    if (r.getTimestamp() == null || !"IN".equals(r.getType())) continue;
                    String day = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(r.getTimestamp().toDate());
                    if (uniqueDays.add(day)) {
                        presentCount++;
                        if ("FLAGGED".equals(r.getStatus())) lateCount++;
                    }
                }
                
                int todayIdx = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
                int daysPassed = Math.max(1, todayIdx - Calendar.MONDAY + 1); 
                int absentCount = Math.max(0, daysPassed - presentCount);

                int finalPresent = presentCount;
                int finalLate = lateCount;
                int finalAbsent = absentCount;
                int progress = (presentCount * 100) / Math.max(1, daysPassed);

                runOnUiThread(() -> {
                    binding.tvWeeklyPresent.setText(String.valueOf(finalPresent));
                    binding.tvWeeklyLate.setText(String.valueOf(finalLate));
                    binding.tvWeeklyAbsent.setText(String.valueOf(finalAbsent));
                    binding.weeklyProgressIndicator.setProgress(progress, true);
                });
            });
    }

    private void startTimer() {
        if (workingTimer != null) return;
        workingTimer = new Timer();
        workingTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (checkInTime == null) return;
                long diff = new Date().getTime() - checkInTime.getTime();
                long hours = diff / (1000 * 60 * 60);
                long minutes = (diff / (1000 * 60)) % 60;
                
                String timeStr = String.format(Locale.getDefault(), "%02dh %02dm", hours, minutes);
                runOnUiThread(() -> binding.tvTimer.setText(timeStr));
            }
        }, 0, 1000);
    }

    private void stopTimer() {
        if (workingTimer != null) {
            workingTimer.cancel();
            workingTimer = null;
        }
    }

    private void calculateEstimatedCheckout() {
        if (checkInTime == null) return;
        Calendar cal = Calendar.getInstance();
        cal.setTime(checkInTime);
        cal.add(Calendar.HOUR_OF_DAY, 8); // 8 hours shift
        binding.tvEstCheckout.setText("EST. CHECKOUT: " + new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(cal.getTime()));
    }

    private Date getStartOfDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    private void updateButtonStates() {
        TransitionManager.beginDelayedTransition(binding.attendanceBottomSheet);
    }

    private void performSecureCheckIn() {
        if (isCheckedIn || isCheckedOut) {
            Toast.makeText(this, "Action not allowed in current state", Toast.LENGTH_SHORT).show();
            return;
        }
        if (gpsSamples.size() < 3 || lastKnownLocation == null) {
            Toast.makeText(this, "Validating Signal...", Toast.LENGTH_SHORT).show();
            return;
        }
        if (assignedGeofence == null) {
            Toast.makeText(this, "Please select an office", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, FaceVerificationActivity.class);
        intent.putExtra("type", "IN");
        intent.putExtra("geofenceId", assignedGeofence.getId());
        intent.putExtra("geofenceName", assignedGeofence.getName());
        intent.putExtra("lat", lastKnownLocation.getLatitude());
        intent.putExtra("lng", lastKnownLocation.getLongitude());
        intent.putExtra("acc", lastKnownLocation.getAccuracy());
        intent.putExtra("dist", lastDistanceToOffice);
        startActivity(intent);
    }

    private void performSecureCheckOut() {
        if (!isCheckedIn || isCheckedOut) {
            Toast.makeText(this, "Please check in first", Toast.LENGTH_SHORT).show();
            return;
        }
        if (assignedGeofence == null || lastKnownLocation == null) return;
        Intent intent = new Intent(this, FaceVerificationActivity.class);
        intent.putExtra("type", "OUT");
        intent.putExtra("geofenceId", assignedGeofence.getId());
        intent.putExtra("geofenceName", assignedGeofence.getName());
        intent.putExtra("lat", lastKnownLocation.getLatitude());
        intent.putExtra("lng", lastKnownLocation.getLongitude());
        intent.putExtra("acc", lastKnownLocation.getAccuracy());
        intent.putExtra("dist", lastDistanceToOffice);
        startActivity(intent);
    }

    private void startLocationUpdates() {
        LocationRequest request = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(2000);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult result) {
                Location last = result.getLastLocation();
                if (last != null) {
                    lastKnownLocation = last;
                    gpsSamples.add(last);
                    if (gpsSamples.size() > 5) gpsSamples.remove(0);
                    updateProximityUI(last);
                    
                    if (!hasInitialMapCentered && assignedGeofence != null) {
                        fitMapToUserAndOffice();
                        hasInitialMapCentered = true;
                    }
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(request, locationCallback, null);
        }
    }

    private void updateProximityUI(Location location) {
        if (assignedGeofence == null) return;
        
        // 1. Security Check: Mock Location Detection
        int mockRisk = SecurityManager.getMockRiskScore(location, this);
        boolean isCompromised = SecurityManager.isDeviceCompromised();
        
        if (mockRisk >= 50 || isCompromised) {
            binding.tvStatus.setText("SECURITY ALERT: DEVICE COMPROMISED");
            binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.error));
            binding.btnCheckIn.setEnabled(false);
            return;
        }

        float[] dist = new float[1];
        Location.distanceBetween(location.getLatitude(), location.getLongitude(), 
                assignedGeofence.getLatitude(), assignedGeofence.getLongitude(), dist);
        
        lastDistanceToOffice = dist[0];
        boolean inside = dist[0] <= assignedGeofence.getRadius();

        runOnUiThread(() -> {
            drawGeofence(); // Update map visuals
            if (inside) {
                binding.statusIndicatorDot.setBackgroundResource(R.drawable.dot_active);
                binding.tvStatus.setText("✓ Inside Attendance Zone");
                binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.accent_emerald));
                binding.tvDistanceOffice.setText("Safely inside " + assignedGeofence.getName());
                
                // Map Chips
                binding.tvMapConfidenceChip.setText("✓ Attendance Secured");
                binding.tvMapConfidenceChip.setTextColor(ContextCompat.getColor(this, R.color.accent_emerald));
                
                if (!isCheckedIn && !isCheckedOut) {
                    binding.btnCheckIn.setEnabled(true);
                    binding.btnCheckIn.setAlpha(1.0f);
                }
            } else {
                binding.statusIndicatorDot.setBackgroundResource(R.drawable.dot_inactive);
                binding.tvStatus.setText("⚠ Outside Attendance Zone");
                binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.error));
                binding.tvDistanceOffice.setText(String.format(Locale.getDefault(), "%.0f m from office", lastDistanceToOffice));

                // Map Chips
                binding.tvMapConfidenceChip.setText("⚠ Move Closer to Office");
                binding.tvMapConfidenceChip.setTextColor(ContextCompat.getColor(this, R.color.error));

                if (!isCheckedIn && !isCheckedOut) {
                    binding.btnCheckIn.setEnabled(false);
                    binding.btnCheckIn.setAlpha(0.5f);
                }
            }
        });
    }

    private void fetchUserData() {
        FirebaseUser user = FirebaseHelper.getAuth().getCurrentUser();
        if (user == null) return;

        FirebaseHelper.getUserRef(user.getUid()).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                User u = doc.toObject(User.class);
                if (u != null) {
                    binding.tvWelcome.setText(u.getName());
                    loadOffices();
                }
            }
        });
    }

    private void loadOffices() {
        FirebaseHelper.getGeofencesRef().get().addOnSuccessListener(query -> {
            if (query != null) {
                availableOffices = query.toObjects(GeofenceItem.class);
                if (!availableOffices.isEmpty()) {
                    // By default select first office or check session
                    assignedGeofence = availableOffices.get(0);
                    updateOfficeUI();
                }
            }
        });
    }

    private void showOfficeSelectionDialog() {
        if (availableOffices.isEmpty()) return;
        
        String[] names = new String[availableOffices.size()];
        for (int i = 0; i < availableOffices.size(); i++) names[i] = availableOffices.get(i).getName();
        
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Select Office Location")
            .setItems(names, (dialog, which) -> {
                assignedGeofence = availableOffices.get(which);
                updateOfficeUI();
            })
            .show();
    }

    private void updateOfficeUI() {
        if (assignedGeofence != null) {
            String branch = assignedGeofence.getAddress() != null ? assignedGeofence.getAddress() : "Main Campus";
            binding.tvAssignedOffice.setText(assignedGeofence.getName());
            binding.tvMapOfficeChip.setText(assignedGeofence.getName() + " • " + branch);
            drawGeofence();
            fitMapToUserAndOffice();
        }
    }

    private void fitMapToUserAndOffice() {
        if (assignedGeofence == null || binding == null) return;
        
        GeoPoint officePos = new GeoPoint(assignedGeofence.getLatitude(), assignedGeofence.getLongitude());
        
        if (myLocationOverlay != null && myLocationOverlay.getMyLocation() != null) {
            GeoPoint userPos = myLocationOverlay.getMyLocation();
            boolean isInside = lastDistanceToOffice <= assignedGeofence.getRadius();
            
            if (isInside) {
                // Focus on the safe zone
                binding.mapView.getController().animateTo(officePos);
                binding.mapView.getController().setZoom(19.0); // Close zoom for reassurance
            } else {
                // Show both
                double centerLat = (officePos.getLatitude() + userPos.getLatitude()) / 2.0;
                double centerLng = (officePos.getLongitude() + userPos.getLongitude()) / 2.0;
                binding.mapView.getController().animateTo(new GeoPoint(centerLat, centerLng));
                binding.mapView.getController().setZoom(17.5);
            }
        } else {
            binding.mapView.getController().animateTo(officePos);
            binding.mapView.getController().setZoom(18.0);
        }
    }

    private void drawGeofence() {
        if (assignedGeofence == null) return;
        
        boolean isInside = lastDistanceToOffice <= assignedGeofence.getRadius();
        
        // Clear existing geofence overlays but keep My Location and Pulse
        List<org.osmdroid.views.overlay.Overlay> overlays = binding.mapView.getOverlays();
        for (int i = overlays.size() - 1; i >= 0; i--) {
            org.osmdroid.views.overlay.Overlay overlay = overlays.get(i);
            if (overlay instanceof Polygon && overlay != pulseCircle) {
                overlays.remove(i);
            } else if (overlay instanceof Marker && "Office".equals(((Marker)overlay).getId())) {
                overlays.remove(i);
            }
        }
        
        GeoPoint center = new GeoPoint(assignedGeofence.getLatitude(), assignedGeofence.getLongitude());
        
        // 1. Geofence Circle with dynamic styling
        Polygon circle = new Polygon();
        circle.setPoints(Polygon.pointsAsCircle(center, assignedGeofence.getRadius()));
        
        if (isInside) {
            // "Safe Harbor" Emerald Glow
            circle.getFillPaint().setColor(0x301EC98E); // Increased opacity (0x30)
            circle.getOutlinePaint().setColor(0xFF1EC98E); // Solid Emerald border
            circle.getOutlinePaint().setStrokeWidth(6f);
        } else {
            // "Searching" Blue/Neutral state
            circle.getFillPaint().setColor(0x103B82F6); 
            circle.getOutlinePaint().setColor(0x803B82F6);
            circle.getOutlinePaint().setStrokeWidth(3f);
        }
        binding.mapView.getOverlays().add(0, circle);

        // 2. HQ Office Marker with dynamic icon
        Marker officeMarker = new Marker(binding.mapView);
        officeMarker.setId("Office");
        officeMarker.setPosition(center);
        officeMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        
        Drawable icon = ContextCompat.getDrawable(this, R.drawable.ic_location_pin);
        if (icon != null) {
            int color = isInside ? R.color.accent_emerald : R.color.accent_blue;
            icon.setTint(ContextCompat.getColor(this, color));
            officeMarker.setIcon(icon);
        }
        binding.mapView.getOverlays().add(officeMarker);

        // Ensure My Location stays on top
        if (myLocationOverlay != null) {
            overlays.remove(myLocationOverlay);
            overlays.add(myLocationOverlay);
        }

        binding.mapView.invalidate();
    }

    private void loadInsights() {
        String uid = FirebaseHelper.getCurrentUserId();
        FirebaseHelper.getAttendanceRef()
            .whereEqualTo("userId", uid)
            .addSnapshotListener((query, error) -> {
                if (error != null || query == null || query.isEmpty()) {
                    if (query == null || query.isEmpty()) {
                        binding.tvStreakCount.setText("0 Days");
                        binding.tvAttendancePercent.setText("0%");
                    }
                    return;
                }
                
                List<com.geoattend.model.AttendanceRecord> records = query.toObjects(com.geoattend.model.AttendanceRecord.class);
                
                // 1. Calculate Percentage
                java.util.Set<String> presentDays = new java.util.HashSet<>();
                for (com.geoattend.model.AttendanceRecord r : records) {
                    if (r.getTimestamp() != null && "IN".equals(r.getType())) {
                        presentDays.add(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(r.getTimestamp().toDate()));
                    }
                }
                
                // Current Month progress
                Calendar cal = Calendar.getInstance();
                int daysPassedInMonth = cal.get(Calendar.DAY_OF_MONTH);
                int percent = (presentDays.size() * 100) / Math.max(1, daysPassedInMonth);
                binding.tvAttendancePercent.setText(Math.min(100, percent) + "%");

                // 2. Calculate Streak
                int streak = 0;
                cal = Calendar.getInstance();
                String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());
                
                // If not checked in today, streak starts from yesterday
                if (!presentDays.contains(today)) {
                    cal.add(Calendar.DAY_OF_YEAR, -1);
                }

                while (true) {
                    String dateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());
                    if (presentDays.contains(dateStr)) {
                        streak++;
                        cal.add(Calendar.DAY_OF_YEAR, -1);
                    } else {
                        // Check if weekend (Saturdays and Sundays don't break streak)
                        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
                        if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
                            cal.add(Calendar.DAY_OF_YEAR, -1);
                            continue;
                        }
                        break;
                    }
                    if (streak > 365) break; // Safety
                }
                binding.tvStreakCount.setText(streak + " Days");
            });
    }

    private void loadWeeklyProgress() {
        String uid = FirebaseHelper.getCurrentUserId();
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        
        // Start from Monday of current week
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        long weekStart = cal.getTimeInMillis();

        List<Long> dayStartTimes = new ArrayList<>();
        for (int j = 0; j < 7; j++) {
            dayStartTimes.add(cal.getTimeInMillis());
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        FirebaseHelper.getAttendanceRef()
                .whereEqualTo("userId", uid)
                .whereGreaterThanOrEqualTo("timestamp", new com.google.firebase.Timestamp(new Date(weekStart)))
                .addSnapshotListener((query, error) -> {
                    if (error != null || query == null) return;
                    List<AttendanceRecord> records = query.toObjects(AttendanceRecord.class);
                    
                    float[] dailyHours = new float[7];
                    
                    // Group records by day
                    for (int i = 0; i < 7; i++) {
                        long dayStart = dayStartTimes.get(i);
                        long dayEnd = dayStart + (24 * 60 * 60 * 1000L);
                        
                        Date checkIn = null;
                        Date checkOut = null;
                        
                        for (AttendanceRecord r : records) {
                            if (r.getTimestamp() == null) continue;
                            long time = r.getTimestamp().toDate().getTime();
                            
                            if (time >= dayStart && time < dayEnd) {
                                if ("IN".equals(r.getType())) checkIn = r.getTimestamp().toDate();
                                else if ("OUT".equals(r.getType()) || "AUTO_OUT".equals(r.getType())) checkOut = r.getTimestamp().toDate();
                            }
                        }
                        
                        if (checkIn != null && checkOut != null) {
                            long diff = checkOut.getTime() - checkIn.getTime();
                            dailyHours[i] = diff / (1000f * 60 * 60); // Convert to hours
                        } else if (checkIn != null) {
                            // If currently checked in today, calculate up to now
                            long now = System.currentTimeMillis();
                            if (now >= dayStart && now < dayEnd) {
                                long diff = now - checkIn.getTime();
                                dailyHours[i] = diff / (1000f * 60 * 60);
                            }
                        }
                    }

                    updateUserChart(dailyHours);
                });
    }

    private void updateUserChart(float[] dailyHours) {
        LineChart chart = binding.weeklyLineChart;
        List<Entry> entries = new ArrayList<>();
        String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        
        float totalHours = 0;
        int activeDays = 0;
        for (int i = 0; i < dailyHours.length; i++) {
            entries.add(new Entry(i, dailyHours[i]));
            if (dailyHours[i] > 0) {
                totalHours += dailyHours[i];
                activeDays++;
            }
        }
        
        float avgHours = activeDays > 0 ? totalHours / activeDays : 0;
        final String avgStr = String.format(Locale.getDefault(), "%.1fh Avg", avgHours);
        
        runOnUiThread(() -> {
            binding.tvWeeklyAvg.setText(avgStr);
            setupChartStyle(chart, entries, days);
        });
    }

    private void setupChartStyle(LineChart chart, List<Entry> entries, String[] days) {
        LineDataSet dataSet = new LineDataSet(entries, "Hours Worked");
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawFilled(true);
        dataSet.setDrawCircles(true);
        dataSet.setCircleColor(ContextCompat.getColor(this, R.color.accent_emerald));
        dataSet.setCircleRadius(3f);
        dataSet.setLineWidth(2f);
        dataSet.setColor(ContextCompat.getColor(this, R.color.accent_emerald));
        dataSet.setDrawValues(false);
        
        // Gradient Fill
        dataSet.setFillDrawable(ContextCompat.getDrawable(this, R.drawable.chart_gradient_emerald));
        
        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);
        
        // General Chart Styling
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setTouchEnabled(false);
        chart.setScaleEnabled(false);
        chart.setPinchZoom(false);
        chart.setDrawGridBackground(false);
        chart.setExtraOffsets(5, 5, 5, 5);
        
        // X-Axis
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(false);
        xAxis.setTextColor(ContextCompat.getColor(this, R.color.text_disabled));
        xAxis.setTextSize(9f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(days));
        xAxis.setLabelCount(7);
        xAxis.setGranularity(1f);
        
        // Y-Axis
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(ContextCompat.getColor(this, R.color.divider));
        leftAxis.setDrawAxisLine(false);
        leftAxis.setTextColor(ContextCompat.getColor(this, R.color.text_disabled));
        leftAxis.setTextSize(9f);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(12f); // Max 12 hours display
        leftAxis.setLabelCount(3);
        
        chart.getAxisRight().setEnabled(false);
        chart.animateY(1000);
        chart.invalidate();
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 2001);
        } else {
            startLocationUpdates();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (fusedLocationClient != null && locationCallback != null) fusedLocationClient.removeLocationUpdates(locationCallback);
    }
}
