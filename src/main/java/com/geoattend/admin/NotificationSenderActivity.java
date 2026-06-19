package com.geoattend.admin;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.geoattend.databinding.ActivityNotificationSenderBinding;
import com.geoattend.model.GeofenceItem;
import com.geoattend.utils.FirebaseHelper;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.geoattend.R;
import com.geoattend.utils.AdminNavigationHelper;

public class NotificationSenderActivity extends AppCompatActivity {
    private ActivityNotificationSenderBinding binding;
    private List<GeofenceItem> geofenceList = new ArrayList<>();
    private List<String> officeNames = new ArrayList<>();
    private ArrayAdapter<String> officeAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNotificationSenderBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());

        AdminNavigationHelper.init(this, R.id.nav_messages);
        
        officeAdapter = new ArrayAdapter<>(this, R.layout.item_spinner, officeNames);
        officeAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        binding.spinnerOffice.setAdapter(officeAdapter);

        loadOffices();

        binding.btnSend.setOnClickListener(v -> validateAndSend());
    }

    private void loadOffices() {
        FirebaseHelper.getGeofencesRef().get().addOnSuccessListener(query -> {
            geofenceList.clear();
            officeNames.clear();
            officeNames.add("All Registered Employees");
            for (DocumentSnapshot doc : query.getDocuments()) {
                GeofenceItem item = doc.toObject(GeofenceItem.class);
                if (item != null) {
                    item.setId(doc.getId());
                    geofenceList.add(item);
                    officeNames.add(item.getName());
                }
            }
            officeAdapter.notifyDataSetChanged();
        });
    }

    private void validateAndSend() {
        final String title = binding.etTitle.getText().toString().trim();
        final String message = binding.etMessage.getText().toString().trim();
        int selectedPos = binding.spinnerOffice.getSelectedItemPosition();

        if (title.isEmpty() || message.isEmpty()) {
            Toast.makeText(this, "Title and message cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnSend.setEnabled(false);
        binding.btnSend.setText("SENDING...");

        String officeId = selectedPos > 0 ? geofenceList.get(selectedPos - 1).getId() : null;

        com.google.firebase.firestore.Query query = FirebaseHelper.getFirestore().collection("users")
                .whereIn("role", java.util.Arrays.asList("employee", "Employee"));

        if (officeId != null) {
            query = query.whereEqualTo("assignedGeofenceId", officeId);
        }

        query.get()
                .addOnSuccessListener(querySnapshot -> processResults(querySnapshot, title, message))
                .addOnFailureListener(e -> handleError(e.getMessage()));
    }

    private void processResults(QuerySnapshot userQuery, String title, String message) {
        int count = 0;
        for (DocumentSnapshot doc : userQuery.getDocuments()) {
            String status = doc.getString("status");

            // Only broadcast to ACTIVE employees
            if (!"ACTIVE".equalsIgnoreCase(status)) continue;

            saveInAppNotification(doc.getId(), title, message);
            count++;
        }

        if (count > 0) {
            Toast.makeText(this, "Alert successfully broadcast to " + count + " active employees", Toast.LENGTH_LONG).show();
            binding.etTitle.setText("");
            binding.etMessage.setText("");
        } else {
            handleError("No active employee accounts found for the selection.");
        }
        
        resetButton();
    }

    private void handleError(String error) {
        Toast.makeText(this, error, Toast.LENGTH_LONG).show();
        resetButton();
    }

    private void resetButton() {
        binding.btnSend.setEnabled(true);
        binding.btnSend.setText("SEND BROADCAST");
    }

    private void saveInAppNotification(String userId, String title, String message) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("userId", userId);
        notification.put("title", title);
        notification.put("body", message);
        notification.put("category", "Announcement");
        notification.put("timestamp", Timestamp.now());
        notification.put("read", false);

        FirebaseHelper.getFirestore().collection("notifications").add(notification);
    }
}
