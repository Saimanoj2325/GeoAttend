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

        binding.btnCalendar.setOnClickListener(v -> {
            int visibility = binding.calendarView.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE;
            binding.calendarView.setVisibility(visibility);
        });

        binding.calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            filterRecords(year, month, dayOfMonth);
            binding.calendarView.setVisibility(View.GONE);
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
            if (id == R.id.nav_attendance) return true;

            if (id == R.id.nav_dashboard) {
                startActivity(new Intent(this, EmployeeDashboardActivity.class));
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                finish();
            } else if (id == R.id.nav_notifications) {
                startActivity(new Intent(this, NotificationsActivity.class));
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
                    updateSummaryStats(allRecords);
                }
            });
    }

    private void updateSummaryStats(List<AttendanceRecord> records) {
        int presentCount = 0;
        java.util.Set<String> uniqueDays = new java.util.HashSet<>();
        
        for (AttendanceRecord r : records) {
            if ("IN".equals(r.getType()) && r.getTimestamp() != null) {
                String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(r.getTimestamp().toDate());
                if (uniqueDays.add(date)) {
                    presentCount++;
                }
            }
        }
        
        // Calculate based on current month
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int totalDaysInMonth = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH);
        int daysPassed = cal.get(java.util.Calendar.DAY_OF_MONTH);
        
        // Assuming 22 work days for a full month
        int absentCount = Math.max(0, daysPassed - presentCount);
        int rate = (presentCount * 100) / Math.max(1, daysPassed);

        binding.tvPresentCount.setText(String.valueOf(presentCount));
        binding.tvAbsentCount.setText(String.valueOf(absentCount));
        binding.tvAttendanceRate.setText(rate + "%");
        
        binding.tvMonth.setText(new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(new java.util.Date()));
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
                holder.tvTimeRange.setText(timeFormat.format(record.getTimestamp().toDate()));
            }
            
            String status = record.getStatus() != null ? record.getStatus() : "PENDING";
            holder.tvStatus.setText(status);
            
            int color = "FLAGGED".equals(status) || "REJECTED".equals(status) ? 0xFFEF4444 : 0xFF10B981;
            holder.layoutStatus.getBackground().setTint(color & 0x1AFFFFFF); // Very faint
            holder.tvStatus.setTextColor(color);

            // Dynamic logic: If it's an OUT record, try to find the duration
            String details = record.getType() + " @ " + record.getGeofenceName();
            if (("OUT".equals(record.getType()) || "AUTO_OUT".equals(record.getType())) && position < list.size() - 1) {
                // Look for previous record in the sorted list (since it's descending, the previous time is at position + 1)
                AttendanceRecord prev = list.get(position + 1);
                if ("IN".equals(prev.getType()) && record.getTimestamp() != null && prev.getTimestamp() != null) {
                    long diff = record.getTimestamp().toDate().getTime() - prev.getTimestamp().toDate().getTime();
                    long hours = diff / (1000 * 60 * 60);
                    long mins = (diff / (1000 * 60)) % 60;
                    details = String.format(Locale.getDefault(), "Worked %dh %dm", hours, mins);
                }
            }
            holder.tvDuration.setText(details);
            
            holder.progressWork.setProgress(100);
            holder.progressWork.getProgressDrawable().setTint(color);
        }

        @Override
        public int getItemCount() { return list.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvDate, tvTimeRange, tvStatus, tvDuration;
            View layoutStatus;
            android.widget.ProgressBar progressWork;
            public ViewHolder(View itemView) {
                super(itemView);
                tvDate = itemView.findViewById(R.id.tv_date);
                tvTimeRange = itemView.findViewById(R.id.tv_time_range);
                tvStatus = itemView.findViewById(R.id.tv_status);
                tvDuration = itemView.findViewById(R.id.tv_duration);
                layoutStatus = itemView.findViewById(R.id.layout_status);
                progressWork = itemView.findViewById(R.id.progress_work);
            }
        }
    }
}
