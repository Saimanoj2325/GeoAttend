package com.geoattend;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.geoattend.databinding.ActivityAttendanceHistoryBinding;
import com.geoattend.employee.EmployeeDashboardActivity;
import com.geoattend.employee.NotificationsActivity;
import com.geoattend.employee.ProfileActivity;
import com.geoattend.model.AttendanceRecord;
import com.geoattend.utils.FirebaseHelper;
import com.google.firebase.firestore.Query;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AttendanceHistoryActivity extends AppCompatActivity {
    private ActivityAttendanceHistoryBinding binding;
    private List<AttendanceRecord> allRecords = new ArrayList<>();
    private List<AttendanceRecord> filteredRecords = new ArrayList<>();
    private HistoryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAttendanceHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.rvHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistoryAdapter(filteredRecords);
        binding.rvHistory.setAdapter(adapter);

        binding.calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            filterRecords(year, month, dayOfMonth);
        });

        initNavigation();
        loadHistory();
    }

    private void filterRecords(int year, int month, int dayOfMonth) {
        filteredRecords.clear();
        String targetDate = String.format(Locale.getDefault(), "%02d %s %d", dayOfMonth, getMonthName(month), year);
        for (AttendanceRecord record : allRecords) {
            if (record.getTimestamp() != null) {
                String recordDate = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(record.getTimestamp().toDate());
                if (recordDate.equals(targetDate)) {
                    filteredRecords.add(record);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private String getMonthName(int month) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.MONTH, month);
        return new SimpleDateFormat("MMM", Locale.getDefault()).format(cal.getTime());
    }

    private void initNavigation() {
        binding.bottomNavigation.setSelectedItemId(R.id.nav_attendance);
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_dashboard) {
                startActivity(new Intent(this, EmployeeDashboardActivity.class));
                overridePendingTransition(0, 0);
                finish();
            } else if (id == R.id.nav_notifications) {
                startActivity(new Intent(this, NotificationsActivity.class));
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

    private void loadHistory() {
        String uid = FirebaseHelper.getCurrentUserId();
        FirebaseHelper.getAttendanceRef()
            .whereEqualTo("userId", uid)
            .addSnapshotListener((value, error) -> {
                if (error != null) {
                    android.util.Log.e("History", "Error loading history: " + error.getMessage());
                    return;
                }
                if (value != null) {
                    List<AttendanceRecord> records = value.toObjects(AttendanceRecord.class);
                    // Manual sort to avoid index requirement
                    java.util.Collections.sort(records, (a, b) -> {
                        if (a.getTimestamp() == null || b.getTimestamp() == null) return 0;
                        return b.getTimestamp().compareTo(a.getTimestamp());
                    });
                    
                    allRecords.clear();
                    allRecords.addAll(records);
                    filteredRecords.clear();
                    filteredRecords.addAll(allRecords); // Default show all
                    adapter.notifyDataSetChanged();
                }
            });
    }

    private static class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
        private final List<AttendanceRecord> list;
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        private final SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

        public HistoryAdapter(List<AttendanceRecord> list) { this.list = list; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_attendance, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            AttendanceRecord record = list.get(position);
            if (record.getTimestamp() != null) {
                holder.tvDate.setText(dateFormat.format(record.getTimestamp().toDate()));
                holder.tvTime.setText(timeFormat.format(record.getTimestamp().toDate()));
            }
            holder.tvLocation.setText(record.getGeofenceName());
            holder.tvStatus.setText(record.getType() + " - " + record.getStatus());
            
            String type = record.getType() != null ? record.getType() : "";
            int color = type.contains("IN") ? 0xFF10B981 : 0xFFEF4444;
            holder.statusNode.setBackgroundColor(color);
            holder.tvStatus.setTextColor(color);

            // Additional details in snippet
            holder.tvDetails.setText(String.format(Locale.getDefault(), "Device: %s • Accuracy: %.1fm", 
                record.getDeviceId() != null ? record.getDeviceId().substring(0, 8) + "..." : "Unknown",
                record.getAccuracy()));
        }

        @Override
        public int getItemCount() { return list.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvDate, tvTime, tvLocation, tvStatus, tvDetails;
            View statusNode;
            public ViewHolder(View itemView) {
                super(itemView);
                tvDate = itemView.findViewById(R.id.tv_date);
                tvTime = itemView.findViewById(R.id.tv_time);
                tvLocation = itemView.findViewById(R.id.tv_location);
                tvStatus = itemView.findViewById(R.id.tv_status);
                tvDetails = itemView.findViewById(R.id.tv_details);
                statusNode = itemView.findViewById(R.id.status_node);
            }
        }
    }
}
