package com.geoattend.employee;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.geoattend.LoginActivity;
import com.geoattend.R;
import com.geoattend.databinding.ActivityProfileBinding;
import com.geoattend.model.User;
import com.geoattend.utils.FirebaseHelper;
import com.google.firebase.firestore.DocumentSnapshot;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ProfileActivity extends AppCompatActivity {
    private ActivityProfileBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initNavigation();
        fetchUserData();
        
        binding.btnExport.setOnClickListener(v -> exportAttendance());

        binding.btnLogout.setOnClickListener(v -> {
            FirebaseHelper.getAuth().signOut();
            startActivity(new Intent(this, LoginActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finishAffinity();
        });
    }

    private void fetchUserData() {
        FirebaseHelper.getUserRef(FirebaseHelper.getCurrentUserId()).get().addOnSuccessListener(doc -> {
            User user = doc.toObject(User.class);
            if (user != null) {
                binding.tvName.setText(user.getName());
                binding.tvEmpId.setText("ID: " + user.getEmployeeId());
            }
        });
    }

    private void exportAttendance() {
        Toast.makeText(this, "Generating Report...", Toast.LENGTH_SHORT).show();
        String uid = FirebaseHelper.getCurrentUserId();
        FirebaseHelper.getAttendanceRef()
            .whereEqualTo("userId", uid)
            .limit(50)
            .get()
            .addOnSuccessListener(query -> {
                StringBuilder sb = new StringBuilder();
                sb.append("GEOATTEND ATTENDANCE REPORT\n");
                sb.append("===========================\n\n");
                for (DocumentSnapshot doc : query.getDocuments()) {
                    com.google.firebase.Timestamp ts = doc.getTimestamp("timestamp");
                    if (ts == null) continue;
                    Date date = ts.toDate();
                    String type = doc.getString("type");
                    String location = doc.getString("geofenceName");
                    sb.append(String.format("%s: %s at %s\n", 
                        new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(date),
                        type, location));
                }
                
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_SUBJECT, "Attendance Report");
                intent.putExtra(Intent.EXTRA_TEXT, sb.toString());
                startActivity(Intent.createChooser(intent, "Share Report via"));
            });
    }

    private void initNavigation() {
        binding.bottomNavigation.setSelectedItemId(R.id.nav_profile);
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_dashboard) {
                startActivity(new Intent(this, EmployeeDashboardActivity.class));
                overridePendingTransition(0, 0);
                finish();
            } else if (id == R.id.nav_attendance) {
                startActivity(new Intent(this, com.geoattend.AttendanceHistoryActivity.class));
                overridePendingTransition(0, 0);
                finish();
            } else if (id == R.id.nav_notifications) {
                startActivity(new Intent(this, NotificationsActivity.class));
                overridePendingTransition(0, 0);
                finish();
            }
            return true;
        });
    }
}
