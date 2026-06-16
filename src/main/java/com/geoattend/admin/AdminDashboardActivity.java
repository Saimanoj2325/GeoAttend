package com.geoattend.admin;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.transition.TransitionManager;
import android.view.ViewGroup;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.geoattend.LoginActivity;
import com.geoattend.R;
import com.geoattend.databinding.ActivityAdminDashboardBinding;
import com.geoattend.model.GeofenceItem;
import com.geoattend.utils.FirebaseHelper;
import com.google.firebase.firestore.DocumentSnapshot;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;
import java.util.List;

public class AdminDashboardActivity extends AppCompatActivity {
    private ActivityAdminDashboardBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initAdminMap();
        initAdminNavigation();
        loadKPIData();

        binding.btnLogout.setOnClickListener(v -> {
            FirebaseHelper.getAuth().signOut();
            startActivity(new Intent(this, LoginActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finishAffinity();
        });

        binding.btnManageZones.setOnClickListener(v -> {
            startActivity(new Intent(this, GeofenceManagerActivity.class));
        });

        binding.cardSecurityCenter.setOnClickListener(v -> {
            startActivity(new Intent(this, SecurityCenterActivity.class));
        });

        loadOfficesAndEmployeesOnMap();
        
        // Micro-interaction: Smooth content entrance
        binding.adminBottomSheet.setTranslationY(200f);
        binding.adminBottomSheet.animate().translationY(0).setDuration(800).start();
    }

    private void initAdminMap() {
        binding.adminMapView.setMultiTouchControls(true);
        binding.adminMapView.getController().setZoom(12.0);
        binding.adminMapView.getController().setCenter(new GeoPoint(12.9716, 77.5946));
    }

    private void initAdminNavigation() {
        binding.adminBottomNavigation.setSelectedItemId(R.id.nav_admin_dashboard);
        binding.adminBottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_employees) {
                startActivity(new Intent(this, GeofenceManagerActivity.class));
                return true;
            } else if (id == R.id.nav_admin_attendance) {
                // To be implemented: AdminAttendanceHistory
                return true;
            }
            return true;
        });
    }

    private void loadKPIData() {
        FirebaseHelper.getFirestore().collection("users")
                .whereEqualTo("role", "employee")
                .get()
                .addOnSuccessListener(query -> {
                    TransitionManager.beginDelayedTransition((ViewGroup) binding.getRoot());
                    binding.tvTotalEmployees.setText(String.valueOf(query.size()));
                });

        FirebaseHelper.getAttendanceRef()
                .whereEqualTo("type", "IN")
                .get()
                .addOnSuccessListener(query -> {
                    binding.tvPresentCount.setText(String.valueOf(query.size()));
                });

        FirebaseHelper.getAttendanceRef()
                .whereIn("status", List.of("REJECTED", "FLAGGED"))
                .get()
                .addOnSuccessListener(query -> {
                    binding.tvSecurityAlerts.setText(String.valueOf(query.size()));
                });
    }

    private void loadOfficesAndEmployeesOnMap() {
        FirebaseHelper.getGeofencesRef().addSnapshotListener((query, error) -> {
            if (error != null) return;
            if (query != null) {
                binding.adminMapView.getOverlays().clear(); // Clear existing
                for (DocumentSnapshot doc : query.getDocuments()) {
                    GeofenceItem office = doc.toObject(GeofenceItem.class);
                    if (office != null) addOfficeToMap(office);
                }
                binding.adminMapView.invalidate();
            }
        });
    }

    private void addOfficeToMap(GeofenceItem office) {
        GeoPoint center = new GeoPoint(office.getLatitude(), office.getLongitude());
        Marker marker = new Marker(binding.adminMapView);
        marker.setPosition(center);
        marker.setTitle(office.getName());
        marker.setSnippet("Radius: " + (int)office.getRadius() + "m");
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        
        android.graphics.drawable.Drawable icon = ContextCompat.getDrawable(this, android.R.drawable.ic_menu_myplaces);
        if (icon != null) {
            icon.setTint(ContextCompat.getColor(this, R.color.accent_blue));
            marker.setIcon(icon);
        }
        binding.adminMapView.getOverlays().add(marker);

        Polygon circle = new Polygon(binding.adminMapView);
        circle.setPoints(Polygon.pointsAsCircle(center, office.getRadius()));
        circle.getFillPaint().setColor(0x153B82F6);
        circle.getOutlinePaint().setColor(0x443B82F6);
        circle.getOutlinePaint().setStrokeWidth(2f);
        binding.adminMapView.getOverlays().add(circle);
    }
}
