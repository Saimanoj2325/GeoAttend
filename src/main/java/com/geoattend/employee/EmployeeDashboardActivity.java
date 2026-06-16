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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
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

public class EmployeeDashboardActivity extends AppCompatActivity {
    private ActivityEmployeeDashboardBinding binding;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private MyLocationNewOverlay myLocationOverlay;
    private GeofenceItem assignedGeofence;
    private List<Location> gpsSamples = new ArrayList<>();
    private boolean isCheckedIn = false;
    private boolean isCheckedOut = false;
    private List<GeofenceItem> availableOffices = new ArrayList<>();
    private Timer workingTimer;
    private Date checkInTime;
    private Location lastKnownLocation;
    private float lastDistanceToOffice = 0f;
    
    private List<TimelineAdapter.TimelineItem> timelineItems = new ArrayList<>();
    private TimelineAdapter timelineAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEmployeeDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

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
        
        binding.tvAssignedOffice.setOnClickListener(v -> showOfficeSelectionDialog());

        binding.btnLogout.setOnClickListener(v -> {
            FirebaseHelper.getAuth().signOut();
            startActivity(new Intent(this, LoginActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finishAffinity();
        });

        binding.fabRecenter.setOnClickListener(v -> {
            if (myLocationOverlay.getMyLocation() != null) {
                binding.mapView.getController().animateTo(myLocationOverlay.getMyLocation());
            }
        });

        checkPermissions();
        fetchUserData();
        loadInsights();
        
        // Micro-interaction: Smooth entrance for bottom sheet area
        binding.attendanceBottomSheet.setTranslationY(200f);
        binding.attendanceBottomSheet.animate().translationY(0).setDuration(800).start();
    }

    private void initTimeline() {
        timelineAdapter = new TimelineAdapter(timelineItems);
        binding.rvTimeline.setLayoutManager(new LinearLayoutManager(this));
        binding.rvTimeline.setAdapter(timelineAdapter);
    }

    private void initMap() {
        binding.mapView.setMultiTouchControls(true);
        binding.mapView.getController().setZoom(18.0);
        
        // Custom My Location Overlay with premium dot
        myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), binding.mapView);
        myLocationOverlay.enableMyLocation();
        binding.mapView.getOverlays().add(myLocationOverlay);
    }

    private void initNavigation() {
        binding.bottomNavigation.setSelectedItemId(R.id.nav_dashboard);
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_attendance) {
                startActivity(new Intent(this, AttendanceHistoryActivity.class));
                overridePendingTransition(0, 0);
            } else if (id == R.id.nav_notifications) {
                startActivity(new Intent(this, NotificationsActivity.class));
                overridePendingTransition(0, 0);
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                overridePendingTransition(0, 0);
            }
            return true;
        });
    }

    private void updateDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd MMMM • hh:mm a", Locale.getDefault());
        binding.tvDateTime.setText(sdf.format(new Date()));
        
        // Dynamic Greeting
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String greeting = "Good Night";
        if (hour >= 5 && hour < 12) greeting = "Good Morning";
        else if (hour >= 12 && hour < 17) greeting = "Good Afternoon";
        else if (hour >= 17 && hour < 21) greeting = "Good Evening";
        
        String currentText = binding.tvWelcome.getText().toString();
        String name = currentText.contains(",") ? currentText.split(",")[1].trim() : "User";
        binding.tvWelcome.setText(greeting + ", " + name);
    }

    private void checkAttendanceState() {
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
                        Collections.sort(records, (a, b) -> {
                            if (a.getTimestamp() == null || b.getTimestamp() == null) return 0;
                            return b.getTimestamp().compareTo(a.getTimestamp());
                        });

                        boolean newIsCheckedIn = false;
                        boolean newIsCheckedOut = false;
                        Date newCheckInTime = null;
                        
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
                                timelineItems.add(new TimelineAdapter.TimelineItem("Checked In", newCheckInTime, android.R.drawable.checkbox_on_background));
                            } else if ("OUT".equals(r.getType()) || "AUTO_OUT".equals(r.getType())) {
                                newIsCheckedIn = false;
                                newIsCheckedOut = true;
                                timelineItems.add(new TimelineAdapter.TimelineItem("Checked Out", r.getTimestamp().toDate(), android.R.drawable.checkbox_on_background));
                            }
                        }
                        
                        boolean finalIsCheckedIn = newIsCheckedIn;
                        boolean finalIsCheckedOut = newIsCheckedOut;
                        Date finalCheckInTime = newCheckInTime;

                        runOnUiThread(() -> {
                            isCheckedIn = finalIsCheckedIn;
                            isCheckedOut = finalIsCheckedOut;
                            checkInTime = finalCheckInTime;
                            timelineAdapter.notifyDataSetChanged();
                            handleStateUI();
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
        updateButtonStates();
        if (isCheckedOut) {
            binding.btnCheckIn.setVisibility(View.VISIBLE);
            binding.btnCheckIn.setEnabled(false);
            binding.btnCheckIn.setText("DAY COMPLETED");
            binding.btnCheckOut.setVisibility(View.GONE);
            binding.tvHeroGreeting.setText("Good Job!\nSee you tomorrow");
            stopTimer();
            binding.timerContainer.setVisibility(View.VISIBLE);
        } else if (isCheckedIn) {
            binding.btnCheckIn.setVisibility(View.GONE);
            binding.btnCheckOut.setVisibility(View.VISIBLE);
            binding.btnCheckOut.setEnabled(true);
            binding.tvHeroGreeting.setText("Work in Progress\nStay Productive");
            startTimer();
            binding.timerContainer.setVisibility(View.VISIBLE);
            calculateEstimatedCheckout();
        } else {
            binding.btnCheckIn.setVisibility(View.VISIBLE);
            binding.btnCheckIn.setEnabled(true);
            binding.btnCheckIn.setText("CHECK IN");
            binding.btnCheckOut.setVisibility(View.GONE);
            binding.tvHeroGreeting.setText("Ready to start\nyour day");
            stopTimer();
            binding.timerContainer.setVisibility(View.GONE);
        }
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
                long seconds = (diff / 1000) % 60;
                
                String timeStr = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
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
        boolean nearBoundary = !inside && dist[0] <= assignedGeofence.getRadius() + 20; // Within 20m of edge
        
        // Micro-interaction: Smooth state transition
        TransitionManager.beginDelayedTransition((ViewGroup) binding.getRoot());
        
        if (inside) {
            binding.tvStatus.setText("INSIDE GEOFENCE");
            binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.accent_emerald));
            binding.statusIndicatorDot.setBackgroundResource(R.drawable.circle_emerald);
        } else if (nearBoundary) {
            binding.tvStatus.setText("NEAR BOUNDARY");
            binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.accent_gold));
            binding.statusIndicatorDot.setBackgroundResource(R.drawable.circle_primary); // Gold-ish
        } else {
            binding.tvStatus.setText("OUTSIDE GEOFENCE");
            binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.error));
            binding.statusIndicatorDot.setBackgroundResource(R.drawable.circle_error);
        }
        
        binding.btnCheckIn.setEnabled(inside && !isCheckedIn && !isCheckedOut);
        binding.btnCheckOut.setEnabled(inside && isCheckedIn);
        
        float distanceToEdge = Math.abs(dist[0] - assignedGeofence.getRadius());
        String detailText = inside ? String.format(Locale.getDefault(), "• %.0fm from center", dist[0]) 
                                  : String.format(Locale.getDefault(), "• %.0fm to boundary", distanceToEdge);
        binding.tvDistanceOffice.setText(detailText);
    }

    private void fetchUserData() {
        FirebaseUser fUser = FirebaseHelper.getAuth().getCurrentUser();
        if (fUser != null) {
            binding.ivEmailStatus.setImageResource(fUser.isEmailVerified() ? android.R.drawable.checkbox_on_background : android.R.drawable.checkbox_off_background);
            binding.ivEmailStatus.setColorFilter(ContextCompat.getColor(this, fUser.isEmailVerified() ? R.color.accent_emerald : R.color.error));
        }

        FirebaseHelper.getUserRef(FirebaseHelper.getCurrentUserId()).get().addOnSuccessListener(doc -> {
            User user = doc.toObject(User.class);
            if (user != null) {
                String greeting = binding.tvWelcome.getText().toString().split(",")[0];
                binding.tvWelcome.setText(greeting + ", " + user.getName().split(" ")[0]);
                
                binding.ivDeviceStatus.setImageResource(user.isDeviceEnrolled() ? android.R.drawable.checkbox_on_background : android.R.drawable.checkbox_off_background);
                binding.ivDeviceStatus.setColorFilter(ContextCompat.getColor(this, user.isDeviceEnrolled() ? R.color.accent_emerald : R.color.error));
                
                binding.ivFaceStatus.setImageResource(user.isFaceRegistered() ? android.R.drawable.checkbox_on_background : android.R.drawable.checkbox_off_background);
                binding.ivFaceStatus.setColorFilter(ContextCompat.getColor(this, user.isFaceRegistered() ? R.color.accent_emerald : R.color.error));

                loadOffices();
            }
        });
    }

    private void loadOffices() {
        FirebaseHelper.getGeofencesRef().get().addOnSuccessListener(query -> {
            if (!query.isEmpty()) {
                availableOffices.clear();
                for (DocumentSnapshot doc : query.getDocuments()) {
                    GeofenceItem item = doc.toObject(GeofenceItem.class);
                    if (item != null) {
                        item.setId(doc.getId()); // Crucial: Document ID is the Geofence ID
                        availableOffices.add(item);
                    }
                }

                if (assignedGeofence == null && !availableOffices.isEmpty()) {
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
            binding.tvAssignedOffice.setText(assignedGeofence.getName());
            drawGeofence();
        }
    }

    private void drawGeofence() {
        if (assignedGeofence == null) return;
        
        // Clear existing geofence overlays
        List<org.osmdroid.views.overlay.Overlay> overlays = binding.mapView.getOverlays();
        for (int i = overlays.size() - 1; i >= 0; i--) {
            if (overlays.get(i) instanceof Polygon || (overlays.get(i) instanceof Marker && !"Me".equals(((Marker)overlays.get(i)).getId()))) {
                overlays.remove(i);
            }
        }
        
        GeoPoint center = new GeoPoint(assignedGeofence.getLatitude(), assignedGeofence.getLongitude());
        
        // Premium Office Marker
        Marker officeMarker = new Marker(binding.mapView);
        officeMarker.setId("Office");
        officeMarker.setPosition(center);
        officeMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        officeMarker.setTitle(assignedGeofence.getName());
        Drawable icon = ContextCompat.getDrawable(this, android.R.drawable.ic_menu_myplaces);
        if (icon != null) {
            icon.setTint(ContextCompat.getColor(this, R.color.accent_blue));
            officeMarker.setIcon(icon);
        }
        binding.mapView.getOverlays().add(officeMarker);

        Polygon circle = new Polygon();
        circle.setPoints(Polygon.pointsAsCircle(center, assignedGeofence.getRadius()));
        circle.getFillPaint().setColor(0x153B82F6);
        circle.getOutlinePaint().setColor(0x443B82F6);
        circle.getOutlinePaint().setStrokeWidth(3);
        binding.mapView.getOverlays().add(circle);

        binding.mapView.getController().animateTo(center);
        binding.mapView.invalidate();
    }

    private void loadInsights() {
        String uid = FirebaseHelper.getCurrentUserId();
        FirebaseHelper.getAttendanceRef()
            .whereEqualTo("userId", uid)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener(query -> {
                if (query == null || query.isEmpty()) {
                    binding.tvStreakCount.setText("0 Days");
                    binding.tvAttendancePercent.setText("0%");
                    return;
                }
                
                List<com.geoattend.model.AttendanceRecord> records = query.toObjects(com.geoattend.model.AttendanceRecord.class);
                
                // 1. Calculate Percentage (Simple: Present Days / Total Work Days in month)
                // For now: Present Days / 30
                java.util.Set<String> presentDays = new java.util.HashSet<>();
                for (com.geoattend.model.AttendanceRecord r : records) {
                    if (r.getTimestamp() != null) {
                        presentDays.add(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(r.getTimestamp().toDate()));
                    }
                }
                int percent = Math.min(100, (presentDays.size() * 100) / 22); // Assuming 22 work days/month
                binding.tvAttendancePercent.setText(percent + "%");

                // 2. Calculate Streak
                int streak = 0;
                Calendar cal = Calendar.getInstance();
                String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());
                
                if (!presentDays.contains(today)) {
                    cal.add(Calendar.DAY_OF_YEAR, -1); // Start from yesterday
                }

                while (true) {
                    String dateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());
                    if (presentDays.contains(dateStr)) {
                        streak++;
                        cal.add(Calendar.DAY_OF_YEAR, -1);
                    } else {
                        // Check if it was a weekend
                        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
                        if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
                            cal.add(Calendar.DAY_OF_YEAR, -1);
                            continue;
                        }
                        break;
                    }
                }
                binding.tvStreakCount.setText(streak + " Days");
            });
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
