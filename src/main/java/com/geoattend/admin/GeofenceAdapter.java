package com.geoattend.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.geoattend.R;
import com.geoattend.model.GeofenceItem;
import com.geoattend.utils.FirebaseHelper;
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
        holder.tvRadius.setText("Radius: " + (int)item.getRadius() + "m");
        
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
        TextView tvName, tvRadius;
        View btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvRadius = itemView.findViewById(R.id.tvRadius);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
