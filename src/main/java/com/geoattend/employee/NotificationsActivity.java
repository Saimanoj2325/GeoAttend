package com.geoattend.employee;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.geoattend.AttendanceHistoryActivity;
import com.geoattend.R;
import com.geoattend.databinding.ActivityNotificationsBinding;
import com.google.android.material.chip.Chip;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity {
    private ActivityNotificationsBinding binding;
    private final List<NotificationAdapter.NotificationItem> allNotifications = new ArrayList<>();
    private final List<NotificationAdapter.NotificationItem> filteredNotifications = new ArrayList<>();
    private NotificationAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNotificationsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        adapter = new NotificationAdapter(filteredNotifications);
        binding.rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        binding.rvNotifications.setAdapter(adapter);

        initFilters();
        initNavigation();
        loadMockNotifications();
    }

    private void initFilters() {
        binding.chipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == View.NO_ID) return;
            
            View chipView = findViewById(checkedId);
            if (chipView instanceof Chip) {
                Chip chip = (Chip) chipView;
                String filter = chip.getText().toString();
                filterNotifications(filter);
            }
        });
    }

    private void filterNotifications(String category) {
        filteredNotifications.clear();
        if ("All".equalsIgnoreCase(category)) {
            filteredNotifications.addAll(allNotifications);
        } else {
            for (NotificationAdapter.NotificationItem item : allNotifications) {
                if (item.getCategory().equalsIgnoreCase(category)) {
                    filteredNotifications.add(item);
                }
            }
        }
        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void loadMockNotifications() {
        allNotifications.add(new NotificationAdapter.NotificationItem("Attendance Verified", "Your check-in at Vijayawada HQ was successful.", "System", new Date(), false));
        allNotifications.add(new NotificationAdapter.NotificationItem("Security Alert", "New device login detected from Pixel 7.", "Security", new Date(System.currentTimeMillis() - 3600000), true));
        allNotifications.add(new NotificationAdapter.NotificationItem("Welcome to GeoAttend", "Setup your biometric profile to start checking in.", "System", new Date(System.currentTimeMillis() - 86400000), true));
        
        filteredNotifications.addAll(allNotifications);
        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void updateEmptyState() {
        binding.emptyState.setVisibility(filteredNotifications.isEmpty() ? View.VISIBLE : View.GONE);
        binding.rvNotifications.setVisibility(filteredNotifications.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void initNavigation() {
        binding.bottomNavigation.setSelectedItemId(R.id.nav_notifications);
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_dashboard) {
                startActivity(new Intent(this, EmployeeDashboardActivity.class));
                overridePendingTransition(0, 0);
                finish();
            } else if (id == R.id.nav_attendance) {
                startActivity(new Intent(this, AttendanceHistoryActivity.class));
                overridePendingTransition(0, 0);
                finish();
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                overridePendingTransition(0, 0);
                finish();
            }
            return true;
        });
    }
}
