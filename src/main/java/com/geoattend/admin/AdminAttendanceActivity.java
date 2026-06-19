package com.geoattend.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.geoattend.R;
import com.geoattend.model.AttendanceRecord;
import com.geoattend.utils.FirebaseHelper;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdminAttendanceActivity extends AppCompatActivity {
    private List<AttendanceRecord> allRecords = new ArrayList<>();
    private List<AttendanceRecord> filteredRecords = new ArrayList<>();
    private AdminAttendanceAdapter adapter;
    private String currentStatusFilter = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_attendance);

        RecyclerView rv = findViewById(R.id.rv_admin_attendance);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminAttendanceAdapter(filteredRecords);
        rv.setAdapter(adapter);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        
        findViewById(R.id.btn_filter_status).setOnClickListener(v -> toggleStatusFilter());

        loadAllAttendance();
    }

    private void toggleStatusFilter() {
        if ("All".equals(currentStatusFilter)) currentStatusFilter = "VERIFIED";
        else if ("VERIFIED".equals(currentStatusFilter)) currentStatusFilter = "FLAGGED";
        else currentStatusFilter = "All";
        
        ((com.google.android.material.button.MaterialButton)findViewById(R.id.btn_filter_status)).setText(currentStatusFilter);
        applyFilters();
    }

    private void applyFilters() {
        filteredRecords.clear();
        for (AttendanceRecord r : allRecords) {
            if ("All".equals(currentStatusFilter)) {
                filteredRecords.add(r);
            } else if ("FLAGGED".equals(currentStatusFilter)) {
                if ("FLAGGED".equals(r.getStatus()) || "REJECTED".equals(r.getStatus())) filteredRecords.add(r);
            } else {
                if (currentStatusFilter.equals(r.getStatus())) filteredRecords.add(r);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void loadAllAttendance() {
        FirebaseHelper.getAttendanceRef()
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener((value, error) -> {
                if (value != null) {
                    allRecords.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        AttendanceRecord r = doc.toObject(AttendanceRecord.class);
                        if (r != null) allRecords.add(r);
                    }
                    applyFilters();
                }
            });
    }

    private static class AdminAttendanceAdapter extends RecyclerView.Adapter<AdminAttendanceAdapter.ViewHolder> {
        private final List<AttendanceRecord> list;
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        private final SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

        public AdminAttendanceAdapter(List<AttendanceRecord> list) { this.list = list; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_attendance, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            AttendanceRecord r = list.get(position);
            holder.tvName.setText(r.getUserName());
            if (r.getTimestamp() != null) {
                holder.tvDate.setText(dateFormat.format(r.getTimestamp().toDate()));
                holder.tvTime.setText(timeFormat.format(r.getTimestamp().toDate()));
            }
            holder.tvStatus.setText(r.getType() + " - " + r.getStatus());
            
            int color = "VERIFIED".equals(r.getStatus()) ? 0xFF10B981 : 0xFFEF4444;
            holder.tvStatus.setTextColor(color);
            holder.progressWork.getProgressDrawable().setTint(color);
        }

        @Override
        public int getItemCount() { return list.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvDate, tvTime, tvStatus;
            android.widget.ProgressBar progressWork;
            public ViewHolder(View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_date); // Reusing tv_date for name in admin view
                tvDate = itemView.findViewById(R.id.tv_time_range); // Reusing tv_time_range for date
                tvTime = itemView.findViewById(R.id.tv_duration); // Reusing tv_duration for time
                tvStatus = itemView.findViewById(R.id.tv_status);
                progressWork = itemView.findViewById(R.id.progress_work);
            }
        }
    }
}
