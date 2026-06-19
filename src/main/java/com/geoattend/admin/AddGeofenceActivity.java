package com.geoattend.admin;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.geoattend.R;
import com.geoattend.databinding.ActivityAddGeofenceBinding;
import com.geoattend.model.GeofenceItem;
import com.geoattend.utils.FirebaseHelper;
import com.geoattend.utils.GeofenceHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;
import java.util.List;

public class AddGeofenceActivity extends AppCompatActivity {
    private ActivityAddGeofenceBinding binding;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private GeoPoint selectedCenter = new GeoPoint(12.9716, 77.5946); // Default Bangalore
    private Marker centerMarker;
    private Polygon geofenceCircle;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddGeofenceBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        initMap();
        checkPermissions();

        binding.btnSave.setOnClickListener(v -> saveGeofence());
        binding.btnBack.setOnClickListener(v -> finish());
        binding.fabCurrentLocation.setOnClickListener(v -> getCurrentLocation());

        binding.sliderRadius.addOnChangeListener((slider, value, fromUser) -> {
            binding.tvRadiusValue.setText((int) value + "m");
            updateVisualCircle();
        });

        checkEditMode();
    }

    private void checkEditMode() {
        if (getIntent().hasExtra("geofenceId")) {
            binding.tvHeader.setText("Update Zone");
            binding.etName.setText(getIntent().getStringExtra("name"));
            binding.etAddress.setText(getIntent().getStringExtra("address"));
            binding.sliderRadius.setValue(getIntent().getFloatExtra("radius", 150f));
            binding.btnSave.setText("UPDATE GEOFENCE ZONE");
            
            selectedCenter = new GeoPoint(getIntent().getDoubleExtra("lat", 0.0), getIntent().getDoubleExtra("lng", 0.0));
            updateVisualCircle();
            binding.mapView.getController().animateTo(selectedCenter);
            if (centerMarker != null) centerMarker.setPosition(selectedCenter);
        }
    }

    private void initMap() {
        binding.mapView.setMultiTouchControls(true);
        binding.mapView.getController().setZoom(17.0);
        binding.mapView.getController().setCenter(selectedCenter);
        
        com.geoattend.utils.MapUtils.applyPremiumDarkStyle(binding.mapView);

        centerMarker = new Marker(binding.mapView);
        centerMarker.setPosition(selectedCenter);
        centerMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        centerMarker.setTitle("Geofence Center");
        centerMarker.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_location_pin));
        centerMarker.getIcon().setTint(ContextCompat.getColor(this, R.color.accent_blue));
        binding.mapView.getOverlays().add(centerMarker);

        MapEventsReceiver receiver = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                selectedCenter = p;
                centerMarker.setPosition(p);
                updateVisualCircle();
                binding.mapView.invalidate();
                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) { return false; }
        };

        binding.mapView.getOverlays().add(new MapEventsOverlay(receiver));
    }

    private void updateVisualCircle() {
        if (geofenceCircle != null) {
            binding.mapView.getOverlays().remove(geofenceCircle);
        }

        float radius = binding.sliderRadius.getValue();
        geofenceCircle = new Polygon();
        List<GeoPoint> circlePoints = Polygon.pointsAsCircle(selectedCenter, radius);
        geofenceCircle.setPoints(circlePoints);
        geofenceCircle.setFillColor(0x102563EB); // Faint primary blue
        geofenceCircle.setStrokeColor(0x802563EB); // Semi-transparent blue stroke
        geofenceCircle.setStrokeWidth(2f);
        binding.mapView.getOverlays().add(geofenceCircle);
        binding.mapView.invalidate();
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getCurrentLocation();
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        selectedCenter = new GeoPoint(location.getLatitude(), location.getLongitude());
                        binding.mapView.getController().animateTo(selectedCenter);
                        centerMarker.setPosition(selectedCenter);
                        updateVisualCircle();
                    } else {
                        Toast.makeText(this, "Unable to get current location", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveGeofence() {
        String name = binding.etName.getText().toString().trim();
        String address = binding.etAddress.getText().toString().trim();
        float radius = binding.sliderRadius.getValue();

        if (name.isEmpty() || address.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // One office = One Geofence. We check if a geofence with this name already exists.
        FirebaseHelper.getGeofencesRef().whereEqualTo("name", name).get().addOnSuccessListener(queryDocumentSnapshots -> {
            if (!queryDocumentSnapshots.isEmpty()) {
                // Update existing
                String docId = queryDocumentSnapshots.getDocuments().get(0).getId();
                updateGeofence(docId, name, address, radius);
            } else {
                // Create new
                createNewGeofence(name, address, radius);
            }
        });
    }

    private void createNewGeofence(String name, String address, float radius) {
        GeofenceItem item = new GeofenceItem(null, name, selectedCenter.getLatitude(), selectedCenter.getLongitude(), radius, address);
        FirebaseHelper.getGeofencesRef().add(item).addOnSuccessListener(doc -> {
            String id = doc.getId();
            item.setId(id);
            // Save the ID back into the document for consistency
            doc.set(item);
            
            registerSystemGeofence(id, radius);
            Toast.makeText(this, "Geofence Created", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void updateGeofence(String id, String name, String address, float radius) {
        GeofenceItem item = new GeofenceItem(id, name, selectedCenter.getLatitude(), selectedCenter.getLongitude(), radius, address);
        FirebaseHelper.getGeofencesRef().document(id).set(item).addOnSuccessListener(aVoid -> {
            registerSystemGeofence(id, radius);
            Toast.makeText(this, "Geofence Updated", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void registerSystemGeofence(String id, float radius) {
        new GeofenceHelper(this).addGeofence(id, selectedCenter.getLatitude(), selectedCenter.getLongitude(), radius);
    }

    @Override
    protected void onResume() {
        super.onResume();
        binding.mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        binding.mapView.onPause();
    }
}
