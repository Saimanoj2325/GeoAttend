package com.geoattend.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.geoattend.R;
import com.geoattend.model.GeofenceItem;
import com.geoattend.model.User;
import com.geoattend.utils.AdminNavigationHelper;
import com.geoattend.utils.FirebaseHelper;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class UserManagementActivity extends AppCompatActivity {
    private List<User> allUsers = new ArrayList<>();
    private List<User> filteredUsers = new ArrayList<>();
    private UserAdapter adapter;
    private RecyclerView rvUsers;
    private EditText etSearch;
    private Spinner spinnerFilterOffice;
    private List<GeofenceItem> geofenceList = new ArrayList<>();
    private List<String> officeNames = new ArrayList<>();
    private ArrayAdapter<String> officeAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_management);

        rvUsers = findViewById(R.id.rv_users);
        etSearch = findViewById(R.id.et_search_users);
        spinnerFilterOffice = findViewById(R.id.spinner_filter_office);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        AdminNavigationHelper.init(this, R.id.nav_employees);

        adapter = new UserAdapter(filteredUsers);
        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        rvUsers.setAdapter(adapter);

        officeAdapter = new ArrayAdapter<>(this, R.layout.item_spinner, officeNames);
        officeAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        spinnerFilterOffice.setAdapter(officeAdapter);

        loadUsers();
        loadOffices();

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterUsers(s.toString(), spinnerFilterOffice.getSelectedItemPosition());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        spinnerFilterOffice.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filterUsers(etSearch.getText().toString(), position);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadOffices() {
        FirebaseHelper.getGeofencesRef().get().addOnSuccessListener(queryDocumentSnapshots -> {
            geofenceList.clear();
            officeNames.clear();
            officeNames.add("All Offices");
            for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
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

    private void loadUsers() {
        FirebaseHelper.getFirestore().collection("users")
            .addSnapshotListener((value, error) -> {
                if (value != null) {
                    allUsers.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        String role = doc.getString("role");
                        if ("employee".equalsIgnoreCase(role)) {
                            User user = doc.toObject(User.class);
                            if (user != null) allUsers.add(user);
                        }
                    }
                    filterUsers(etSearch.getText().toString(), spinnerFilterOffice.getSelectedItemPosition());
                }
            });
    }

    private void filterUsers(String query, int officePos) {
        filteredUsers.clear();
        String q = query.toLowerCase(Locale.getDefault());
        String officeId = officePos > 0 ? geofenceList.get(officePos - 1).getId() : null;

        for (User u : allUsers) {
            boolean matchesSearch = u.getName().toLowerCase().contains(q) || 
                                    (u.getEmployeeId() != null && u.getEmployeeId().toLowerCase().contains(q));
            
            boolean matchesOffice = officeId == null || officeId.equals(u.getAssignedGeofenceId());

            if (matchesSearch && matchesOffice) {
                filteredUsers.add(u);
            }
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(0, 0);
    }

    private static class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {
        private final List<User> list;
        public UserAdapter(List<User> list) { this.list = list; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            User user = list.get(position);
            holder.tvName.setText(user.getName());
            holder.tvId.setText(user.getEmployeeId() != null ? user.getEmployeeId() : "No ID");
            holder.tvStatus.setText(user.getStatus() != null ? user.getStatus() : "ACTIVE");
            
            int statusColor = "ACTIVE".equalsIgnoreCase(user.getStatus()) ? 0xFF10B981 : 0xFFF59E0B;
            holder.tvStatus.setTextColor(statusColor);

            holder.btnManage.setOnClickListener(v -> {
                showManageUserDialog(v.getContext(), user);
            });
        }

        private void showManageUserDialog(android.content.Context context, User user) {
            android.widget.EditText etDept = new android.widget.EditText(context);
            etDept.setHint("Department");
            etDept.setText(user.getDepartment());
            
            android.widget.EditText etManager = new android.widget.EditText(context);
            etManager.setHint("Manager Name");
            etManager.setText(user.getManagerName());

            // Add Office Re-assignment
            android.widget.Spinner spOffice = new android.widget.Spinner(context);
            List<String> names = new ArrayList<>();
            final List<GeofenceItem> geofences = ((UserManagementActivity)context).geofenceList;
            names.add("Unassigned / Select Office");
            int selectedIdx = 0;
            for (int i = 0; i < geofences.size(); i++) {
                names.add(geofences.get(i).getName());
                if (geofences.get(i).getId().equals(user.getAssignedGeofenceId())) {
                    selectedIdx = i + 1;
                }
            }
            android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(context, R.layout.item_spinner, names);
            adapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
            spOffice.setAdapter(adapter);
            spOffice.setBackgroundResource(R.drawable.bg_spinner);
            spOffice.setSelection(selectedIdx);

            android.widget.LinearLayout layout = new android.widget.LinearLayout(context);
            layout.setOrientation(android.widget.LinearLayout.VERTICAL);
            layout.setPadding(40, 20, 40, 20);
            
            android.widget.TextView tvLabel = new android.widget.TextView(context);
            tvLabel.setText("Assign to Office:");
            tvLabel.setPadding(0, 10, 0, 10);

            layout.addView(etDept);
            layout.addView(etManager);
            layout.addView(tvLabel);
            layout.addView(spOffice);

            new androidx.appcompat.app.AlertDialog.Builder(context)
                .setTitle("Manage " + user.getName())
                .setView(layout)
                .setPositiveButton("Update", (dialog, which) -> {
                    user.setDepartment(etDept.getText().toString());
                    user.setManagerName(etManager.getText().toString());
                    
                    String oldOfficeId = user.getAssignedGeofenceId();
                    int pos = spOffice.getSelectedItemPosition();
                    String newOfficeId = pos > 0 ? geofences.get(pos - 1).getId() : null;

                    user.setAssignedGeofenceId(newOfficeId);
                    
                    // Update main document
                    FirebaseHelper.getUserRef(user.getUid()).set(user);

                    // Maintain Office-Wise subcollections
                    if (oldOfficeId != null && !oldOfficeId.equals(newOfficeId)) {
                        FirebaseHelper.getGeofencesRef().document(oldOfficeId)
                                .collection("members").document(user.getUid()).delete();
                    }
                    
                    if (newOfficeId != null) {
                        java.util.Map<String, Object> member = new java.util.HashMap<>();
                        member.put("uid", user.getUid());
                        member.put("name", user.getName());
                        member.put("email", user.getEmail());
                        FirebaseHelper.getGeofencesRef().document(newOfficeId)
                                .collection("members").document(user.getUid()).set(member);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
        }

        @Override
        public int getItemCount() { return list.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvId, tvStatus;
            View btnManage;
            public ViewHolder(View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_user_name);
                tvId = itemView.findViewById(R.id.tv_user_id);
                tvStatus = itemView.findViewById(R.id.tv_user_status);
                btnManage = itemView.findViewById(R.id.btn_manage);
            }
        }
    }
}
