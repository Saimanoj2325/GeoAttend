package com.geoattend.employee;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.geoattend.AttendanceHistoryActivity;
import com.geoattend.R;
import com.geoattend.databinding.ActivityNotificationsBinding;
import com.geoattend.utils.FirebaseHelper;
import com.google.android.material.chip.Chip;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity {
    private ActivityNotificationsBinding binding;
    private final List<NotificationAdapter.NotificationItem> allNotifications = new ArrayList<>();
    private final List<NotificationAdapter.NotificationItem> filteredNotifications = new ArrayList<>();
    private NotificationAdapter adapter;
    private String currentFilter = "All";

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
        loadNotifications();

        binding.btnMarkAll.setOnClickListener(v -> markAllRead());
    }

    private void markAllRead() {
        String uid = FirebaseHelper.getCurrentUserId();
        FirebaseHelper.getFirestore().collection("notifications")
            .whereEqualTo("userId", uid)
            .whereEqualTo("read", false)
            .get()
            .addOnSuccessListener(query -> {
                for (DocumentSnapshot doc : query.getDocuments()) {
                    doc.getReference().update("read", true);
                }
            });
    }

    private void initFilters() {
        binding.chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            
            int checkedId = checkedIds.get(0);
            View chipView = findViewById(checkedId);
            if (chipView instanceof Chip) {
                Chip chip = (Chip) chipView;
                currentFilter = chip.getText().toString();
                filterNotifications(currentFilter);
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

    private void loadNotifications() {
        String uid = FirebaseHelper.getCurrentUserId();
        FirebaseHelper.getFirestore().collection("notifications")
            .whereEqualTo("userId", uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener((value, error) -> {
                if (value != null) {
                    allNotifications.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        NotificationAdapter.NotificationItem item = doc.toObject(NotificationAdapter.NotificationItem.class);
                        if (item != null) allNotifications.add(item);
                    }
                    filterNotifications(currentFilter);
                }
            });
    }

    private void updateEmptyState() {
        boolean isEmpty = filteredNotifications.isEmpty();
        binding.emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        binding.rvNotifications.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void initNavigation() {
        binding.bottomNavigation.setSelectedItemId(R.id.nav_notifications);
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
            if (id == R.id.nav_notifications) return true;

            if (id == R.id.nav_dashboard) {
                startActivity(new Intent(this, EmployeeDashboardActivity.class));
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                finish();
            } else if (id == R.id.nav_attendance) {
                startActivity(new Intent(this, AttendanceHistoryActivity.class));
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                finish();
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                finish();
            }
            return true;
        });
    }
}
