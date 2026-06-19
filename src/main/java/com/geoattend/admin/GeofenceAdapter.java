package com.geoattend.admin;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.geoattend.R;
import com.geoattend.model.GeofenceItem;
import com.geoattend.utils.FirebaseHelper;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.List;

public class GeofenceAdapter extends RecyclerView.Adapter<GeofenceAdapter.ViewHolder> {
    private final List<GeofenceItem> list;

    public GeofenceAdapter(List<GeofenceItem> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_geofence, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        GeofenceItem item = list.get(position);
        holder.tvName.setText(item.getName());
        holder.tvAddress.setText(item.getAddress());
        holder.tvRadius.setText((int)item.getRadius() + "m radius");

        // Dynamic Staff Count (Office-Wise query)
        if (item.getId() != null) {
            FirebaseHelper.getGeofencesRef().document(item.getId())
                    .collection("members")
                    .get()
                    .addOnSuccessListener(query -> {
                        int count = query.size();
                        // Fallback: If office-wise subcollection is empty (old data), check main users collection
                        if (count == 0) {
                            FirebaseHelper.getFirestore().collection("users")
                                    .whereEqualTo("assignedGeofenceId", item.getId())
                                    .get()
                                    .addOnSuccessListener(userQuery -> {
                                        int legacyCount = 0;
                                        for (DocumentSnapshot doc : userQuery.getDocuments()) {
                                            if ("employee".equalsIgnoreCase(doc.getString("role"))) {
                                                legacyCount++;
                                            }
                                        }
                                        holder.tvStaff.setText(legacyCount + " employees");
                                    });
                        } else {
                            holder.tvStaff.setText(count + " employees");
                        }
                    });
        }
        
        holder.btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), AddGeofenceActivity.class);
            intent.putExtra("geofenceId", item.getId());
            intent.putExtra("name", item.getName());
            intent.putExtra("lat", item.getLatitude());
            intent.putExtra("lng", item.getLongitude());
            intent.putExtra("radius", item.getRadius());
            intent.putExtra("address", item.getAddress());
            v.getContext().startActivity(intent);
        });

        holder.btnMap.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), AdminDashboardActivity.class);
            intent.putExtra("lat", item.getLatitude());
            intent.putExtra("lng", item.getLongitude());
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            v.getContext().startActivity(intent);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (item.getId() != null) {
                FirebaseHelper.getGeofencesRef().document(item.getId()).delete();
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvAddress, tvRadius, tvStaff;
        View btnDelete, btnEdit, btnMap;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvAddress = itemView.findViewById(R.id.tv_address);
            tvRadius = itemView.findViewById(R.id.tvRadius);
            tvStaff = itemView.findViewById(R.id.tv_staff);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnMap = itemView.findViewById(R.id.btn_map);
        }
    }
}
