package com.geoattend.admin;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.geoattend.databinding.ActivityGeofenceManagerBinding;
import com.geoattend.model.GeofenceItem;
import com.geoattend.utils.FirebaseHelper;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class GeofenceManagerActivity extends AppCompatActivity {
    private ActivityGeofenceManagerBinding binding;
    private List<GeofenceItem> geofenceList = new ArrayList<>();
    private GeofenceAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGeofenceManagerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        adapter = new GeofenceAdapter(geofenceList);
        binding.rvGeofences.setLayoutManager(new LinearLayoutManager(this));
        binding.rvGeofences.setAdapter(adapter);
        
        binding.fabAddGeofence.setOnClickListener(v -> 
            startActivity(new Intent(this, AddGeofenceActivity.class))
        );

        binding.btnBack.setOnClickListener(v -> finish());

        loadGeofences();
    }

    private void loadGeofences() {
        FirebaseHelper.getGeofencesRef().addSnapshotListener((value, error) -> {
            if (value != null) {
                geofenceList.clear();
                for (DocumentSnapshot doc : value.getDocuments()) {
                    GeofenceItem item = doc.toObject(GeofenceItem.class);
                    if (item != null) {
                        item.setId(doc.getId());
                        geofenceList.add(item);
                    }
                }
                adapter.notifyDataSetChanged();
            }
        });
    }
}
